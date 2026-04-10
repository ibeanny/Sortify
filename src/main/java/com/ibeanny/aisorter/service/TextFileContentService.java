package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.CleanedTextFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TextFileContentService {

    public CleanedTextFile readAndClean(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        return new CleanedTextFile(fileName, cleanLines(content));
    }

    public List<String> cleanLines(String content) {
        String[] rawLines = content.split("\\r?\\n");
        List<String> cleanedLines = new ArrayList<>();

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                cleanedLines.add(trimmed);
            }
        }

        return cleanedLines;
    }
}
