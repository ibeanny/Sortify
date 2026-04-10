package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.config.UploadLimitsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadValidationServiceTest {

    @Test
    void validateFilesRejectsTooManyFiles() {
        UploadLimitsProperties properties = new UploadLimitsProperties();
        properties.setMaxFiles(1);
        UploadValidationService service = new UploadValidationService(properties);

        MockMultipartFile first = new MockMultipartFile("files", "one.txt", "text/plain", "a".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "two.txt", "text/plain", "b".getBytes());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateFiles(new MockMultipartFile[]{first, second})
        );

        assertEquals("You can upload up to 1 files at a time.", exception.getMessage());
    }

    @Test
    void validateFilesRejectsOversizedFile() {
        UploadLimitsProperties properties = new UploadLimitsProperties();
        properties.setMaxFileSizeBytes(3);
        UploadValidationService service = new UploadValidationService(properties);

        MockMultipartFile file = new MockMultipartFile("files", "one.txt", "text/plain", "abcd".getBytes());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateFiles(new MockMultipartFile[]{file})
        );

        assertEquals("Each file must be 3 bytes or smaller.", exception.getMessage());
    }

    @Test
    void validateFilesRejectsOversizedTotalUpload() {
        UploadLimitsProperties properties = new UploadLimitsProperties();
        properties.setMaxTotalUploadBytes(5);
        UploadValidationService service = new UploadValidationService(properties);

        MockMultipartFile first = new MockMultipartFile("files", "one.txt", "text/plain", "abc".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "two.txt", "text/plain", "def".getBytes());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateFiles(new MockMultipartFile[]{first, second})
        );

        assertEquals("The total upload size must be 5 bytes or smaller.", exception.getMessage());
    }

    @Test
    void validateFilesAllowsValidTxtFiles() {
        UploadValidationService service = new UploadValidationService(new UploadLimitsProperties());
        MockMultipartFile file = new MockMultipartFile("files", "one.txt", "text/plain", "hello".getBytes());

        assertDoesNotThrow(() -> service.validateFiles(new MockMultipartFile[]{file}));
    }

    @Test
    void validateFilesRejectsNonTxtFile() {
        UploadValidationService service = new UploadValidationService(new UploadLimitsProperties());
        MockMultipartFile file = new MockMultipartFile("files", "one.csv", "text/csv", "hello".getBytes());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateFiles(new MockMultipartFile[]{file})
        );

        assertEquals("Only .txt files are allowed.", exception.getMessage());
    }
}
