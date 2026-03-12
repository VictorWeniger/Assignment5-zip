package org.example.db;

import org.bson.Document;

import java.util.List;
import java.util.Optional;

/**
 * @author
 * Victor Weniger
 */

/**
 * DatabaseHandler database
 */
public interface DatabaseHandler<T> {
    void insert(String collection, T entity);

    void replaceById(String collection, String id, T entity);

    Optional<T> findById(String collection, String id, Class<T> type);

    List<T> find(String collection, Document filter, Class<T> type);

    default List<T> findLimited(String collection, Document filter, Class<T> type, int limit) {
        List<T> values = find(collection, filter, type);
        if (limit <= 0 || values.size() <= limit) {
            return values;
        }
        return values.subList(0, limit);
    }

    long count(String collection, Document filter);

    List<Document> aggregate(String collection, List<Document> pipeline);

    void deleteById(String collection, String id);
}
