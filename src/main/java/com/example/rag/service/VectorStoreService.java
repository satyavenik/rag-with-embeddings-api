package com.example.rag.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final OpenAiService openAiService;
    private final Map<String, DocumentEmbedding> vectorStore = new ConcurrentHashMap<>();

    public void addDocument(String id, String content, Map<String, String> metadata) {
        log.info("Adding document with ID: {}", id);
        
        // Generate embedding for the document
        List<Double> embedding = generateEmbedding(content);
        
        // Store document with embedding
        vectorStore.put(id, new DocumentEmbedding(id, content, embedding, metadata));
        
        log.info("Document added successfully. Total documents: {}", vectorStore.size());
    }

    public List<DocumentEmbedding> similaritySearch(String query, int topK) {
        log.info("Performing similarity search for: {}", query);
        
        if (vectorStore.isEmpty()) {
            log.warn("Vector store is empty");
            return Collections.emptyList();
        }

        // Generate embedding for the query
        List<Double> queryEmbedding = generateEmbedding(query);

        // Calculate cosine similarity with all stored documents
        List<ScoredDocument> scoredDocs = vectorStore.values().stream()
            .map(doc -> new ScoredDocument(doc, cosineSimilarity(queryEmbedding, doc.getEmbedding())))
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .collect(Collectors.toList());

        log.info("Found {} similar documents", scoredDocs.size());
        
        return scoredDocs.stream()
            .map(ScoredDocument::getDocument)
            .collect(Collectors.toList());
    }

    private List<Double> generateEmbedding(String text) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                .model("text-embedding-ada-002")
                .input(Collections.singletonList(text))
                .build();
            
            EmbeddingResult result = openAiService.createEmbeddings(request);
            return result.getData().get(0).getEmbedding();
        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += vectorA.get(i) * vectorA.get(i);
            normB += vectorB.get(i) * vectorB.get(i);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Data
    @AllArgsConstructor
    public static class DocumentEmbedding {
        private String id;
        private String content;
        private List<Double> embedding;
        private Map<String, String> metadata;
    }

    @Data
    @AllArgsConstructor
    private static class ScoredDocument {
        private DocumentEmbedding document;
        private double score;
    }
}
