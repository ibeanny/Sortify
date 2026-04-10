package com.ibeanny.aisorter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import com.ibeanny.aisorter.model.ClassifiedLine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Queue;

@Service
public class OpenAiClassificationParser {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CategoryNormalizer categoryNormalizer;

    public OpenAiClassificationParser(CategoryNormalizer categoryNormalizer) {
        this.categoryNormalizer = categoryNormalizer;
    }

    public List<ClassifiedLine> parse(String responseBody, List<String> inputLines) throws IOException {
        String jsonText = extractTextContentFromOpenAiResponse(responseBody);
        List<Map<String, String>> parsedItems = parseJsonItems(jsonText);

        if (parsedItems.size() != inputLines.size()) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned %d classified lines for %d input lines.".formatted(parsedItems.size(), inputLines.size())
            );
        }

        Map<String, Queue<String>> categoriesByValue = new LinkedHashMap<>();
        for (Map<String, String> item : parsedItems) {
            String value = item.getOrDefault("value", "").trim();
            if (value.isEmpty()) {
                throw new OpenAiIntegrationException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI returned a classified line without a value."
                );
            }

            String rawCategory = item.getOrDefault("category", "").trim();
            String category = categoryNormalizer.normalizeOrThrow(rawCategory, value);
            categoriesByValue.computeIfAbsent(value, key -> new ArrayDeque<>()).add(category);
        }

        List<ClassifiedLine> classifiedLines = new ArrayList<>();
        for (String inputLine : inputLines) {
            Queue<String> categories = categoriesByValue.get(inputLine);
            if (categories == null || categories.isEmpty()) {
                throw new OpenAiIntegrationException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI changed, dropped, or duplicated a line instead of returning the original text."
                );
            }

            classifiedLines.add(new ClassifiedLine(inputLine, categories.remove(), inputLine, 1.0));
        }

        for (Queue<String> remainingCategories : categoriesByValue.values()) {
            if (!remainingCategories.isEmpty()) {
                throw new OpenAiIntegrationException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI changed, dropped, or duplicated a line instead of returning the original text."
                );
            }
        }

        if (classifiedLines.isEmpty()) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI did not return any usable classified lines."
            );
        }

        return classifiedLines;
    }

    private String extractTextContentFromOpenAiResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode output = root.path("output");

        if (!output.isArray() || output.isEmpty()) {
            throw new OpenAiIntegrationException(HttpStatus.BAD_GATEWAY, "OpenAI response did not contain any output.");
        }

        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                String text = contentItem.path("text").asText("").trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        throw new OpenAiIntegrationException(HttpStatus.BAD_GATEWAY, "OpenAI response did not contain any text output.");
    }

    private List<Map<String, String>> parseJsonItems(String jsonText) throws IOException {
        try {
            return objectMapper.readValue(jsonText, new TypeReference<List<Map<String, String>>>() {});
        } catch (JsonMappingException e) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned text, but it was not valid classification JSON.",
                    e
            );
        } catch (IOException e) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned text, but it was not valid classification JSON.",
                    e
            );
        }
    }
}
