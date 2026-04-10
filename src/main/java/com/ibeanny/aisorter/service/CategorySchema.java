package com.ibeanny.aisorter.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorySchema {
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

    public List<String> getAllowedCategories() {
        return ALLOWED_CATEGORIES;
    }

    public boolean isAllowed(String category) {
        return ALLOWED_CATEGORIES.contains(category);
    }

    public String toPromptList() {
        StringBuilder builder = new StringBuilder();
        for (String category : ALLOWED_CATEGORIES) {
            builder.append("- ").append(category).append("\n");
        }
        return builder.toString().trim();
    }
}
