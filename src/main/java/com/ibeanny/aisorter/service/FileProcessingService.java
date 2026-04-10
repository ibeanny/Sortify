package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.UploadResponse;
import com.ibeanny.aisorter.model.UploadedFileResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileProcessingService {
    private final UploadValidationService uploadValidationService;

    public FileProcessingService(UploadValidationService uploadValidationService) {
        this.uploadValidationService = uploadValidationService;
    }

    public UploadResponse processFiles(MultipartFile[] files) throws IOException {
        uploadValidationService.validateFiles(files);

        List<UploadedFileResult> results = new ArrayList<>();
        int totalLines = 0;

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();

            if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
                throw new IllegalArgumentException("Only .txt files are allowed.");
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            String[] rawLines = content.split("\\r?\\n");
            List<String> cleanedLines = new ArrayList<>();

            for (String line : rawLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    cleanedLines.add(trimmed);
                }
            }

            totalLines += cleanedLines.size();

            UploadedFileResult result = new UploadedFileResult(
                    fileName,
                    cleanedLines,
                    cleanedLines.size()
            );

            results.add(result);
        }

        return new UploadResponse(results, results.size(), totalLines);
    }
}
