package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.CleanedTextFile;
import com.ibeanny.aisorter.model.ClassifiedLine;
import com.ibeanny.aisorter.model.CombinedCategoryGroup;
import com.ibeanny.aisorter.model.ProcessResponse;
import com.ibeanny.aisorter.model.ProcessedFileResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class DocumentProcessingService {
    private final OpenAiService openAiService;
    private final UploadValidationService uploadValidationService;
    private final TextFileContentService textFileContentService;
    private final OpenAiClassificationParser openAiClassificationParser;
    private final CombinedResultsBuilder combinedResultsBuilder;

    public DocumentProcessingService(OpenAiService openAiService,
                                     UploadValidationService uploadValidationService,
                                     TextFileContentService textFileContentService,
                                     OpenAiClassificationParser openAiClassificationParser,
                                     CombinedResultsBuilder combinedResultsBuilder) {
        this.openAiService = openAiService;
        this.uploadValidationService = uploadValidationService;
        this.textFileContentService = textFileContentService;
        this.openAiClassificationParser = openAiClassificationParser;
        this.combinedResultsBuilder = combinedResultsBuilder;
    }

    public ProcessResponse processFiles(MultipartFile[] files) throws IOException, InterruptedException {
        uploadValidationService.validateFiles(files);

        List<ProcessedFileResult> processedFiles = new ArrayList<>();
        int totalLines = 0;

        for (MultipartFile file : files) {
            CleanedTextFile cleanedFile = textFileContentService.readAndClean(file);
            List<String> cleanedLines = cleanedFile.getCleanedLines();

            if (cleanedLines.isEmpty()) {
                processedFiles.add(new ProcessedFileResult(cleanedFile.getFileName(), List.of(), List.of()));
                continue;
            }

            String aiRawResponse = openAiService.processLines(cleanedLines);
            List<ClassifiedLine> classifiedLines = openAiClassificationParser.parse(aiRawResponse, cleanedLines);
            List<String> discoveredCategories = new ArrayList<>(
                    new LinkedHashSet<>(classifiedLines.stream().map(ClassifiedLine::getCategory).toList())
            );

            processedFiles.add(new ProcessedFileResult(cleanedFile.getFileName(), discoveredCategories, classifiedLines));
            totalLines += cleanedFile.getLineCount();
        }

        List<CombinedCategoryGroup> combinedCategories = combinedResultsBuilder.build(processedFiles);
        return new ProcessResponse(processedFiles, combinedCategories, processedFiles.size(), totalLines);
    }
}
