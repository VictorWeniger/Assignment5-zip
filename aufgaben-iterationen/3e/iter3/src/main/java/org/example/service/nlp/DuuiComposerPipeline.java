package org.example.service.nlp;

import org.example.config.NlpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIDockerDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.LuaConsts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * DUUI composer wrapper for the professor components (spaCy, GerVader, ParlBERT topic).
 */
final class DuuiComposerPipeline implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DuuiComposerPipeline.class);

    private final DUUIComposer composer;
    private final String selection;
    private final String viewName;

    private DuuiComposerPipeline(DUUIComposer composer, String selection, String viewName) {
        this.composer = composer;
        this.selection = selection;
        this.viewName = viewName;
    }

    static DuuiComposerPipeline create(NlpConfig config) {
        if (config == null || !config.hasDuuiPipelineTargets()) {
            throw new IllegalStateException("DUUI pipeline targets are not fully configured");
        }

        int workers = Math.max(1, config.duuiWorkers());
        String selection = notBlank(config.duuiSelection()) ? config.duuiSelection().trim() : "text";
        String view = notBlank(config.duuiViewName()) ? config.duuiViewName().trim() : "speech";
        String mode = config.duuiMode() == null ? "remote" : config.duuiMode().toLowerCase(Locale.ROOT).trim();

        try {
            DUUIComposer composer = new DUUIComposer()
                    .withSkipVerification(true)
                    .withWorkers(workers);
            attachLuaContext(composer);

            switch (mode) {
                case "docker" -> composer.addDriver(new DUUIDockerDriver());
                case "mixed" -> composer.addDriver(new DUUIDockerDriver(), new DUUIRemoteDriver());
                default -> composer.addDriver(new DUUIRemoteDriver());
            }

            // Build the component chain in the same order as the assignment DUUI pipeline.
            Object spacyBuilder = buildComponent(config.duuiSpacyTarget());
            configureCommon(spacyBuilder, workers, selection, view);
            setParameterSafe(spacyBuilder, "language", notBlank(config.duuiSpacyLanguage()) ? config.duuiSpacyLanguage() : "de");
            addComponent(composer, invokeBuild(spacyBuilder));

            Object gervaderBuilder = buildComponent(config.duuiGervaderTarget());
            configureCommon(gervaderBuilder, workers, selection, view);
            addComponent(composer, invokeBuild(gervaderBuilder));

            Object parlbertBuilder = buildComponent(config.duuiParlbertTopicTarget());
            configureCommon(parlbertBuilder, workers, selection, view);
            addComponent(composer, invokeBuild(parlbertBuilder));

            LOGGER.info("DUUI composer pipeline initialized (mode={}, workers={}, view={}, selection={})",
                    mode, workers, view, selection);
            return new DuuiComposerPipeline(composer, selection, view);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build DUUI composer pipeline: " + ex.getMessage(), ex);
        }
    }

    String selection() {
        return selection;
    }

    String viewName() {
        return viewName;
    }

    void process(org.apache.uima.jcas.JCas base) throws Exception {
        composer.run(base);
    }

    @Override
    public void close() {
        try {
            Method shutdown = composer.getClass().getMethod("shutdown");
            shutdown.invoke(composer);
            return;
        } catch (Exception ignored) {
            // fallback to close
        }
        try {
            Method close = composer.getClass().getMethod("close");
            close.invoke(composer);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static void attachLuaContext(DUUIComposer composer) {
        try {
            Object ctx = LuaConsts.getJSON();
            Method withLuaContext = composer.getClass().getMethod("withLuaContext", ctx.getClass());
            withLuaContext.invoke(composer, ctx);
        } catch (Exception ignored) {
            // optional on some DUUI versions
        }
    }

    private static void configureCommon(Object builder, int workers, String selection, String viewName) {
        callIfExists(builder, "withScale", new Class[]{int.class}, new Object[]{workers});
        callIfExists(builder, "withImageFetching", new Class[]{}, new Object[]{});

        // Mirror the professor component parameter names because different DUUI versions expose different builders.
        setParameterSafe(builder, "selection", selection);
        setParameterSafe(builder, "selector", selection);
        setParameterSafe(builder, "sourceView", viewName);
        setParameterSafe(builder, "targetView", viewName);
        setParameterSafe(builder, "inputView", viewName);
        setParameterSafe(builder, "outputView", viewName);
        setParameterSafe(builder, "view", viewName);
        setParameterSafe(builder, "viewName", viewName);

        callIfExists(builder, "withSourceView", new Class[]{String.class}, new Object[]{viewName});
        callIfExists(builder, "withTargetView", new Class[]{String.class}, new Object[]{viewName});
    }

    private static Object buildComponent(String target) throws Exception {
        String normalized = target == null ? "" : target.trim();
        if (normalized.isBlank()) {
            throw new IllegalStateException("DUUI component target must not be blank");
        }

        String className = looksLikeUrl(normalized)
                ? "org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver$Component"
                : "org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIDockerDriver$Component";
        try {
            Class<?> cls = Class.forName(className);
            Constructor<?> constructor = cls.getConstructor(String.class);
            return constructor.newInstance(normalized);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Could not find DUUI component builder class " + className, ex);
        }
    }

    private static Object invokeBuild(Object builder) throws Exception {
        Method build = builder.getClass().getMethod("build");
        return build.invoke(builder);
    }

    private static void addComponent(DUUIComposer composer, Object component) throws Exception {
        for (Method method : composer.getClass().getMethods()) {
            if (!method.getName().equals("add") && !method.getName().equals("addComponent")) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> p = method.getParameterTypes()[0];
            if (!p.isAssignableFrom(component.getClass())) {
                continue;
            }
            method.invoke(composer, component);
            return;
        }
        for (Method method : composer.getClass().getMethods()) {
            if (!method.getName().equals("add") && !method.getName().equals("addComponent")) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> p = method.getParameterTypes()[0];
            String parameterName = p.getName();
            String componentName = component.getClass().getName();
            if (componentName.equals(parameterName) || componentName.startsWith(parameterName + "$") || Object.class.equals(p)) {
                method.invoke(composer, component);
                return;
            }
        }
        throw new IllegalStateException("Could not add DUUI component to composer");
    }

    private static void setParameterSafe(Object builder, String key, String value) {
        try {
            invokeWithParameter(builder, key, value);
        } catch (Exception ignored) {
            // optional, depends on builder implementation
        }
    }

    private static void invokeWithParameter(Object builder, String key, String value) throws Exception {
        if (builder == null || key == null || key.isBlank() || value == null) {
            return;
        }
        for (Method method : builder.getClass().getMethods()) {
            if (!method.getName().equals("withParameter") || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!String.class.equals(parameterTypes[0])) {
                continue;
            }

            Object converted;
            Class<?> valueType = parameterTypes[1];
            if (valueType.isAssignableFrom(String.class) || Object.class.equals(valueType)) {
                converted = value;
            } else if (Integer.TYPE.equals(valueType) || Integer.class.equals(valueType)) {
                converted = Integer.parseInt(value.trim());
            } else if (Boolean.TYPE.equals(valueType) || Boolean.class.equals(valueType)) {
                converted = Boolean.parseBoolean(value.trim());
            } else if (Double.TYPE.equals(valueType) || Double.class.equals(valueType)) {
                converted = Double.parseDouble(value.trim());
            } else {
                converted = value;
            }
            method.invoke(builder, key, converted);
            return;
        }
        throw new NoSuchMethodException(builder.getClass().getName() + ".withParameter(String, ..) not found");
    }

    private static void callIfExists(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            method.invoke(target, args);
        } catch (Exception ignored) {
            // optional
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean looksLikeUrl(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}
