package com.questionhub.desktop.model;

public record Folder(String id, String name, int sort, long createdAt, long updatedAt) {
    @Override public String toString() { return name; }
}
