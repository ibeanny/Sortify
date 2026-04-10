package com.ibeanny.aisorter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import com.ibeanny.aisorter.model.ClassifiedLine;
import com.ibeanny.aisorter.model.CombinedCategoryGroup;
import com.ibeanny.aisorter.model.ProcessResponse;
import com.ibeanny.aisorter.model.ProcessedFileResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DocumentProcessingService {
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> CATEGORY_STOP_WORDS = Set.of(
            "detail", "details", "info", "information", "item", "items", "entry", "entries",
            "record", "records", "list", "lists", "section", "sections", "data",
            "personal", "medical", "health", "healthcare", "general", "misc", "miscellaneous",
            "dental"
    );
    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "Tasks & Reminders",
            "Appointments & Schedule",
            "Contacts",
            "Travel",
            "Finance",
            "Shopping & Orders",
            "Events & Dates",
            "Medical",
            "Legal",
            "Work & School",
            "Reference",
            "Other"
    );

    private final OpenAiService openAiService;
    private final UploadValidationService uploadValidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentProcessingService(OpenAiService openAiService, UploadValidationService uploadValidationService) {
        this.openAiService = openAiService;
        this.uploadValidationService = uploadValidationService;
    }

    public ProcessResponse processFiles(MultipartFile[] files) throws IOException, InterruptedException {
        uploadValidationService.validateFiles(files);

        List<ProcessedFileResult> processedFiles = new ArrayList<>();
        int totalLines = 0;

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();

            if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
                throw new IllegalArgumentException("Only .txt files are allowed.");
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> cleanedLines = extractCleanLines(content);
            if (cleanedLines.isEmpty()) {
                processedFiles.add(new ProcessedFileResult(fileName, List.of(), List.of()));
                continue;
            }

            String aiRawResponse = openAiService.processLines(cleanedLines);
            String jsonText = extractTextContentFromOpenAiResponse(aiRawResponse);
            List<ClassifiedLine> classifiedLines = parseClassifiedLines(jsonText);
            Set<String> categorySet = new LinkedHashSet<>();
            for (ClassifiedLine classifiedLine : classifiedLines) {
                categorySet.add(classifiedLine.getCategory());
            }

            List<String> discoveredCategories = new ArrayList<>(categorySet);

            processedFiles.add(new ProcessedFileResult(fileName, discoveredCategories, classifiedLines));
            totalLines += cleanedLines.size();
        }

        List<CombinedCategoryGroup> combinedCategories = buildCombinedCategories(processedFiles);

        return new ProcessResponse(processedFiles, combinedCategories, processedFiles.size(), totalLines);
    }

    private List<String> extractCleanLines(String content) {
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

    private List<CombinedCategoryGroup> buildCombinedCategories(List<ProcessedFileResult> processedFiles) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        for (ProcessedFileResult fileResult : processedFiles) {
            for (ClassifiedLine item : fileResult.getItems()) {
                grouped.computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
                        .add(item.getValue());
            }
        }

        List<CombinedCategoryGroup> combined = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            combined.add(new CombinedCategoryGroup(entry.getKey(), entry.getValue()));
        }

        return combined;
    }

    private String normalizeCombinedCategoryKey(String category) {
        if (category == null || category.isBlank()) {
            return "uncategorized";
        }

        String normalized = category.toLowerCase(Locale.ROOT).trim();
        normalized = NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll(" ").trim();

        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank() || CATEGORY_STOP_WORDS.contains(token)) {
                continue;
            }

            tokens.add(singularizeToken(token));
        }

        if (tokens.isEmpty()) {
            return "uncategorized";
        }

        return String.join(" ", tokens);
    }

    private String singularizeToken(String token) {
        if (token.endsWith("ies") && token.length() > 3) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.endsWith("s") && token.length() > 3 && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private String canonicalizeCategory(String rawCategory, String value) {
        String trimmedCategory = rawCategory == null ? "" : rawCategory.trim();
        if (ALLOWED_CATEGORIES.contains(trimmedCategory)) {
            return trimmedCategory;
        }

        Set<String> tokens = collectTokens(trimmedCategory + " " + (value == null ? "" : value));

        if (containsAny(tokens, "medical", "patient", "symptom", "prescription", "medication", "doctor", "provider", "insurance", "vital", "clinical")) {
            return "Medical";
        }
        if (containsAny(tokens, "legal", "case", "court", "hearing", "attorney", "plaintiff", "defendant", "claim", "lawsuit", "filing")) {
            return "Legal";
        }
        if (containsAny(tokens, "appointment", "meeting", "schedule", "scheduled", "calendar", "booking", "reservation")) {
            return "Appointments & Schedule";
        }
        if (containsAny(tokens, "task", "todo", "reminder", "remember", "followup", "follow", "deadline")) {
            return "Tasks & Reminders";
        }
        if (containsAny(tokens, "contact", "phone", "email", "call", "text", "message", "communication")) {
            return "Contacts";
        }
        if (containsAny(tokens, "travel", "flight", "hotel", "trip", "airport", "booking", "itinerary")) {
            return "Travel";
        }
        if (containsAny(tokens, "bill", "payment", "expense", "rent", "subscription", "membership", "invoice", "financial", "finance", "amount", "cost")) {
            return "Finance";
        }
        if (containsAny(tokens, "shopping", "grocery", "groceries", "purchase", "order", "delivery", "package", "amazon", "buy")) {
            return "Shopping & Orders";
        }
        if (containsAny(tokens, "birthday", "event", "occasion", "anniversary", "holiday", "date")) {
            return "Events & Dates";
        }
        if (containsAny(tokens, "school", "class", "professor", "prof", "course", "homework", "assignment", "exam", "study", "work", "project", "meeting")) {
            return "Work & School";
        }
        if (containsAny(tokens, "id", "number", "reference", "record", "identifier", "confirmation")) {
            return "Reference";
        }

        if (trimmedCategory.isBlank()) {
            throw new OpenAiIntegrationException(HttpStatus.BAD_GATEWAY, "OpenAI returned a classified line without a category.");
        }

        throw new OpenAiIntegrationException(
                HttpStatus.BAD_GATEWAY,
                "OpenAI returned an unsupported category: %s".formatted(trimmedCategory)
        );
    }

    private Set<String> collectTokens(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).trim();
        normalized = NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll(" ").trim();

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank() || CATEGORY_STOP_WORDS.contains(token)) {
                continue;
            }
            tokens.add(singularizeToken(token));
        }
        return tokens;
    }

    private boolean containsAny(Set<String> tokens, String... candidates) {
        for (String candidate : candidates) {
            if (tokens.contains(candidate)) {
                return true;
            }
        }
        return false;
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

    private List<ClassifiedLine> parseClassifiedLines(String jsonText) throws IOException {
        List<Map<String, String>> parsedItems;
        try {
            parsedItems = objectMapper.readValue(jsonText, new TypeReference<List<Map<String, String>>>() {});
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

        List<ClassifiedLine> classifiedLines = new ArrayList<>();
        for (Map<String, String> item : parsedItems) {
            String value = item.getOrDefault("value", "").trim();
            if (value.isEmpty()) {
                continue;
            }

            String rawCategory = item.getOrDefault("category", "").trim();
            String category = canonicalizeCategory(rawCategory, value);
            classifiedLines.add(new ClassifiedLine(value, category, value, 1.0));
        }

        if (classifiedLines.isEmpty()) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI did not return any usable classified lines."
            );
        }

        return classifiedLines;
    }
}
