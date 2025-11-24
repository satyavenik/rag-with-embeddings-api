package com.example.rag.service;

import com.example.rag.model.DocumentUploadRequest;
import com.example.rag.model.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStoreService vectorStoreService;

    public DocumentUploadResponse uploadDocument(MultipartFile file) throws IOException {
        log.info("Uploading document: {}", file.getOriginalFilename());
        
        String content = new String(file.getBytes());
        String documentId = UUID.randomUUID().toString();
        
        // Split document into chunks
        List<String> chunks = splitIntoChunks(content, 500);
        
        log.info("Document split into {} chunks", chunks.size());
        
        // Store chunks in vector store
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", file.getOriginalFilename());
        metadata.put("documentId", documentId);
        metadata.put("uploadTime", String.valueOf(System.currentTimeMillis()));
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = documentId + "_chunk_" + i;
            vectorStoreService.addDocument(chunkId, chunks.get(i), metadata);
        }
        
        return DocumentUploadResponse.builder()
            .message("Document uploaded successfully")
            .documentId(documentId)
            .chunkCount(chunks.size())
            .build();
    }

    public DocumentUploadResponse uploadDocumentContent(DocumentUploadRequest request) {
        log.info("Uploading document content: {}", request.getFileName());
        
        String documentId = UUID.randomUUID().toString();
        
        // Split document into chunks
        List<String> chunks = splitIntoChunks(request.getContent(), 500);
        
        log.info("Document content split into {} chunks", chunks.size());
        
        // Store chunks in vector store
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileName", request.getFileName() != null ? request.getFileName() : "text-document");
        metadata.put("documentId", documentId);
        metadata.put("metadata", request.getMetadata() != null ? request.getMetadata() : "");
        metadata.put("uploadTime", String.valueOf(System.currentTimeMillis()));
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = documentId + "_chunk_" + i;
            vectorStoreService.addDocument(chunkId, chunks.get(i), metadata);
        }
        
        return DocumentUploadResponse.builder()
            .message("Document content uploaded successfully")
            .documentId(documentId)
            .chunkCount(chunks.size())
            .build();
    }

    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        StringBuilder chunk = new StringBuilder();
        int wordCount = 0;
        
        for (String word : words) {
            chunk.append(word).append(" ");
            wordCount++;
            
            if (wordCount >= chunkSize) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
                wordCount = 0;
            }
        }
        
        if (chunk.length() > 0) {
            chunks.add(chunk.toString().trim());
        }
        
        return chunks;
    }
}
