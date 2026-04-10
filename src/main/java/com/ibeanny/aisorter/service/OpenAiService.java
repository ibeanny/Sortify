package com.ibeanny.aisorter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibeanny.aisorter.config.OpenAiConfig;
import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final String MODEL_NAME = "gpt-4.1-mini";

    private final OpenAiConfig config;
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final CategorySchema categorySchema;

    public OpenAiService(OpenAiConfig config, CategorySchema categorySchema) {
        this.config = config;
        this.categorySchema = categorySchema;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String testConnection() throws IOException, InterruptedException {
        return sendResponsesRequest("Say hello in one short sentence.");
    }

    public String processLines(List<String> lines) throws IOException, InterruptedException {
        String joinedLines = String.join("\n", lines);

        String prompt = """
        You are a smart document organizer.

        Your job is to classify each line into one stable category from a fixed schema.

        The file may contain mixed content such as tasks, schedules, contacts, finances, shopping lists, medical notes, legal records, travel details, work notes, school notes, and general reference text.

        You must assign every line to exactly one of these allowed categories:
        %s

        Instructions:
        1. Read all lines carefully.
        2. Use only the allowed categories listed above.
        3. Do not invent new category names.
        4. Pick the single best category for each line.
        5. Return exactly one JSON item for each input line.
        6. Keep the output items in the same order as the input lines.
        7. Copy each "value" exactly from the original input line with no rewriting.
        8. Use "Reference" for factual identifiers or structured details that do not fit a more specific category.
        9. Use "Other" only when no listed category fits reasonably well.
        10. Return ONLY valid JSON.
        11. Do not include markdown, code fences, explanations, or extra text.

        Return this exact JSON format:
        [
          {
            "category": "One allowed category exactly as written above",
            "value": "Original line here"
          }
        ]

        Lines:
        %s
        """.formatted(categorySchema.toPromptList(), joinedLines);

        return sendResponsesRequest(prompt);
    }

    private String sendResponsesRequest(String input) throws IOException, InterruptedException {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI API key is missing. Set OPENAI_API_KEY and restart the backend."
            );
        }

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("model", MODEL_NAME);
        requestPayload.put("input", input);

        String requestBody = objectMapper.writeValueAsString(requestPayload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(RESPONSES_URI)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (IOException e) {
            throw new OpenAiIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to reach OpenAI.",
                    e
            );
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new OpenAiIntegrationException(
                    mapUpstreamStatus(response.statusCode()),
                    buildOpenAiErrorMessage(response)
            );
        }

        return response.body();
    }

    private HttpStatus mapUpstreamStatus(int upstreamStatus) {
        return HttpStatus.BAD_GATEWAY;
    }

    private String buildOpenAiErrorMessage(HttpResponse<String> response) {
        String fallback = "OpenAI request failed with status %d.".formatted(response.statusCode());

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode errorNode = root.path("error");
            String message = errorNode.path("message").asText("").trim();

            if (!message.isEmpty()) {
                return "OpenAI error: " + message;
            }
        } catch (Exception ignored) {
            // Fall back to a generic message if the upstream error body is not JSON.
        }

        return fallback;
    }
}
