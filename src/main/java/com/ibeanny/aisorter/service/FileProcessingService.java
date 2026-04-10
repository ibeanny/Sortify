package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.UploadResponse;
import com.ibeanny.aisorter.model.UploadedFileResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileProcessingService {
    private final UploadValidationService uploadValidationService;
    private final TextFileContentService textFileContentService;

    public FileProcessingService(UploadValidationService uploadValidationService, TextFileContentService textFileContentService) {
        this.uploadValidationService = uploadValidationService;
        this.textFileContentService = textFileContentService;
    }

    public UploadResponse processFiles(MultipartFile[] files) throws IOException {
        uploadValidationService.validateFiles(files);

        List<UploadedFileResult> results = new ArrayList<>();
        int totalLines = 0;

        for (MultipartFile file : files) {
            var cleanedFile = textFileContentService.readAndClean(file);
            List<String> cleanedLines = cleanedFile.getCleanedLines();

            totalLines += cleanedLines.size();

            UploadedFileResult result = new UploadedFileResult(
                    cleanedFile.getFileName(),
                    cleanedLines,
                    cleanedLines.size()
            );

            results.add(result);
        }

        return new UploadResponse(results, results.size(), totalLines);
    }
}
