package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.ProcessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentProcessingServiceTest {
    private static DocumentProcessingService createService(OpenAiService openAiService,
                                                           OpenAiClassificationParser parser,
                                                           CombinedResultsBuilder combinedResultsBuilder) {
        return new DocumentProcessingService(
                openAiService,
                new UploadValidationService(new com.ibeanny.aisorter.config.UploadLimitsProperties()),
                new TextFileContentService(),
                parser,
                combinedResultsBuilder
        );
    }

    @Test
    void processFilesBuildsResultsFromSharedServices() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        OpenAiClassificationParser parser = mock(OpenAiClassificationParser.class);
        CombinedResultsBuilder combinedResultsBuilder = new CombinedResultsBuilder();
        DocumentProcessingService service = createService(openAiService, parser, combinedResultsBuilder);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "notes.txt",
                "text/plain",
                "Call mom\nPay rent\n".getBytes()
        );

        when(openAiService.processLines(List.of("Call mom", "Pay rent"))).thenReturn("{\"output\":[]}");
        when(parser.parse("{\"output\":[]}", List.of("Call mom", "Pay rent"))).thenReturn(List.of(
                new com.ibeanny.aisorter.model.ClassifiedLine("Call mom", "Tasks & Reminders", "Call mom", 1.0),
                new com.ibeanny.aisorter.model.ClassifiedLine("Pay rent", "Finance", "Pay rent", 1.0)
        ));

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{file});

        verify(openAiService).processLines(List.of("Call mom", "Pay rent"));
        verify(parser).parse("{\"output\":[]}", List.of("Call mom", "Pay rent"));
        assertEquals(1, response.getTotalFiles());
        assertEquals(2, response.getTotalLines());
        assertEquals(2, response.getFiles().get(0).getDiscoveredCategories().size());
        assertEquals("Tasks & Reminders", response.getCombinedCategories().get(0).getCategory());
        assertEquals("Finance", response.getCombinedCategories().get(1).getCategory());
    }

    @Test
    void processFilesSkipsOpenAiForEmptyTextFiles() throws IOException, InterruptedException {
        OpenAiService openAiService = mock(OpenAiService.class);
        OpenAiClassificationParser parser = mock(OpenAiClassificationParser.class);
        CombinedResultsBuilder combinedResultsBuilder = new CombinedResultsBuilder();
        DocumentProcessingService service = createService(openAiService, parser, combinedResultsBuilder);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "empty.txt",
                "text/plain",
                "\n  \n".getBytes()
        );

        ProcessResponse response = service.processFiles(new MockMultipartFile[]{file});

        assertEquals(0, response.getTotalLines());
        assertEquals(0, response.getFiles().get(0).getItems().size());
        assertEquals(0, response.getCombinedCategories().size());
    }
}
