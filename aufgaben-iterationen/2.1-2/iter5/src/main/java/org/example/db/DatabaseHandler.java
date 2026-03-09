package org.example.db;

/**
 * Developer guide: Generic database abstraction used by services/controllers.
 */

import org.bson.Document;

import java.util.List;
import java.util.Optional;

/**
 * Generic database access abstraction used by services/controllers.
 */
public interface DatabaseHandler<T> {
    /**
     * Inserts one entity into a collection.
     */
    void insert(String collection, T entity);

    /**
     * Replaces an entity by id, creating it if missing.
     */
    void replaceById(String collection, String id, T entity);

    /**
     * Finds one entity by id.
     */
    Optional<T> findById(String collection, String id, Class<T> type);

    /**
     * Finds entities matching a filter.
     */
    List<T> find(String collection, Document filter, Class<T> type);

    /**
     * Finds entities matching a Mongo filter and applies the limit server-side where supported.
     */
    default List<T> findLimited(String collection, Document filter, Class<T> type, int limit) {
        List<T> values = find(collection, filter, type);
        if (limit <= 0 || values.size() <= limit) {
            return values;
        }
        return values.subList(0, limit);
    }

    /**
     * Counts entities matching a filter.
     */
    long count(String collection, Document filter);

    /**
     * Executes an aggregation pipeline and returns raw documents.
     */
    List<Document> aggregate(String collection, List<Document> pipeline);

    /**
     * Deletes one entity by id.
     */
    void deleteById(String collection, String id);
}
