package com.questionhub.desktop.model;

import java.util.List;

public record ArchiveData(int version, Long exportedAt, List<Folder> folders, List<QaItem> qaItems) {}
