package org.example.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

/**
 * @author
 * Victor Weniger
 */

/**
 * DatabaseIndexInitializer service
 */
public class DatabaseIndexInitializer {
    private final MongoDatabase database;

/**
 * Constructor
 */
    public DatabaseIndexInitializer(MongoClient mongoClient, String databaseName) {
        this.database = mongoClient.getDatabase(databaseName);
    }

/**
 * Method
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
