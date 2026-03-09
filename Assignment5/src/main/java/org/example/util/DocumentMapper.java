package org.example.util;

/**
 * Developer guide: Helper conversions between BSON documents and typed models.
 */

import org.bson.Document;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DocumentMapper {
    private DocumentMapper() {
    }

    public static <T> Document toDocument(T value) {
        Object converted = toValue(value);
        if (converted instanceof Map<?, ?> map) {
            return new Document((Map<String, Object>) map);
        }
        return new Document();
    }

    public static <T> T fromDocument(Document document, Class<T> type) {
        return convertValue(document, type, type);
    }

    private static Object toValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    out.put(String.valueOf(entry.getKey()), toValue(entry.getValue()));
                }
            }
            return out;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> out = new ArrayList<>();
            for (Object item : iterable) {
                out.add(toValue(item));
            }
            return out;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        try {
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(value.getClass(), Object.class).getPropertyDescriptors()) {
                if (descriptor.getReadMethod() == null) {
                    continue;
                }
                Object propertyValue = descriptor.getReadMethod().invoke(value);
                out.put(descriptor.getName(), toValue(propertyValue));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize " + value.getClass().getName(), e);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertValue(Object value, Class<T> rawType, Type genericType) {
        if (value == null) {
            return null;
        }
        if (rawType == String.class) {
            return (T) String.valueOf(value);
        }
        if (rawType == Integer.class || rawType == int.class) {
            return (T) Integer.valueOf(value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value)));
        }
        if (rawType == Long.class || rawType == long.class) {
            return (T) Long.valueOf(value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value)));
        }
        if (rawType == Double.class || rawType == double.class) {
            return (T) Double.valueOf(value instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(value)));
        }
        if (rawType == Boolean.class || rawType == boolean.class) {
            return (T) Boolean.valueOf(value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value)));
        }
        if (rawType == Instant.class) {
            return (T) Instant.parse(String.valueOf(value));
        }
        if (rawType == LocalDate.class) {
            return (T) LocalDate.parse(String.valueOf(value));
        }
        if (rawType == Object.class) {
            return (T) normalizeRaw(value);
        }
        if (rawType.isEnum()) {
            return (T) Enum.valueOf((Class<? extends Enum>) rawType.asSubclass(Enum.class), String.valueOf(value));
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Map<String, Object> out = new HashMap<>();
            if (value instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    out.put(String.valueOf(entry.getKey()), normalizeRaw(entry.getValue()));
                }
            }
            return (T) out;
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            Collection<Object> out = new ArrayList<>();
            Type elementType = Object.class;
            if (genericType instanceof ParameterizedType parameterizedType) {
                elementType = parameterizedType.getActualTypeArguments()[0];
            }
            Class<?> elementClass = rawClass(elementType);
            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    out.add(convertValue(item, elementClass, elementType));
                }
            }
            return (T) out;
        }

        if (!(value instanceof Map<?, ?> mapValue)) {
            throw new IllegalStateException("Unsupported conversion to " + rawType.getName());
        }

        try {
            Constructor<T> constructor = rawType.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(rawType, Object.class).getPropertyDescriptors()) {
                Object propertyValue = normalized.get(descriptor.getName());
                if (propertyValue == null) {
                    continue;
                }

                Field field = findField(rawType, descriptor.getName());
                Type propertyType = field != null ? field.getGenericType() : descriptor.getPropertyType();
                Class<?> propertyClass = descriptor.getPropertyType();

                if (descriptor.getWriteMethod() != null) {
                    descriptor.getWriteMethod().invoke(instance, convertValue(propertyValue, propertyClass, propertyType));
                    continue;
                }

                if (descriptor.getReadMethod() != null) {
                    Object target = descriptor.getReadMethod().invoke(instance);
                    if (target instanceof Collection<?> existing && propertyValue instanceof Collection<?> rawCollection) {
                        Collection<Object> writable = (Collection<Object>) existing;
                        writable.clear();
                        Type elementType = propertyType instanceof ParameterizedType pt ? pt.getActualTypeArguments()[0] : Object.class;
                        Class<?> elementClass = rawClass(elementType);
                        for (Object item : rawCollection) {
                            writable.add(convertValue(item, elementClass, elementType));
                        }
                    } else if (target instanceof Map<?, ?> existingMap && propertyValue instanceof Map<?, ?> rawMap) {
                        Map<String, Object> writable = (Map<String, Object>) existingMap;
                        writable.clear();
                        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                            writable.put(String.valueOf(entry.getKey()), normalizeRaw(entry.getValue()));
                        }
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize " + rawType.getName(), e);
        }
    }

    private static Object normalizeRaw(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.put(String.valueOf(entry.getKey()), normalizeRaw(entry.getValue()));
            }
            return out;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> out = new ArrayList<>();
            for (Object item : collection) {
                out.add(normalizeRaw(item));
            }
            return out;
        }
        return value;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getRawType());
        }
        return Object.class;
    }
}
