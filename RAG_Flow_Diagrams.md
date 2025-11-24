# RAG API Flow Diagrams

## Document Upload Process Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller as RagController
    participant DocService as DocumentService
    participant VectorStore as VectorStoreService
    participant OpenAI as OpenAI API

    User->>Controller: POST /documents/upload (MultipartFile)
    Controller->>DocService: uploadDocument(file)
    
    Note over DocService: Extract content from file
    DocService->>DocService: new String(file.getBytes())
    DocService->>DocService: Generate UUID documentId
    
    Note over DocService: Split into chunks
    DocService->>DocService: splitIntoChunks(content, 500)
    
    Note over DocService: Create metadata
    DocService->>DocService: metadata.put("fileName", filename)
    DocService->>DocService: metadata.put("documentId", documentId)
    DocService->>DocService: metadata.put("uploadTime", timestamp)
    
    loop For each chunk
        DocService->>VectorStore: addDocument(chunkId, chunk, metadata)
        VectorStore->>OpenAI: generateEmbedding(chunk)
        OpenAI-->>VectorStore: embedding vector (1536 dimensions)
        VectorStore->>VectorStore: vectorStore.put(chunkId, DocumentEmbedding)
    end
    
    VectorStore-->>DocService: Success
    DocService-->>Controller: DocumentUploadResponse
    Controller-->>User: HTTP 200 + Response JSON
```

## Search Process Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller as RagController
    participant RagService as RagService
    participant VectorStore as VectorStoreService
    participant OpenAI as OpenAI API

    User->>Controller: POST /search (SearchRequest)
    Controller->>RagService: search(request)
    
    Note over RagService: Start similarity search
    RagService->>VectorStore: similaritySearch(query, topK)
    
    Note over VectorStore: Generate query embedding
    VectorStore->>OpenAI: generateEmbedding(query)
    OpenAI-->>VectorStore: query embedding vector
    
    Note over VectorStore: Calculate similarities
    loop For each stored document
        VectorStore->>VectorStore: cosineSimilarity(queryVector, docVector)
    end
    
    VectorStore->>VectorStore: Sort by similarity score
    VectorStore->>VectorStore: Take top K results
    VectorStore-->>RagService: List<DocumentEmbedding>
    
    Note over RagService: Prepare context
    RagService->>RagService: Join document contents
    RagService->>RagService: Format RAG prompt template
    
    Note over RagService: Generate answer
    RagService->>OpenAI: createChatCompletion(prompt)
    OpenAI-->>RagService: Generated answer
    
    Note over RagService: Build response
    RagService->>RagService: Create SearchResponse
    RagService-->>Controller: SearchResponse
    Controller-->>User: HTTP 200 + Response JSON
```

## System Architecture Diagram

```mermaid
graph TB
    User[👤 User]
    
    subgraph "Spring Boot Application"
        Controller[📋 RagController]
        DocService[📄 DocumentService]
        RagService[🔍 RagService]
        VectorStore[🗃️ VectorStoreService]
        Config[⚙️ OpenAIConfig]
    end
    
    subgraph "External Services"
        OpenAI[🤖 OpenAI API]
        Embedding[📊 text-embedding-ada-002]
        ChatGPT[💬 gpt-3.5-turbo]
    end
    
    subgraph "Storage"
        Memory[(💾 In-Memory Vector Store)]
    end
    
    User -->|Upload Document| Controller
    User -->|Search Query| Controller
    
    Controller --> DocService
    Controller --> RagService
    
    DocService --> VectorStore
    RagService --> VectorStore
    
    VectorStore --> Memory
    VectorStore --> OpenAI
    
    OpenAI --> Embedding
    OpenAI --> ChatGPT
    
    Config -.->|API Key| OpenAI
```

## Data Structure Diagram

```mermaid
classDiagram
    class DocumentEmbedding {
        +String id
        +String content
        +List~Double~ embedding
        +Map~String,String~ metadata
    }
    
    class SearchRequest {
        +String query
        +int topK
    }
    
    class SearchResponse {
        +String answer
        +List~String~ relevantDocuments
        +String query
    }
    
    class DocumentUploadRequest {
        +String content
        +String fileName
        +String metadata
    }
    
    class DocumentUploadResponse {
        +String message
        +String documentId
        +int chunkCount
    }
    
    class ScoredDocument {
        +DocumentEmbedding document
        +double score
    }
    
    VectorStoreService --> DocumentEmbedding : stores
    VectorStoreService --> ScoredDocument : creates during search
    RagService --> SearchRequest : receives
    RagService --> SearchResponse : returns
    DocumentService --> DocumentUploadRequest : receives
    DocumentService --> DocumentUploadResponse : returns
```

## Vector Similarity Calculation

```mermaid
graph LR
    Query[🔍 User Query] --> QEmbed[📊 Query Embedding]
    
    subgraph "Vector Store"
        Doc1[📄 Doc Chunk 1] --> Emb1[📊 Embedding 1]
        Doc2[📄 Doc Chunk 2] --> Emb2[📊 Embedding 2]
        Doc3[📄 Doc Chunk 3] --> Emb3[📊 Embedding 3]
        DocN[📄 Doc Chunk N] --> EmbN[📊 Embedding N]
    end
    
    QEmbed --> Cosine1[🧮 Cosine Similarity 1]
    QEmbed --> Cosine2[🧮 Cosine Similarity 2]
    QEmbed --> Cosine3[🧮 Cosine Similarity 3]
    QEmbed --> CosineN[🧮 Cosine Similarity N]
    
    Emb1 --> Cosine1
    Emb2 --> Cosine2
    Emb3 --> Cosine3
    EmbN --> CosineN
    
    Cosine1 --> Sort[📊 Sort by Score]
    Cosine2 --> Sort
    Cosine3 --> Sort
    CosineN --> Sort
    
    Sort --> TopK[🎯 Top K Results]
```

## Error Handling Flow

```mermaid
graph TD
    Start([API Request]) --> Validate{Valid Input?}
    
    Validate -->|No| Error1[❌ Return 400 Bad Request]
    Validate -->|Yes| Process[📊 Process Request]
    
    Process --> OpenAICall{OpenAI API Call}
    
    OpenAICall -->|Success| Success[✅ Return Response]
    OpenAICall -->|API Error| Retry{Retry Logic?}
    OpenAICall -->|Auth Error| Error2[❌ Return 401 Unauthorized]
    OpenAICall -->|Rate Limit| Error3[❌ Return 429 Too Many Requests]
    
    Retry -->|Yes| OpenAICall
    Retry -->|No| Error4[❌ Return 500 Internal Server Error]
    
    Process --> Exception{Exception Caught?}
    Exception -->|Yes| Log[📝 Log Error]
    Exception -->|No| Success
    
    Log --> Error5[❌ Return Error Response]
    
    Error1 --> End([End])
    Error2 --> End
    Error3 --> End
    Error4 --> End
    Error5 --> End
    Success --> End
```

## Configuration Dependencies

```mermaid
graph TB
    subgraph "Environment Variables"
        ApiKey[🔑 OPENAI_API_KEY]
        Port[🌐 SERVER_PORT]
    end
    
    subgraph "Application Properties"
        Props[📄 application.properties]
    end
    
    subgraph "Spring Configuration"
        Config[⚙️ OpenAIConfig]
        Service[🔧 OpenAiService Bean]
    end
    
    ApiKey --> Props
    Port --> Props
    Props --> Config
    Config --> Service
    
    subgraph "Service Layer"
        VectorStore[🗃️ VectorStoreService]
        RagService[🔍 RagService]
    end
    
    Service --> VectorStore
    Service --> RagService
```
