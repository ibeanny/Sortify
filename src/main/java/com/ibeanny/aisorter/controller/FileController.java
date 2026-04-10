package com.ibeanny.aisorter.controller;

import com.ibeanny.aisorter.config.SortifySecurityProperties;
import com.ibeanny.aisorter.config.UploadLimitsProperties;
import com.ibeanny.aisorter.exception.OpenAiIntegrationException;
import com.ibeanny.aisorter.model.ApiErrorResponse;
import com.ibeanny.aisorter.model.ClientConfigResponse;
import com.ibeanny.aisorter.model.ProcessResponse;
import com.ibeanny.aisorter.model.UploadResponse;
import com.ibeanny.aisorter.service.DocumentProcessingService;
import com.ibeanny.aisorter.service.FileProcessingService;
import com.ibeanny.aisorter.service.OpenAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileProcessingService fileProcessingService;
    private final OpenAiService openAiService;
    private final DocumentProcessingService documentProcessingService;
    private final SortifySecurityProperties securityProperties;
    private final UploadLimitsProperties uploadLimitsProperties;

    public FileController(FileProcessingService fileProcessingService,
                          OpenAiService openAiService,
                          DocumentProcessingService documentProcessingService,
                          SortifySecurityProperties securityProperties,
                          UploadLimitsProperties uploadLimitsProperties) {
        this.fileProcessingService = fileProcessingService;
        this.openAiService = openAiService;
        this.documentProcessingService = documentProcessingService;
        this.securityProperties = securityProperties;
        this.uploadLimitsProperties = uploadLimitsProperties;
    }

    @GetMapping("/client-config")
    public ResponseEntity<ClientConfigResponse> clientConfig() {
        return ResponseEntity.ok(new ClientConfigResponse(
                securityProperties.isAccessTokenRequired(),
                uploadLimitsProperties.getMaxFiles(),
                uploadLimitsProperties.getMaxFileSizeBytes(),
                uploadLimitsProperties.getMaxTotalUploadBytes()
        ));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            UploadResponse response = fileProcessingService.processFiles(files);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiErrorResponse("Failed to read uploaded file(s)."));
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> processFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            ProcessResponse response = documentProcessingService.processFiles(files);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(e.getMessage()));
        } catch (OpenAiIntegrationException e) {
            return ResponseEntity.status(e.getStatus()).body(new ApiErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiErrorResponse("Failed to process uploaded file(s)."));
        }
    }

    @GetMapping("/test-ai")
    public ResponseEntity<?> testAI() {
        try {
            String result = openAiService.testConnection();
            return ResponseEntity.ok(result);
        } catch (OpenAiIntegrationException e) {
            return ResponseEntity.status(e.getStatus()).body(new ApiErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiErrorResponse(e.getMessage()));
        }
    }
}
