package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CategoryNormalizer {
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> CATEGORY_STOP_WORDS = Set.of(
            "detail", "details", "info", "information", "item", "items", "entry", "entries",
            "record", "records", "list", "lists", "section", "sections", "data",
            "personal", "medical", "health", "healthcare", "general", "misc", "miscellaneous",
            "dental"
    );

    private final CategorySchema categorySchema;

    public CategoryNormalizer(CategorySchema categorySchema) {
        this.categorySchema = categorySchema;
    }

    public String normalizeOrThrow(String rawCategory, String value) {
        String trimmedCategory = rawCategory == null ? "" : rawCategory.trim();
        if (categorySchema.isAllowed(trimmedCategory)) {
            return trimmedCategory;
        }

        Set<String> tokens = collectTokens(trimmedCategory + " " + (value == null ? "" : value));

        if (containsAny(tokens, "id", "number", "reference", "record", "identifier", "confirmation")
                && !containsAny(tokens, "patient", "symptom", "prescription", "medication", "doctor", "provider", "clinical",
                "court", "hearing", "attorney", "plaintiff", "defendant", "lawsuit", "filing")) {
            return "Reference";
        }
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
        if (containsAny(tokens, "school", "class", "professor", "prof", "course", "homework", "assignment", "exam", "study", "work", "project")) {
            return "Work & School";
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

    private String singularizeToken(String token) {
        if (token.endsWith("ies") && token.length() > 3) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.endsWith("s") && token.length() > 3 && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
