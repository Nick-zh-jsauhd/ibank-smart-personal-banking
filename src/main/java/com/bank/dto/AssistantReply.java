package com.bank.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssistantReply {
    private String intent;
    private String title;
    private String summary;
    private String safetyNote;
    private String narrative;
    private String answerSource = "RULE";
    private String modelName;
    private String fallbackNote;
    private final List<String> highlights = new ArrayList<String>();
    private final List<AssistantAction> actions = new ArrayList<AssistantAction>();

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSafetyNote() {
        return safetyNote;
    }

    public void setSafetyNote(String safetyNote) {
        this.safetyNote = safetyNote;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getAnswerSource() {
        return answerSource;
    }

    public void setAnswerSource(String answerSource) {
        this.answerSource = answerSource;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getFallbackNote() {
        return fallbackNote;
    }

    public void setFallbackNote(String fallbackNote) {
        this.fallbackNote = fallbackNote;
    }

    public List<String> getHighlights() {
        return Collections.unmodifiableList(highlights);
    }

    public void addHighlight(String highlight) {
        if (highlight != null && highlight.trim().length() > 0) {
            highlights.add(highlight.trim());
        }
    }

    public List<AssistantAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public void addAction(String label, String href, String style) {
        actions.add(new AssistantAction(label, href, style));
    }
}
