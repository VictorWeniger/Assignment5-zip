package org.example.service.tex;

/**
 * Developer guide: Transforms speech data into TeX document sections using templates.
 */

import org.example.model.Comment;
import org.example.model.Speech;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Renders speeches into LaTeX using built-in defaults and optional template overrides.
 */
public class TeXSpeechExporter {
    /**
     * Exports speeches with default templates and protocol grouping.
     */
    public String exportSpeeches(String title, List<Speech> speeches) {
        return exportSpeeches(title, speeches, Map.of());
    }

    /**
     * Exports speeches with custom templates and protocol grouping.
     */
    public String exportSpeeches(String title, List<Speech> speeches, Map<String, String> templates) {
        return exportSpeeches(title, speeches, templates, "protocol", false);
    }

    /**
     * Exports speeches with custom templates and selected grouping.
     */
    public String exportSpeeches(String title, List<Speech> speeches, Map<String, String> templates, String groupByInput) {
        return exportSpeeches(title, speeches, templates, groupByInput, false);
    }

    /**
     * Exports speeches with optional grouping and TikZ statistics blocks.
     *
     * @param title document title
     * @param speeches speeches to render
     * @param templates template overrides keyed by template id
     * @param groupByInput one of protocol, speaker, faction, topic, none
     * @param includeTikz include a small TikZ bar visualization for NLP stats
     * @return complete LaTeX document source
     */
    public String exportSpeeches(String title, List<Speech> speeches, Map<String, String> templates, String groupByInput, boolean includeTikz) {
        GroupBy groupBy = GroupBy.parse(groupByInput);
        String defaultHeader = "\\\\documentclass[a4paper,11pt]{article}\\n"
                + "\\\\usepackage[T1]{fontenc}\\n"
                + "\\\\usepackage[utf8]{inputenc}\\n"
                + "\\\\usepackage[german]{babel}\\n"
                + "\\\\usepackage{hyperref}\\n"
                + "\\\\usepackage{longtable}\\n"
                + "\\\\usepackage{tikz}\\n"
                + "\\\\title{${title}}\\n"
                + "\\\\date{\\\\today}\\n"
                + "\\\\begin{document}\\n\\\\maketitle\\n\\\\tableofcontents\\n\\\\newpage\\n";
        String defaultFooter = "\\\\end{document}\\n";
        String defaultSpeechSection = "\\\\section{${groupType}: ${groupLabel}}\\n${speechBlock}\\n";
        String defaultSpeechEntry = ""
                + "\\\\subsection{${speakerName}${factionSuffix}}\\n"
                + "\\\\textbf{Rede-ID:} ${speechId}\\\\\\\\\\n"
                + "\\\\textbf{Sitzung:} ${sessionId}\\\\\\\\\\n"
                + "\\\\textbf{Tagesordnungspunkt:} ${agendaItem}\\\\\\\\\\n"
                + "${startedAt}${endedAt}"
                + "\\\\paragraph{NLP-Statistik}\\n${nlpStats}\\n"
                + "\\\\paragraph{Redeinhalt}\\n${speechText}\\n"
                + "${commentsBlock}\\n"
                + "\\\\medskip\\\\hrule\\\\medskip\\n";
        String defaultCommentEntry = "\\\\item [${commentMeta}] ${commentText}\\n";

        String headerTemplate = templates.getOrDefault("document-header", defaultHeader);
        String footerTemplate = templates.getOrDefault("document-footer", defaultFooter);
        String speechSectionTemplate = templates.getOrDefault("speech-section", defaultSpeechSection);
        String speechEntryTemplate = templates.getOrDefault("speech-entry", defaultSpeechEntry);
        String commentEntryTemplate = templates.getOrDefault("comment-entry", defaultCommentEntry);

        StringBuilder tex = new StringBuilder();
        tex.append(headerTemplate.replace("${title}", TeXEscaper.escape(title)));

        Map<String, List<Speech>> groupedByProtocol = new TreeMap<>();
        for (Speech speech : speeches) {
            String label = determineGroupLabel(speech, groupBy);
            groupedByProtocol.computeIfAbsent(label, ignored -> new ArrayList<>()).add(speech);
        }

        for (Map.Entry<String, List<Speech>> entry : groupedByProtocol.entrySet()) {
            StringBuilder block = new StringBuilder();
            for (Speech speech : entry.getValue()) {
                block.append(exportSpeech(speech, speechEntryTemplate, commentEntryTemplate, includeTikz));
            }
            tex.append(speechSectionTemplate
                    .replace("${protocol}", TeXEscaper.escape(entry.getKey()))
                    .replace("${groupType}", TeXEscaper.escape(groupBy.label()))
                    .replace("${groupLabel}", TeXEscaper.escape(entry.getKey()))
                    .replace("${speechBlock}", block.toString()));
        }

        tex.append(footerTemplate);
        return tex.toString();
    }

    private String exportSpeech(Speech speech, String speechEntryTemplate, String commentEntryTemplate, boolean includeTikz) {
        String speakerName = "Unbekannt";
        String faction = "";
        if (speech.getSpeaker() != null) {
            String first = safe(speech.getSpeaker().getFirstName());
            String last = safe(speech.getSpeaker().getLastName());
            speakerName = (first + " " + last).trim();
            if (speakerName.isBlank()) {
                speakerName = "Unbekannt";
            }
            if (speech.getSpeaker().getParliamentaryGroup() != null) {
                faction = safe(speech.getSpeaker().getParliamentaryGroup().getShortName());
            }
        }

        List<Comment> comments = speech.getComments();
        String commentsBlock = "";
        if (comments != null && !comments.isEmpty()) {
            commentsBlock = "\\\\paragraph{Kommentare und Zurufe}\\n" + renderCommentList(comments, commentEntryTemplate);
        }

        String startedAt = speech.getStartedAt() == null
                ? ""
                : "\\\\textbf{Beginn:} " + TeXEscaper.escape(DateTimeFormatter.ISO_INSTANT.format(speech.getStartedAt())) + "\\\\\\\\\n";
        String endedAt = speech.getEndedAt() == null
                ? ""
                : "\\\\textbf{Ende:} " + TeXEscaper.escape(DateTimeFormatter.ISO_INSTANT.format(speech.getEndedAt())) + "\\\\\\\\\n";

        return speechEntryTemplate
                .replace("${speakerName}", TeXEscaper.escape(speakerName))
                .replace("${faction}", TeXEscaper.escape(faction))
                .replace("${factionSuffix}", faction.isBlank() ? "" : " (" + TeXEscaper.escape(faction) + ")")
                .replace("${speechId}", TeXEscaper.escape(safe(speech.getId())))
                .replace("${sessionId}", TeXEscaper.escape(safe(speech.getSessionId())))
                .replace("${agendaItem}", String.valueOf(speech.getAgendaItem()))
                .replace("${startedAt}", startedAt)
                .replace("${endedAt}", endedAt)
                .replace("${nlpStats}", renderNlpStats(speech, includeTikz))
                .replace("${speechText}", TeXEscaper.escape(safe(speech.getText())).replace("\n", "\\n\\n"))
                .replace("${commentsBlock}", commentsBlock);
    }

    private String renderCommentList(List<Comment> comments, String commentEntryTemplate) {
        StringBuilder tex = new StringBuilder("\\begin{itemize}\n");
        for (Comment comment : comments) {
            String meta = safe(comment.getAuthorName());
            if (comment.getAuthorFaction() != null && !comment.getAuthorFaction().isBlank()) {
                meta += " (" + comment.getAuthorFaction() + ")";
            }
            tex.append(commentEntryTemplate
                    .replace("${commentMeta}", TeXEscaper.escape(meta))
                    .replace("${commentText}", TeXEscaper.escape(safe(comment.getText()))));
        }
        tex.append("\\end{itemize}\n");
        return tex.toString();
    }

    private String renderNlpStats(Speech speech, boolean includeTikz) {
        int topicCount = speech.getTopics() == null ? 0 : speech.getTopics().size();
        int entityCount = speech.getNamedEntities() == null ? 0 : speech.getNamedEntities().size();
        int sentimentCount = speech.getSentenceSentiments() == null ? 0 : speech.getSentenceSentiments().size();
        int posTags = speech.getPosDistribution() == null ? 0 : speech.getPosDistribution().size();
        String text = "\\\\begin{itemize}\\n"
                + "\\\\item Topics: " + topicCount + "\\n"
                + "\\\\item Named Entities: " + entityCount + "\\n"
                + "\\\\item Satz-Sentiments: " + sentimentCount + "\\n"
                + "\\\\item POS-Tags: " + posTags + "\\n"
                + "\\\\end{itemize}\\n";
        if (!includeTikz) {
            return text;
        }
        return text + renderNlpStatsTikz(topicCount, entityCount, sentimentCount, posTags);
    }

    private String renderNlpStatsTikz(int topicCount, int entityCount, int sentimentCount, int posTags) {
        int max = Math.max(1, Math.max(Math.max(topicCount, entityCount), Math.max(sentimentCount, posTags)));
        double scale = 4.0 / max;
        return ""
                + "\\\\begin{tikzpicture}[x=1cm,y=0.5cm]\\n"
                + "\\\\fill[teal!40] (0,0) rectangle (" + (topicCount * scale) + ",0.6);\\\\node[anchor=west] at (" + (topicCount * scale + 0.1) + ",0.3) {Topics};\\n"
                + "\\\\fill[blue!35] (0,1) rectangle (" + (entityCount * scale) + ",1.6);\\\\node[anchor=west] at (" + (entityCount * scale + 0.1) + ",1.3) {Entities};\\n"
                + "\\\\fill[orange!45] (0,2) rectangle (" + (sentimentCount * scale) + ",2.6);\\\\node[anchor=west] at (" + (sentimentCount * scale + 0.1) + ",2.3) {Sentiments};\\n"
                + "\\\\fill[gray!45] (0,3) rectangle (" + (posTags * scale) + ",3.6);\\\\node[anchor=west] at (" + (posTags * scale + 0.1) + ",3.3) {POS};\\n"
                + "\\\\end{tikzpicture}\\n";
    }

    private String determineGroupLabel(Speech speech, GroupBy groupBy) {
        return switch (groupBy) {
            case PROTOCOL -> safe(speech.getProtocolId()).isBlank() ? "unknown" : speech.getProtocolId();
            case SPEAKER -> {
                if (speech.getSpeaker() == null) {
                    yield "Unbekannt";
                }
                String first = safe(speech.getSpeaker().getFirstName());
                String last = safe(speech.getSpeaker().getLastName());
                String full = (first + " " + last).trim();
                yield full.isBlank() ? "Unbekannt" : full;
            }
            case FACTION -> {
                if (speech.getSpeaker() == null || speech.getSpeaker().getParliamentaryGroup() == null) {
                    yield "ohne Fraktion";
                }
                String faction = safe(speech.getSpeaker().getParliamentaryGroup().getShortName());
                yield faction.isBlank() ? "ohne Fraktion" : faction;
            }
            case TOPIC -> primaryTopic(speech);
            case NONE -> "Alle Reden";
        };
    }

    private String primaryTopic(Speech speech) {
        if (speech.getTopics() == null || speech.getTopics().isEmpty()) {
            return "ohne Topic";
        }
        Object first = speech.getTopics().getFirst();
        if (first instanceof Map<?, ?> map) {
            Object label = map.get("label");
            if (label == null) {
                label = map.get("topic");
            }
            if (label != null && !String.valueOf(label).isBlank()) {
                return String.valueOf(label);
            }
        }
        return "ohne Topic";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private enum GroupBy {
        PROTOCOL("Protokoll"),
        SPEAKER("Redner"),
        FACTION("Fraktion"),
        TOPIC("Topic"),
        NONE("Auswahl");

        private final String label;

        GroupBy(String label) {
            this.label = label;
        }

        static GroupBy parse(String value) {
            if (value == null || value.isBlank()) {
                return PROTOCOL;
            }
            return switch (value.trim().toLowerCase()) {
                case "protocol" -> PROTOCOL;
                case "speaker" -> SPEAKER;
                case "faction" -> FACTION;
                case "topic" -> TOPIC;
                case "none" -> NONE;
                default -> PROTOCOL;
            };
        }

        String label() {
            return label;
        }
    }
}
