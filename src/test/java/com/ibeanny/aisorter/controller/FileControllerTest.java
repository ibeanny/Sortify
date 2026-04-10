package com.ibeanny.aisorter.controller;

import com.ibeanny.aisorter.config.SortifyAccessTokenInterceptor;
import com.ibeanny.aisorter.config.SortifySecurityProperties;
import com.ibeanny.aisorter.config.UploadLimitsProperties;
import com.ibeanny.aisorter.service.DocumentProcessingService;
import com.ibeanny.aisorter.service.FileProcessingService;
import com.ibeanny.aisorter.service.OpenAiService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileControllerTest {

    @Test
    void processFilesReturnsStructuredBadRequest() throws Exception {
        FileProcessingService fileProcessingService = mock(FileProcessingService.class);
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService documentProcessingService = mock(DocumentProcessingService.class);
        SortifySecurityProperties securityProperties = new SortifySecurityProperties();
        UploadLimitsProperties uploadLimitsProperties = new UploadLimitsProperties();

        doThrow(new IllegalArgumentException("Each file must be 1 MB or smaller."))
                .when(documentProcessingService).processFiles(any());

        FileController controller = new FileController(
                fileProcessingService,
                openAiService,
                documentProcessingService,
                securityProperties,
                uploadLimitsProperties
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("files", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

        mockMvc.perform(multipart("/api/files/process").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Each file must be 1 MB or smaller."));
    }

    @Test
    void processFilesRequiresAccessTokenWhenConfigured() throws Exception {
        FileProcessingService fileProcessingService = mock(FileProcessingService.class);
        OpenAiService openAiService = mock(OpenAiService.class);
        DocumentProcessingService documentProcessingService = mock(DocumentProcessingService.class);
        SortifySecurityProperties securityProperties = new SortifySecurityProperties();
        securityProperties.setAccessToken("secret-token");
        UploadLimitsProperties uploadLimitsProperties = new UploadLimitsProperties();

        FileController controller = new FileController(
                fileProcessingService,
                openAiService,
                documentProcessingService,
                securityProperties,
                uploadLimitsProperties
        );

        SortifyAccessTokenInterceptor interceptor = new SortifyAccessTokenInterceptor(securityProperties);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .build();

        MockMultipartFile file = new MockMultipartFile("files", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

        mockMvc.perform(multipart("/api/files/process").file(file))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        multipart("/api/files/process")
                                .file(file)
                                .header(SortifyAccessTokenInterceptor.ACCESS_TOKEN_HEADER, "secret-token")
                )
                .andExpect(status().isOk());
    }
}
