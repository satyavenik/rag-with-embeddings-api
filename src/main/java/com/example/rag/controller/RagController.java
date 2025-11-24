package com.example.rag.controller;

import com.example.rag.model.DocumentUploadRequest;
import com.example.rag.model.DocumentUploadResponse;
import com.example.rag.model.SearchRequest;
import com.example.rag.model.SearchResponse;
import com.example.rag.service.DocumentService;
import com.example.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG API", description = "Retrieval Augmented Generation API with vector embeddings")
public class RagController {

    private final DocumentService documentService;
    private final RagService ragService;

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document file", description = "Upload a document file to the vector database")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Received file upload request: {}", file.getOriginalFilename());
            DocumentUploadResponse response = documentService.uploadDocument(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentUploadResponse.builder()
                            .message("Error uploading document: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/documents/upload-content")
    @Operation(summary = "Upload document content", description = "Upload document content directly to the vector database")
    public ResponseEntity<DocumentUploadResponse> uploadDocumentContent(
            @RequestBody DocumentUploadRequest request) {
        log.info("Received document content upload request");
        DocumentUploadResponse response = documentService.uploadDocumentContent(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    @Operation(summary = "Search documents", description = "Search documents using RAG with embeddings")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        log.info("Received search request: {}", request.getQuery());
        SearchResponse response = ragService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents (GET)", description = "Search documents using RAG with embeddings via GET request")
    public ResponseEntity<SearchResponse> searchGet(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        log.info("Received search GET request: {}", query);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        SearchResponse response = ragService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the RAG API is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG API is running");
    }
}
