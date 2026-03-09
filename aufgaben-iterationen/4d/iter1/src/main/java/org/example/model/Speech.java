package org.example.model;

/**
 * Developer guide: Core domain model for speech text, NLP metadata, comments, and processing status.
 */

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Speech aggregate containing text, speaker, comments, NLP annotations, and processing metadata.
 */
public class Speech implements Identifiable {
    private String id;
    private String protocolId;
    private String sessionId;
    private int agendaItem;
    private Deputy speaker;
    private String text;
    private Instant startedAt;
    private Instant endedAt;
    private final List<Comment> comments = new ArrayList<>();
    private Map<String, Object> nlp = new HashMap<>();
    private List<Object> topics = new ArrayList<>();
    private Map<String, Integer> posDistribution = new HashMap<>();
    private List<Object> namedEntities = new ArrayList<>();
    private List<Double> sentenceSentiments = new ArrayList<>();
    private List<Double> sentiments = new ArrayList<>();
    private boolean nlpProcessed;
    private Instant nlpProcessedAt;

    /**
     * Returns speech id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets speech id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns protocol id.
     */
    public String getProtocolId() {
        return protocolId;
    }

    /**
     * Sets protocol id.
     */
    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    /**
     * Returns session id.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets session id.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Returns agenda item number.
     */
    public int getAgendaItem() {
        return agendaItem;
    }

    /**
     * Sets agenda item number.
     */
    public void setAgendaItem(int agendaItem) {
        this.agendaItem = agendaItem;
    }

    /**
     * Returns speaker profile.
     */
    public Deputy getSpeaker() {
        return speaker;
    }

    /**
     * Sets speaker profile.
     */
    public void setSpeaker(Deputy speaker) {
        this.speaker = speaker;
    }

    /**
     * Returns speech text.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets speech text.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns speech start timestamp.
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Sets speech start timestamp.
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Returns speech end timestamp.
     */
    public Instant getEndedAt() {
        return endedAt;
    }

    /**
     * Sets speech end timestamp.
     */
    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    /**
     * Returns mutable comment list.
     */
    public List<Comment> getComments() {
        return comments;
    }

    /**
     * Returns NLP metadata map.
     */
    public Map<String, Object> getNlp() {
        return nlp;
    }

    /**
     * Sets NLP metadata map.
     */
    public void setNlp(Map<String, Object> nlp) {
        this.nlp = nlp;
    }

    /**
     * Returns topic annotations.
     */
    public List<Object> getTopics() {
        return topics;
    }

    /**
     * Sets topic annotations.
     */
    public void setTopics(List<Object> topics) {
        this.topics = topics;
    }

    /**
     * Returns POS distribution.
     */
    public Map<String, Integer> getPosDistribution() {
        return posDistribution;
    }

    /**
     * Sets POS distribution.
     */
    public void setPosDistribution(Map<String, Integer> posDistribution) {
        this.posDistribution = posDistribution;
    }

    /**
     * Returns named entity annotations.
     */
    public List<Object> getNamedEntities() {
        return namedEntities;
    }

    /**
     * Sets named entity annotations.
     */
    public void setNamedEntities(List<Object> namedEntities) {
        this.namedEntities = namedEntities;
    }

    /**
     * Returns sentence-level sentiment values.
     */
    public List<Double> getSentenceSentiments() {
        return sentenceSentiments;
    }

    /**
     * Sets sentence-level sentiment values.
     */
    public void setSentenceSentiments(List<Double> sentenceSentiments) {
        this.sentenceSentiments = sentenceSentiments;
    }

    /**
     * Returns overall sentiment values.
     */
    public List<Double> getSentiments() {
        return sentiments;
    }

    /**
     * Sets overall sentiment values.
     */
    public void setSentiments(List<Double> sentiments) {
        this.sentiments = sentiments;
    }

    /**
     * Returns whether NLP processing is completed.
     */
    public boolean isNlpProcessed() {
        return nlpProcessed;
    }

    /**
     * Sets NLP processing status.
     */
    public void setNlpProcessed(boolean nlpProcessed) {
        this.nlpProcessed = nlpProcessed;
    }

    /**
     * Returns NLP processing timestamp.
     */
    public Instant getNlpProcessedAt() {
        return nlpProcessedAt;
    }

    /**
     * Sets NLP processing timestamp.
     */
    public void setNlpProcessedAt(Instant nlpProcessedAt) {
        this.nlpProcessedAt = nlpProcessedAt;
    }
}
