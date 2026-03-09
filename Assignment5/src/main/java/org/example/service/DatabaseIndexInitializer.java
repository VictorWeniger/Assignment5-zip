package org.example.service;

/**
 * Developer guide: Creates/ensures Mongo indexes required for query performance and uniqueness.
 */

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

/**
 * Ensures required MongoDB indexes for all persisted collections.
 */
public class DatabaseIndexInitializer {
    private final MongoDatabase database;

    /**
     * Creates an index initializer for the configured database.
     */
    public DatabaseIndexInitializer(MongoClient mongoClient, String databaseName) {
        this.database = mongoClient.getDatabase(databaseName);
    }

    /**
     * Creates all required indexes (unique and non-unique).
     */
    public void ensureIndexes() {
        ensureUniqueIndex("protocols", "id");
        ensureUniqueIndex("sessions", "id");
        ensureIndex("sessions", "protocolId");

        ensureUniqueIndex("speeches", "id");
        ensureIndex("speeches", "protocolId");
        ensureIndex("speeches", "sessionId");
        ensureIndex("speeches", "agendaItem");
        ensureIndex("speeches", "speaker.id");
        ensureIndex("speeches", "speaker.parliamentaryGroup.shortName");
        ensureTextIndex("speeches", "text");

        ensureUniqueIndex("deputies", "id");

        ensureUniqueIndex("speech_videos", "id");
        ensureIndex("speech_videos", "speechId");

        ensureUniqueIndex("export_templates", "id");
    }

    private void ensureUniqueIndex(String collectionName, String field) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.createIndex(new Document(field, 1), new IndexOptions().unique(true));
    }

    private void ensureIndex(String collectionName, String field) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.createIndex(new Document(field, 1));
    }

    private void ensureTextIndex(String collectionName, String field) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.createIndex(new Document(field, "text"));
    }
}
