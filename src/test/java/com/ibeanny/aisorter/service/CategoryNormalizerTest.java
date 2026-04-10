package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryNormalizerTest {
    private final CategoryNormalizer normalizer = new CategoryNormalizer(new CategorySchema());

    @Test
    void normalizeOrThrowMapsAppointmentVariants() {
        assertEquals("Appointments & Schedule", normalizer.normalizeOrThrow("Appointment Details", "Dentist appointment"));
        assertEquals("Appointments & Schedule", normalizer.normalizeOrThrow("Scheduling", "Meeting at 2pm"));
    }

    @Test
    void normalizeOrThrowMapsReferenceAndLegalVariants() {
        assertEquals("Reference", normalizer.normalizeOrThrow("Insurance Details", "Confirmation number 12"));
        assertEquals("Legal", normalizer.normalizeOrThrow("Case Information", "Court hearing date"));
    }

    @Test
    void normalizeOrThrowRejectsUnknownCategories() {
        assertThrows(OpenAiIntegrationException.class,
                () -> normalizer.normalizeOrThrow("Unmapped Bucket", "mystery content"));
    }
}
