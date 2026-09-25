package com.questionhub.desktop.model;

public record QaItem(String id, String folderId, String question, String answer, long createdAt, long updatedAt) {
    public QaItem withContent(String newQuestion, String newAnswer, long now) {
        return new QaItem(id, folderId, newQuestion, newAnswer, createdAt, now);
    }
    @Override public String toString() { return question; }
}
