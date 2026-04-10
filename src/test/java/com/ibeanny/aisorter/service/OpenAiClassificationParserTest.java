package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import com.ibeanny.aisorter.model.ClassifiedLine;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiClassificationParserTest {
    private final OpenAiClassificationParser parser =
            new OpenAiClassificationParser(new CategoryNormalizer(new CategorySchema()));

    @Test
    void parseAcceptsExactLineCountAndOrder() throws IOException {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"},{\\"category\\":\\"Finance\\",\\"value\\":\\"Pay rent\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """;

        List<ClassifiedLine> lines = parser.parse(response, List.of("Call mom", "Pay rent"));

        assertEquals(2, lines.size());
        assertEquals("Tasks & Reminders", lines.get(0).getCategory());
        assertEquals("Finance", lines.get(1).getCategory());
    }

    @Test
    void parseRejectsChangedLineText() {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom now\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """;

        OpenAiIntegrationException exception = assertThrows(
                OpenAiIntegrationException.class,
                () -> parser.parse(response, List.of("Call mom"))
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void parseAcceptsReorderedItemsWhenValuesStillMatchExactly() throws IOException {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Finance\\",\\"value\\":\\"Pay rent\\"},{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """;

        List<ClassifiedLine> lines = parser.parse(response, List.of("Call mom", "Pay rent"));

        assertEquals(2, lines.size());
        assertEquals("Call mom", lines.get(0).getValue());
        assertEquals("Tasks & Reminders", lines.get(0).getCategory());
        assertEquals("Pay rent", lines.get(1).getValue());
        assertEquals("Finance", lines.get(1).getCategory());
    }

    @Test
    void parseRejectsDifferentItemCount() {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """;

        OpenAiIntegrationException exception = assertThrows(
                OpenAiIntegrationException.class,
                () -> parser.parse(response, List.of("Call mom", "Pay rent"))
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void parseRejectsUnsupportedCategories() {
        String response = """
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
                """;

        OpenAiIntegrationException exception = assertThrows(
                OpenAiIntegrationException.class,
                () -> parser.parse(response, List.of("mystery content"))
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void parseAcceptsDuplicateInputLinesWhenCountsStillMatch() throws IOException {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"},{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """;

        List<ClassifiedLine> lines = parser.parse(response, List.of("Call mom", "Call mom"));

        assertEquals(2, lines.size());
        assertEquals("Call mom", lines.get(0).getValue());
        assertEquals("Call mom", lines.get(1).getValue());
    }

    @Test
    void parseRejectsDuplicateReturnedLineWhenAnotherInputLineIsMissing() {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": "[{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"},{\\"category\\":\\"Tasks & Reminders\\",\\"value\\":\\"Call mom\\"}]"
                        }
                      ]
                    }
                  ]
                }
                """;

        OpenAiIntegrationException exception = assertThrows(
                OpenAiIntegrationException.class,
                () -> parser.parse(response, List.of("Call mom", "Pay rent"))
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }
}
