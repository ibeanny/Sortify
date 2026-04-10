package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.CleanedTextFile;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFileContentServiceTest {
    private final TextFileContentService service = new TextFileContentService();

    @Test
    void cleanLinesTrimsAndDropsBlankLines() {
        assertEquals(List.of("alpha", "beta"), service.cleanLines(" alpha \n\n beta \r\n"));
    }

    @Test
    void readAndCleanReturnsFilenameAndLines() throws IOException {
        MockMultipartFile file = new MockMultipartFile("files", "notes.txt", "text/plain", " one \n two ".getBytes());

        CleanedTextFile cleanedFile = service.readAndClean(file);

        assertEquals("notes.txt", cleanedFile.getFileName());
        assertEquals(List.of("one", "two"), cleanedFile.getCleanedLines());
        assertEquals(2, cleanedFile.getLineCount());
    }
}
