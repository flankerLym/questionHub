package com.questionhub.desktop.service;

import com.questionhub.desktop.model.ArchiveData;
import com.questionhub.desktop.model.Folder;
import com.questionhub.desktop.model.QaItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveJsonServiceTest {
    @Test void validArchivePasses() {
        Folder f = new Folder("folder_1", "Java", 0, 1, 1);
        QaItem q = new QaItem("qa_1", "folder_1", "问题", "答案", 1, 1);
        assertDoesNotThrow(() -> ArchiveJsonService.validate(new ArchiveData(1, 1L, List.of(f), List.of(q))));
    }

    @Test void danglingFolderIsRejected() {
        QaItem q = new QaItem("qa_1", "missing", "问题", "答案", 1, 1);
        assertThrows(IllegalArgumentException.class, () -> ArchiveJsonService.validate(new ArchiveData(1, 1L, List.of(), List.of(q))));
    }
}
