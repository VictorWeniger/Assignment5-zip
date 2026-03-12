package org.example.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.example.config.DatabaseConfig;
import org.example.util.DocumentMapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author
 * Victor Weniger
 */

/**
 * MongoDatabaseHandler database
 */
public class MongoDatabaseHandler<T> implements DatabaseHandler<T>, AutoCloseable {
    private final MongoClient mongoClient;
    private final MongoDatabase database;

/**
 * Constructor
 */
    public MongoDatabaseHandler(DatabaseConfig config) {
        this(MongoClients.create(config.connectionString()), config.databaseName());
    }

/**
 * Constructor
 */
    public MongoDatabaseHandler(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.database = mongoClient.getDatabase(databaseName);
    }

    @Override

/**
 * Method
 */
    public void insert(String collection, T entity) {
        getCollection(collection).insertOne(DocumentMapper.toDocument(entity));
    }

    @Override

/**
 * Method
 */
    public void replaceById(String collection, String id, T entity) {
        getCollection(collection).replaceOne(Filters.eq("id", id), DocumentMapper.toDocument(entity), new ReplaceOptions().upsert(true));
    }

    @Override

/**
 * Method
 */
    public Optional<T> findById(String collection, String id, Class<T> type) {
        Document document = getCollection(collection).find(Filters.eq("id", id)).first();
        if (document == null) {
            return Optional.empty();
        }
        return Optional.of(DocumentMapper.fromDocument(document, type));
    }

    @Override

/**
 * Method
 */
    public List<T> find(String collection, Document filter, Class<T> type) {
        List<T> values = new ArrayList<>();
        for (Document document : getCollection(collection).find(filter)) {
            values.add(DocumentMapper.fromDocument(document, type));
        }
        return values;
    }

    @Override

/**
 * Method
 */
    public List<T> findLimited(String collection, Document filter, Class<T> type, int limit) {
        if (limit <= 0) {
            return find(collection, filter, type);
        }
        List<T> values = new ArrayList<>();
        for (Document document : getCollection(collection).find(filter).limit(limit)) {
            values.add(DocumentMapper.fromDocument(document, type));
        }
        return values;
    }

    @Override

/**
 * Method
 */
    public long count(String collection, Document filter) {
        return getCollection(collection).countDocuments(filter);
    }

    @Override

/**
 * Method
 */
    public List<Document> aggregate(String collection, List<Document> pipeline) {
        List<Document> values = new ArrayList<>();
        for (Document document : getCollection(collection).aggregate(pipeline)) {
            values.add(document);
        }
        return values;
    }

    @Override

/**
 * Method
 */
    public void deleteById(String collection, String id) {
        getCollection(collection).deleteOne(Filters.eq("id", id));
    }

    @Override

/**
 * Method
 */
    public void close() {
        mongoClient.close();
    }

/**
 * Method
 */
    public MongoClient mongoClient() {
        return mongoClient;
    }

    private MongoCollection<Document> getCollection(String collection) {
        return database.getCollection(collection);
    }
}
