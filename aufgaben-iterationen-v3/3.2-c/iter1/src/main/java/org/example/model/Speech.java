package org.example.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author
 * Victor Weniger
 */

/**
 * Speech data
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

    @Override

/**
 * Getter
 */
    public String getId() {
        return id;
    }

/**
 * Setter
 */
    public void setId(String id) {
        this.id = id;
    }

/**
 * Getter
 */
    public String getProtocolId() {
        return protocolId;
    }

/**
 * Setter
 */
    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

/**
 * Getter
 */
    public String getSessionId() {
        return sessionId;
    }

/**
 * Setter
 */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

/**
 * Getter
 */
    public int getAgendaItem() {
        return agendaItem;
    }

/**
 * Setter
 */
    public void setAgendaItem(int agendaItem) {
        this.agendaItem = agendaItem;
    }

/**
 * Getter
 */
    public Deputy getSpeaker() {
        return speaker;
    }

/**
 * Setter
 */
    public void setSpeaker(Deputy speaker) {
        this.speaker = speaker;
    }

/**
 * Getter
 */
    public String getText() {
        return text;
    }

/**
 * Setter
 */
    public void setText(String text) {
        this.text = text;
    }

/**
 * Getter
 */
    public Instant getStartedAt() {
        return startedAt;
    }

/**
 * Setter
 */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

/**
 * Getter
 */
    public Instant getEndedAt() {
        return endedAt;
    }

/**
 * Setter
 */
    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

/**
 * Getter
 */
    public List<Comment> getComments() {
        return comments;
    }

/**
 * Getter
 */
    public Map<String, Object> getNlp() {
        return nlp;
    }

/**
 * Setter
 */
    public void setNlp(Map<String, Object> nlp) {
        this.nlp = nlp;
    }

/**
 * Getter
 */
    public List<Object> getTopics() {
        return topics;
    }

/**
 * Setter
 */
    public void setTopics(List<Object> topics) {
        this.topics = topics;
    }

/**
 * Getter
 */
    public Map<String, Integer> getPosDistribution() {
        return posDistribution;
    }

/**
 * Setter
 */
    public void setPosDistribution(Map<String, Integer> posDistribution) {
        this.posDistribution = posDistribution;
    }

/**
 * Getter
 */
    public List<Object> getNamedEntities() {
        return namedEntities;
    }

/**
 * Setter
 */
    public void setNamedEntities(List<Object> namedEntities) {
        this.namedEntities = namedEntities;
    }

/**
 * Getter
 */
    public List<Double> getSentenceSentiments() {
        return sentenceSentiments;
    }

/**
 * Setter
 */
    public void setSentenceSentiments(List<Double> sentenceSentiments) {
        this.sentenceSentiments = sentenceSentiments;
    }

/**
 * Getter
 */
    public List<Double> getSentiments() {
        return sentiments;
    }

/**
 * Setter
 */
    public void setSentiments(List<Double> sentiments) {
        this.sentiments = sentiments;
    }

/**
 * Getter
 */
    public boolean isNlpProcessed() {
        return nlpProcessed;
    }

/**
 * Setter
 */
    public void setNlpProcessed(boolean nlpProcessed) {
        this.nlpProcessed = nlpProcessed;
    }

/**
 * Getter
 */
    public Instant getNlpProcessedAt() {
        return nlpProcessedAt;
    }

/**
 * Setter
 */
    public void setNlpProcessedAt(Instant nlpProcessedAt) {
        this.nlpProcessedAt = nlpProcessedAt;
    }
}
