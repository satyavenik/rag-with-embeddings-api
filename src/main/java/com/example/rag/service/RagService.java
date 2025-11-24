package com.example.rag.service;

import com.example.rag.model.SearchRequest;
import com.example.rag.model.SearchResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final OpenAiService openAiService;

    private static final String RAG_PROMPT_TEMPLATE = """
        Use the following context to answer the question. If you cannot answer the question based on the context, say so.
        
        Context:
        %s
        
        Question: %s
        
        Answer:
        """;

    public SearchResponse search(SearchRequest request) {
        log.info("Searching for: {}", request.getQuery());
        
        // Search for similar documents in vector store
        List<VectorStoreService.DocumentEmbedding> similarDocuments = 
            vectorStoreService.similaritySearch(request.getQuery(), request.getTopK());
        
        log.info("Found {} similar documents", similarDocuments.size());
        
        if (similarDocuments.isEmpty()) {
            return SearchResponse.builder()
                .answer("No relevant documents found.")
                .relevantDocuments(List.of())
                .query(request.getQuery())
                .build();
        }
        
        // Extract content from documents
        String context = similarDocuments.stream()
            .map(VectorStoreService.DocumentEmbedding::getContent)
            .collect(Collectors.joining("\n\n"));
        
        // Create prompt with context
        String prompt = String.format(RAG_PROMPT_TEMPLATE, context, request.getQuery());
        
        // Get answer from OpenAI using ChatClient
        String answer = generateAnswer(prompt);
        
        // Extract document snippets
        List<String> relevantDocs = similarDocuments.stream()
            .map(doc -> {
                String content = doc.getContent();
                return content.length() > 200 ? content.substring(0, 200) + "..." : content;
            })
            .collect(Collectors.toList());
        
        return SearchResponse.builder()
            .answer(answer)
            .relevantDocuments(relevantDocs)
            .query(request.getQuery())
            .build();
    }

    private String generateAnswer(String prompt) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(500)
                .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("Error generating answer", e);
            return "Error generating answer: " + e.getMessage();
        }
    }
}
