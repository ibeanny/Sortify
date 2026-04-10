package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import com.ibeanny.aisorter.model.ProcessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentProcessingServiceTest {
    private static DocumentProcessingService createService(OpenAiService openAiService) {
        return new DocumentProcessingService(openAiService, new UploadValidationService(new com.ibeanny.aisorter.config.UploadLimitsProperties()));
    }

    @Test
    void processFilesBuildsCombinedCategoriesFromAiOutput() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "notes.txt",
                "text/plain",
                "Alice Smith\n555-1234\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("Alice Smith", "555-1234"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Person\\",\\"value\\":\\"Alice Smith\\"},{\\"category\\":\\"Contact\\",\\"value\\":\\"555-1234\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{file});

        assertEquals(1, response.getTotalFiles());
        assertEquals(2, response.getTotalLines());
        assertEquals(2, response.getCombinedCategories().size());
        assertEquals("Other", response.getCombinedCategories().get(0).getCategory());
        assertEquals("Alice Smith", response.getCombinedCategories().get(0).getValues().get(0));
        assertEquals("Contacts", response.getCombinedCategories().get(1).getCategory());
        assertEquals("555-1234", response.getCombinedCategories().get(1).getValues().get(0));
    }

    @Test
    void processFilesRejectsMissingTextInAiResponse() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "notes.txt",
                "text/plain",
                "Alice Smith\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("Alice Smith"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": []
                    }
                  ]
                }
                """);

        OpenAiIntegrationException exception = assertThrows(
                OpenAiIntegrationException.class,
                () -> service.processFiles(new MockMultipartFile[]{file})
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void processFilesMapsAppointmentVariantsIntoFixedCategory() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "one.txt",
                "text/plain",
                "Dentist visit\n".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "two.txt",
                "text/plain",
                "Follow-up visit\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("Dentist visit"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Appointments\\",\\"value\\":\\"Dentist visit\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        when(openAiService.processLines(java.util.List.of("Follow-up visit"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Appointment Details\\",\\"value\\":\\"Follow-up visit\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{firstFile, secondFile});

        assertEquals(1, response.getCombinedCategories().size());
        assertEquals("Appointments & Schedule", response.getCombinedCategories().get(0).getCategory());
        assertEquals(2, response.getCombinedCategories().get(0).getValues().size());
    }

    @Test
    void processFilesMapsMixedReminderAndBillingVariantsIntoSchema() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "one.txt",
                "text/plain",
                "Call doctor\n".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "two.txt",
                "text/plain",
                "Pick up prescription\n".getBytes()
        );
        MockMultipartFile thirdFile = new MockMultipartFile(
                "files",
                "three.txt",
                "text/plain",
                "Buy groceries\n".getBytes()
        );
        MockMultipartFile fourthFile = new MockMultipartFile(
                "files",
                "four.txt",
                "text/plain",
                "Pay electricity bill\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("Call doctor"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Medical Appointments\\",\\"value\\":\\"Call doctor\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        when(openAiService.processLines(java.util.List.of("Pick up prescription"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Appointment Details\\",\\"value\\":\\"Pick up prescription\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        when(openAiService.processLines(java.util.List.of("Buy groceries"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Personal Reminders\\",\\"value\\":\\"Buy groceries\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        when(openAiService.processLines(java.util.List.of("Pay electricity bill"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Reminders\\",\\"value\\":\\"Pay electricity bill\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{firstFile, secondFile, thirdFile, fourthFile});

        assertEquals(3, response.getCombinedCategories().size());
        assertEquals("Medical", response.getCombinedCategories().get(0).getCategory());
        assertEquals(2, response.getCombinedCategories().get(0).getValues().size());
        assertEquals("Shopping & Orders", response.getCombinedCategories().get(1).getCategory());
        assertEquals(1, response.getCombinedCategories().get(1).getValues().size());
        assertEquals("Finance", response.getCombinedCategories().get(2).getCategory());
        assertEquals(1, response.getCombinedCategories().get(2).getValues().size());
    }

    @Test
    void processFilesUsesStableSchemaForLegalAndReferenceContent() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "one.txt",
                "text/plain",
                "Dentist visit\nCourt: New York Supreme Court\n".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "two.txt",
                "text/plain",
                "Amazon package\nInsurance ID: 1\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("Dentist visit", "Court: New York Supreme Court"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Dental Appointments\\",\\"value\\":\\"Dentist visit\\"},{\\"category\\":\\"Court Information\\",\\"value\\":\\"Court: New York Supreme Court\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        when(openAiService.processLines(java.util.List.of("Amazon package", "Insurance ID: 1"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Orders & Deliveries\\",\\"value\\":\\"Amazon package\\"},{\\"category\\":\\"Insurance Details\\",\\"value\\":\\"Insurance ID: 1\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{firstFile, secondFile});

        assertEquals("Appointments & Schedule", response.getCombinedCategories().get(0).getCategory());
        assertEquals("Legal", response.getCombinedCategories().get(1).getCategory());
        assertEquals("Shopping & Orders", response.getCombinedCategories().get(2).getCategory());
        assertEquals("Medical", response.getCombinedCategories().get(3).getCategory());
    }

    @Test
    void processFilesMapsLegalAndBillingVariantsIntoStableSchema() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "one.txt",
                "text/plain",
                "rent due\ncase hearing\n".getBytes()
        );
        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "two.txt",
                "text/plain",
                "membership renewal\nattorney name\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("rent due", "case hearing"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Financial Obligations\\",\\"value\\":\\"rent due\\"},{\\"category\\":\\"Scheduling\\",\\"value\\":\\"case hearing\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        when(openAiService.processLines(java.util.List.of("membership renewal", "attorney name"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Membership Renewals\\",\\"value\\":\\"membership renewal\\"},{\\"category\\":\\"Legal Representatives\\",\\"value\\":\\"attorney name\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{firstFile, secondFile});

        assertEquals(2, response.getCombinedCategories().size());
        assertEquals("Finance", response.getCombinedCategories().get(0).getCategory());
        assertEquals("Legal", response.getCombinedCategories().get(1).getCategory());
    }

    @Test
    void processFilesMapsGeneralScheduleItemsIntoAppointmentsSchema() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "notes.txt",
                "text/plain",
                "gym at 7\nmeeting friday\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("gym at 7", "meeting friday"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Schedule\\",\\"value\\":\\"gym at 7\\"},{\\"category\\":\\"Scheduling\\",\\"value\\":\\"meeting friday\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{file});

        assertEquals(1, response.getCombinedCategories().size());
        assertEquals("Appointments & Schedule", response.getCombinedCategories().get(0).getCategory());
        assertEquals(2, response.getCombinedCategories().get(0).getValues().size());
    }

    @Test
    void processFilesRejectsUnsupportedAiCategory() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService service = createService(openAiService);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "notes.txt",
                "text/plain",
                "mystery content\n".getBytes()
        );

        when(openAiService.processLines(java.util.List.of("mystery content"))).thenReturn("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Totally Custom Bucket\\",\\"value\\":\\"mystery content\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """);

        OpenAiIntegrationException exception = assertThrows(
                OpenAiIntegrationException.class,
                () -> service.processFiles(new MockMultipartFile[]{file})
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }
}
