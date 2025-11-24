# RAG API Documentation
## Retrieval Augmented Generation with Vector Embeddings

### Table of Contents
1. [Overview](#overview)
2. [Document Upload Process](#document-upload-process)
3. [Search Endpoints](#search-endpoints)
4. [API Reference](#api-reference)
5. [Architecture Details](#architecture-details)
6. [Example Usage](#example-usage)

---

## Overview

This RAG (Retrieval Augmented Generation) API provides functionality to:
- Upload documents and convert them to vector embeddings
- Store document chunks in an in-memory vector database
- Search through documents using semantic similarity
- Generate contextual answers using OpenAI's GPT models

The system uses OpenAI's `text-embedding-ada-002` model for generating embeddings and `gpt-3.5-turbo` for answer generation.

---

## Document Upload Process

### Step-by-Step Document Upload Flow

#### 1. Document Reception
**File**: `RagController.java` - `uploadDocument()` method
```java
@PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file)
```

**What happens:**
- Receives a `MultipartFile` through HTTP POST request
- Logs the incoming file name for tracking
- Validates the file is not empty and readable

#### 2. File Content Extraction
**File**: `DocumentService.java` - `uploadDocument()` method
```java
String content = new String(file.getBytes());
String documentId = UUID.randomUUID().toString();
```

**What happens:**
- Converts the uploaded file bytes to a UTF-8 string
- Generates a unique document ID using UUID for tracking
- Assumes the file contains text content (PDF, Word docs need additional parsing)

#### 3. Document Chunking
**File**: `DocumentService.java` - `splitIntoChunks()` method
```java
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
```

**What happens:**
- Splits the document text by whitespace into individual words
- Groups words into chunks of 500 words each (configurable)
- Creates overlapping is not implemented (could be enhanced)
- Each chunk becomes a separate searchable unit
- Returns a list of text chunks ready for embedding

**Why chunking is important:**
- OpenAI embedding models have token limits
- Smaller chunks provide more precise search results
- Allows for better context matching during retrieval

#### 4. Metadata Creation
**File**: `DocumentService.java` - `uploadDocument()` method
```java
Map<String, String> metadata = new HashMap<>();
metadata.put("fileName", file.getOriginalFilename());
metadata.put("documentId", documentId);
metadata.put("uploadTime", String.valueOf(System.currentTimeMillis()));
```

**What happens:**
- Creates metadata map to store document information
- Stores original filename for reference
- Stores document ID for grouping chunks
- Records upload timestamp for tracking
- Metadata is attached to each chunk for filtering/sorting

#### 5. Vector Embedding Generation
**File**: `VectorStoreService.java` - `addDocument()` method
```java
public void addDocument(String id, String content, Map<String, String> metadata) {
    log.info("Adding document with ID: {}", id);
    
    // Generate embedding for the document
    List<Double> embedding = generateEmbedding(content);
    
    // Store document with embedding
    vectorStore.put(id, new DocumentEmbedding(id, content, embedding, metadata));
    
    log.info("Document added successfully. Total documents: {}", vectorStore.size());
}
```

**What happens:**
- For each chunk, calls OpenAI API to generate embeddings
- Uses `text-embedding-ada-002` model (1536-dimensional vectors)
- Each chunk gets its own unique embedding vector
- Embeddings capture semantic meaning of the text

#### 6. Embedding API Call Details
**File**: `VectorStoreService.java` - `generateEmbedding()` method
```java
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
```

**What happens:**
- Creates embedding request with chunk text
- Calls OpenAI API synchronously
- Returns 1536-dimensional vector representing semantic meaning
- Handles errors and logs failures

#### 7. Vector Storage
**File**: `VectorStoreService.java` - In-memory storage
```java
private final Map<String, DocumentEmbedding> vectorStore = new ConcurrentHashMap<>();

vectorStore.put(id, new DocumentEmbedding(id, content, embedding, metadata));
```

**What happens:**
- Stores each chunk in a concurrent HashMap (thread-safe)
- Key: unique chunk ID (documentId + "_chunk_" + chunkNumber)
- Value: DocumentEmbedding object containing:
  - Chunk ID
  - Original text content
  - 1536-dimensional embedding vector
  - Metadata map

#### 8. Response Generation
**File**: `DocumentService.java` - Response building
```java
return DocumentUploadResponse.builder()
    .message("Document uploaded successfully")
    .documentId(documentId)
    .chunkCount(chunks.size())
    .build();
```

**What happens:**
- Creates success response with document ID
- Includes total number of chunks created
- Returns HTTP 200 with response body

---

## Search Endpoints

### GET /api/v1/rag/search
Simple query parameter-based search endpoint.

**Parameters:**
- `query` (required): The search question/query
- `topK` (optional, default=3): Number of similar documents to retrieve

**Example:**
```http
GET /api/v1/rag/search?query=What are wire transfer fees?&topK=5
```

### POST /api/v1/rag/search
More flexible JSON-based search endpoint.

**Request Body:**
```json
{
    "query": "What are the requirements for international wire transfers?",
    "topK": 3
}
```

### Detailed Search Process Flow

#### 1. Search Request Reception
**File**: `RagController.java` - `search()` methods
```java
@PostMapping("/search")
public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
    log.info("Received search request: {}", request.getQuery());
    SearchResponse response = ragService.search(request);
    return ResponseEntity.ok(response);
}
```

**What happens:**
- Receives search request with query string and topK parameter
- Logs the incoming query for monitoring
- Delegates to RagService for processing

#### 2. Query Embedding Generation
**File**: `RagService.java` - `search()` method
```java
List<VectorStoreService.DocumentEmbedding> similarDocuments = 
    vectorStoreService.similaritySearch(request.getQuery(), request.getTopK());
```

**File**: `VectorStoreService.java` - `similaritySearch()` method
```java
public List<DocumentEmbedding> similaritySearch(String query, int topK) {
    log.info("Performing similarity search for: {}", query);
    
    if (vectorStore.isEmpty()) {
        log.warn("Vector store is empty");
        return Collections.emptyList();
    }

    // Generate embedding for the query
    List<Double> queryEmbedding = generateEmbedding(query);
    
    // ... similarity calculation
}
```

**What happens:**
- Generates embedding for the user's query using same model as documents
- Query gets converted to 1536-dimensional vector
- Uses OpenAI's `text-embedding-ada-002` model for consistency

#### 3. Similarity Calculation
**File**: `VectorStoreService.java` - `similaritySearch()` method
```java
List<ScoredDocument> scoredDocs = vectorStore.values().stream()
    .map(doc -> new ScoredDocument(doc, cosineSimilarity(queryEmbedding, doc.getEmbedding())))
    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
    .limit(topK)
    .collect(Collectors.toList());
```

**File**: `VectorStoreService.java` - `cosineSimilarity()` method
```java
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
```

**What happens:**
- Calculates cosine similarity between query vector and all document vectors
- Cosine similarity formula: (A · B) / (||A|| × ||B||)
- Returns similarity score between 0 and 1 (higher = more similar)
- Sorts results by similarity score in descending order
- Limits results to topK most similar documents

#### 4. Context Preparation
**File**: `RagService.java` - `search()` method
```java
String context = similarDocuments.stream()
    .map(VectorStoreService.DocumentEmbedding::getContent)
    .collect(Collectors.joining("\n\n"));

String prompt = String.format(RAG_PROMPT_TEMPLATE, context, request.getQuery());
```

**What happens:**
- Extracts text content from the most similar document chunks
- Joins all retrieved chunks with double newlines for separation
- Creates a formatted prompt using the RAG template
- Combines retrieved context with user's original query

#### 5. RAG Prompt Template
**File**: `RagService.java` - Static template
```java
private static final String RAG_PROMPT_TEMPLATE = """
    Use the following context to answer the question. If you cannot answer the question based on the context, say so.
    
    Context:
    %s
    
    Question: %s
    
    Answer:
    """;
```

**What happens:**
- Provides clear instructions to the language model
- Includes retrieved context as reference material
- Includes the user's original question
- Instructs model to acknowledge when context is insufficient

#### 6. Answer Generation
**File**: `RagService.java` - `generateAnswer()` method
```java
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
```

**What happens:**
- Creates ChatCompletion request using GPT-3.5-turbo
- Sets temperature to 0.7 (moderate creativity)
- Limits response to 500 tokens maximum
- Handles API errors gracefully
- Returns generated answer or error message

#### 7. Response Assembly
**File**: `RagService.java` - `search()` method
```java
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
```

**What happens:**
- Creates truncated versions of source documents (200 chars max)
- Builds comprehensive response object containing:
  - Generated answer from GPT model
  - Relevant document snippets for transparency
  - Original user query for reference
- Returns structured JSON response

---

## API Reference

### Document Upload Endpoints

#### POST /api/v1/rag/documents/upload
Upload a document file.

**Content-Type:** `multipart/form-data`

**Parameters:**
- `file`: MultipartFile - The document to upload

**Response:**
```json
{
    "message": "Document uploaded successfully",
    "documentId": "uuid-string",
    "chunkCount": 15
}
```

#### POST /api/v1/rag/documents/upload-content
Upload document content directly.

**Content-Type:** `application/json`

**Request Body:**
```json
{
    "content": "Document text content here...",
    "fileName": "document.txt",
    "metadata": "Additional metadata"
}
```

**Response:**
```json
{
    "message": "Document content uploaded successfully",
    "documentId": "uuid-string",
    "chunkCount": 8
}
```

### Search Endpoints

#### GET /api/v1/rag/search
Simple search with query parameters.

**Parameters:**
- `query` (string, required): Search query
- `topK` (integer, optional, default=3): Number of results

**Response:**
```json
{
    "answer": "Generated answer based on retrieved context...",
    "relevantDocuments": [
        "Document snippet 1...",
        "Document snippet 2...",
        "Document snippet 3..."
    ],
    "query": "original user query"
}
```

#### POST /api/v1/rag/search
Advanced search with JSON body.

**Request Body:**
```json
{
    "query": "What are the wire transfer requirements?",
    "topK": 5
}
```

**Response:** Same as GET endpoint

### Health Check

#### GET /api/v1/rag/health
Check API status.

**Response:** `RAG API is running`

---

## Architecture Details

### Components Overview

1. **RagController**: REST API endpoints and request handling
2. **DocumentService**: Document processing and chunking logic
3. **VectorStoreService**: Vector embeddings and similarity search
4. **RagService**: RAG orchestration and answer generation

### Data Flow

```
User Query → Controller → RagService → VectorStore (similarity search) → OpenAI (answer generation) → Response
```

### Document Processing Flow

```
File Upload → Content Extraction → Chunking → Embedding Generation → Vector Storage
```

### Technology Stack

- **Spring Boot**: Web framework and dependency injection
- **OpenAI API**: Embeddings (`text-embedding-ada-002`) and Chat (`gpt-3.5-turbo`)
- **Lombok**: Code generation for boilerplate
- **SLF4J**: Logging framework
- **Swagger/OpenAPI**: API documentation

### Storage

- **In-Memory**: ConcurrentHashMap for vector storage
- **Thread-Safe**: Concurrent access supported
- **Volatile**: Data lost on restart (consider persistent storage for production)

### Security Considerations

- **API Keys**: OpenAI API key required in configuration
- **Rate Limits**: OpenAI API rate limiting applies
- **Input Validation**: File size limits recommended
- **Error Handling**: Graceful degradation on API failures

---

## Example Usage

### 1. Upload the Wire Transfer Instructions Document

```bash
curl -X POST "http://localhost:8080/api/v1/rag/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@wire-payment-instructions.txt"
```

**Response:**
```json
{
    "message": "Document uploaded successfully",
    "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkCount": 45
}
```

### 2. Search for Wire Transfer Information

```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the fees for international wire transfers?",
    "topK": 3
  }'
```

**Response:**
```json
{
    "answer": "Based on the provided context, the fees for international wire transfers are:\n\n- Outgoing International Wire (USD): $45.00 - $65.00\n- Outgoing International Wire (Foreign Exchange): $50.00 - $75.00\n- Incoming International Wire: $20.00\n- Foreign Exchange Markup: 0.5% - 2.5% of the transfer amount\n- Intermediary Bank Fees: $10.00 - $30.00 (if applicable)\n- Correspondent Bank Fees: Varies by bank\n- Amendment Fee: $50.00\n- Cancellation Fee: $65.00 (if not yet processed)\n\nAdditional fees may apply depending on the fee payment option selected (OUR/BEN/SHA) and the specific banks involved in the transfer.",
    "relevantDocuments": [
        "3.7 FEES FOR INTERNATIONAL WIRES\n\nOutgoing International Wire (USD):     $45.00 - $65.00\nOutgoing International Wire (FX):      $50.00 - $75.00\nIncoming International Wire:           $20.00\nForeign Exchange Markup...",
        "When sending international wires, you must specify who pays the fees:\n\nOUR (All charges paid by sender):\n   • Sender pays all fees (originating, intermediary, and receiving)...",
        "Factors affecting processing time:\n   • Destination country banking hours\n   • Intermediary bank processing\n   • Currency conversion requirements..."
    ],
    "query": "What are the fees for international wire transfers?"
}
```

### 3. Search Using GET Endpoint

```bash
curl "http://localhost:8080/api/v1/rag/search?query=How%20to%20prevent%20wire%20fraud&topK=2"
```

### 4. Upload Content Directly

```bash
curl -X POST "http://localhost:8080/api/v1/rag/documents/upload-content" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Additional banking policies and procedures...",
    "fileName": "banking-policies.txt",
    "metadata": "Internal policy document"
  }'
```

---

## Configuration

### Required Environment Variables

```properties
# application.properties
openai.api.key=${OPENAI_API_KEY}
server.port=8080
logging.level.com.example.rag=INFO
```

### OpenAI Configuration
**File**: `OpenAIConfig.java`
```java
@Configuration
public class OpenAIConfig {
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(apiKey);
    }
}
```

---

This documentation provides a comprehensive overview of the RAG API's document upload and search functionality, including detailed step-by-step processes, API references, and practical examples.
