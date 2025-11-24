# rag-with-embeddings-api

Spring Boot RAG (Retrieval Augmented Generation) API using embeddings for document search and question answering.

## Features

- Document upload and vectorization
- Semantic search using embeddings
- RAG-based question answering using ChatGPT
- Swagger/OpenAPI documentation
- RESTful API endpoints

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- OpenAI API Key

## Configuration

Set your OpenAI API key as an environment variable:

```bash
export OPENAI_API_KEY=your-api-key-here
```

Or configure it in `src/main/resources/application.properties`:

```properties
openai.api-key=your-api-key-here
```

## Building the Application

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/rag-with-embeddings-api-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Documentation

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

## Data Models

### DocumentUploadRequest
```java
{
  "content": "string",    // Required: The document text content to be embedded
  "fileName": "string",   // Optional: Name or identifier for the document
  "metadata": "string"    // Optional: Additional metadata as JSON string
}
```

**Fields:**
- `content` (String, required): The text content to be embedded and stored
- `fileName` (String, optional): Document name or identifier
- `metadata` (String, optional): JSON-formatted metadata for categorization

### DocumentUploadResponse
```java
{
  "message": "string",     // Status message
  "documentId": "string",  // Unique identifier for the uploaded document
  "chunkCount": int        // Number of chunks created from the document
}
```

**Fields:**
- `message` (String): Success or error message
- `documentId` (String): UUID assigned to the document
- `chunkCount` (int): Number of text chunks created for embedding

### SearchRequest
```java
{
  "query": "string",  // Required: Search query
  "topK": int        // Optional: Number of results (default: 3)
}
```

**Fields:**
- `query` (String, required): Natural language search query
- `topK` (int, optional, default: 3): Maximum number of relevant documents to retrieve

### SearchResponse
```java
{
  "answer": "string",              // AI-generated answer
  "relevantDocuments": ["string"], // Array of relevant document chunks
  "query": "string"                // Original query
}
```

**Fields:**
- `answer` (String): ChatGPT-generated answer based on relevant documents
- `relevantDocuments` (List<String>): List of the most relevant document chunks
- `query` (String): The original search query

## API Endpoints

### Upload Document (File)

**POST** `/api/v1/rag/documents/upload`

Upload a text file to the vector database.

**Request:**
- Content-Type: `multipart/form-data`
- Body: file (multipart file)

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/rag/documents/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/document.txt"
```

**Success Response (200 OK):**
```json
{
  "message": "Document uploaded successfully",
  "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "chunkCount": 5
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "File is empty or invalid",
  "documentId": null,
  "chunkCount": 0
}
```

---

### Upload Document (Content)

**POST** `/api/v1/rag/documents/upload-content`

Upload document content directly as JSON.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body (DocumentUploadRequest):**
```json
{
  "content": "Your document text here...",
  "fileName": "document.txt",
  "metadata": "{\"author\":\"John Doe\",\"category\":\"Technology\"}"
}
```

**cURL Example - Small Document:**
```bash
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring Boot is a Java framework for building production-ready applications. It provides comprehensive infrastructure support.",
    "fileName": "spring-boot-intro.txt",
    "metadata": "{\"category\":\"Programming\",\"language\":\"Java\",\"difficulty\":\"beginner\"}"
  }'
```

**cURL Example - Large Document for Testing Embeddings:**
```bash
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Artificial Intelligence and Machine Learning have revolutionized the technology landscape in unprecedented ways. Deep learning, a subset of machine learning, uses neural networks with multiple layers to progressively extract higher-level features from raw input. These neural networks are inspired by the biological neural networks that constitute animal brains. The fundamental unit of computation in a neural network is the neuron, often called a node or unit. It receives input from other nodes, or from an external source, and computes an output. Each input has an associated weight which is assigned based on its relative importance to other inputs. The node applies a function to the weighted sum of its inputs. Convolutional Neural Networks (CNNs) have become the go-to architecture for image recognition tasks. They work by applying filters across the input image to detect features like edges, textures, and patterns. Recurrent Neural Networks (RNNs) are designed to work with sequence data, making them ideal for natural language processing tasks. Long Short-Term Memory (LSTM) networks are a special kind of RNN capable of learning long-term dependencies. Transformers have emerged as a powerful architecture for NLP tasks, using self-attention mechanisms to process input sequences. The attention mechanism allows the model to focus on different parts of the input when producing each part of the output. BERT, GPT, and T5 are examples of transformer-based models that have achieved state-of-the-art results on various benchmarks. Transfer learning has become a crucial technique in deep learning, where models pre-trained on large datasets are fine-tuned for specific tasks. This approach significantly reduces training time and improves performance, especially when labeled data is scarce. Reinforcement learning is another important paradigm where agents learn to make decisions by interacting with an environment.",
    "fileName": "ai-ml-comprehensive.txt",
    "metadata": "{\"author\":\"AI Research Team\",\"category\":\"Technology\",\"tags\":[\"AI\",\"ML\",\"Deep Learning\",\"Neural Networks\"],\"date\":\"2024-01-15\"}"
  }'
```

**Success Response (200 OK):**
```json
{
  "message": "Document content uploaded successfully",
  "documentId": "b2c3d4e5-f6g7-8901-bcde-fg2345678901",
  "chunkCount": 8
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Content cannot be empty",
  "documentId": null,
  "chunkCount": 0
}
```

**Error Response (500 Internal Server Error):**
```json
{
  "message": "Failed to generate embeddings",
  "documentId": null,
  "chunkCount": 0
}
```

---

### Search Documents (POST)

**POST** `/api/v1/rag/search`

Search documents and get AI-generated answers using RAG.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body (SearchRequest):**
```json
{
  "query": "What is the main topic?",
  "topK": 3
}
```

**Field Details:**
- `query`: Your natural language question
- `topK`: Number of relevant documents to retrieve (default: 3, max recommended: 10)

**cURL Examples:**
```bash
# Basic search
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are neural networks?",
    "topK": 3
  }'

# Search with more context
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Explain the difference between CNNs and RNNs",
    "topK": 5
  }'

# Simple question
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How does Spring Boot help developers?"
  }'
```

**Success Response (200 OK):**
```json
{
  "answer": "Based on the documents, neural networks are computational models inspired by biological neural networks in animal brains. The fundamental unit is the neuron (or node), which receives inputs from other nodes or external sources, applies weights to these inputs based on their importance, and computes an output using a function on the weighted sum.",
  "relevantDocuments": [
    "The fundamental unit of computation in a neural network is the neuron, often called a node or unit. It receives input from other nodes, or from an external source, and computes an output.",
    "These neural networks are inspired by the biological neural networks that constitute animal brains.",
    "Deep learning, a subset of machine learning, uses neural networks with multiple layers to progressively extract higher-level features from raw input."
  ],
  "query": "What are neural networks?"
}
```

**Error Response (400 Bad Request):**
```json
{
  "answer": null,
  "relevantDocuments": [],
  "query": ""
}
```

**Error Response (404 Not Found):**
```json
{
  "answer": "No relevant documents found for your query.",
  "relevantDocuments": [],
  "query": "non-existent topic"
}
```

---

### Search Documents (GET)

**GET** `/api/v1/rag/search?query=your+question&topK=3`

Alternative GET endpoint for searching documents.

**Query Parameters:**
- `query` (required): URL-encoded search query
- `topK` (optional, default: 3): Number of results

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/rag/search?query=What%20is%20deep%20learning&topK=3"
```

**Response:** Same as POST `/api/v1/rag/search`

---

### Health Check

**GET** `/api/v1/rag/health`

Check if the API is running.

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/v1/rag/health
```

**Response (200 OK):**
```
RAG API is running
```

## How It Works

1. **Document Upload**: Documents are split into chunks and converted to embeddings using OpenAI's `text-embedding-ada-002` model.

2. **Vector Storage**: Embeddings are stored in an in-memory vector store with their associated text content.

3. **Semantic Search**: When a query is received, it's converted to an embedding and compared with stored embeddings using cosine similarity.

4. **RAG Response**: The most relevant document chunks are retrieved and sent to ChatGPT along with the user's query to generate a contextual answer.

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  RagController  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐  ┌───────┐
│Document│  │  Rag  │
│Service │  │Service│
└───┬────┘  └───┬───┘
    │           │
    └────┬──────┘
         ▼
┌─────────────────┐
│ VectorStore     │
│ Service         │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ OpenAI Service  │
│ - Embeddings    │
│ - ChatGPT       │
└─────────────────┘
```

## Example Usage

### Complete Workflow Example

#### Step 1: Upload Multiple Documents

```bash
# Upload document about Spring Framework
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "The Spring Framework provides comprehensive infrastructure support for developing Java applications. Spring handles the infrastructure so you can focus on your application. Spring Boot makes it easy to create stand-alone, production-grade Spring based applications. It takes an opinionated view of the Spring platform and third-party libraries, allowing you to get started with minimum fuss.",
    "fileName": "spring-intro.txt",
    "metadata": "{\"category\":\"Java\",\"framework\":\"Spring\",\"difficulty\":\"beginner\"}"
  }'

# Upload document about AI/ML (Large content for testing embeddings)
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Artificial Intelligence and Machine Learning have revolutionized the technology landscape. Deep learning uses neural networks with multiple layers to extract features from raw input. Convolutional Neural Networks (CNNs) excel at image recognition by applying filters to detect edges, textures, and patterns. Recurrent Neural Networks (RNNs) handle sequence data, making them ideal for natural language processing. LSTM networks can learn long-term dependencies. Transformers use self-attention mechanisms for NLP tasks. Transfer learning allows pre-trained models to be fine-tuned for specific tasks, reducing training time significantly.",
    "fileName": "ai-ml-overview.txt",
    "metadata": "{\"category\":\"AI\",\"topics\":[\"Deep Learning\",\"CNNs\",\"RNNs\",\"Transformers\"],\"level\":\"intermediate\"}"
  }'

# Upload document about microservices
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Microservices architecture is an approach to building applications as a collection of small, independent services. Each service runs in its own process and communicates through well-defined APIs. This architecture enables teams to work independently, deploy services separately, and scale components individually. Spring Cloud provides tools for building microservices including service discovery, configuration management, circuit breakers, and API gateways.",
    "fileName": "microservices-intro.txt",
    "metadata": "{\"category\":\"Architecture\",\"pattern\":\"Microservices\",\"framework\":\"Spring Cloud\"}"
  }'
```

#### Step 2: Search and Query Documents

```bash
# Question about Spring
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What does Spring Boot provide for developers?",
    "topK": 3
  }'

# Question about AI/ML
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the different types of neural networks and their use cases?",
    "topK": 5
  }'

# Question about architecture
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the benefits of microservices architecture?",
    "topK": 3
  }'

# Using GET endpoint
curl -X GET "http://localhost:8080/api/v1/rag/search?query=How%20does%20transfer%20learning%20work&topK=3"
```

### Testing with Large Documents

For comprehensive embedding testing, use this large document:

```bash
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Artificial Intelligence and Machine Learning have revolutionized the technology landscape in unprecedented ways. Deep learning, a subset of machine learning, uses neural networks with multiple layers to progressively extract higher-level features from raw input. These neural networks are inspired by the biological neural networks that constitute animal brains. The fundamental unit of computation in a neural network is the neuron, often called a node or unit. It receives input from other nodes, or from an external source, and computes an output. Each input has an associated weight which is assigned based on its relative importance to other inputs. The node applies a function to the weighted sum of its inputs. Convolutional Neural Networks (CNNs) have become the go-to architecture for image recognition tasks. They work by applying filters across the input image to detect features like edges, textures, and patterns. Recurrent Neural Networks (RNNs) are designed to work with sequence data, making them ideal for natural language processing tasks. Long Short-Term Memory (LSTM) networks are a special kind of RNN capable of learning long-term dependencies. Transformers have emerged as a powerful architecture for NLP tasks, using self-attention mechanisms to process input sequences. The attention mechanism allows the model to focus on different parts of the input when producing each part of the output. BERT, GPT, and T5 are examples of transformer-based models that have achieved state-of-the-art results on various benchmarks. Transfer learning has become a crucial technique in deep learning, where models pre-trained on large datasets are fine-tuned for specific tasks. This approach significantly reduces training time and improves performance, especially when labeled data is scarce. Reinforcement learning is another important paradigm where agents learn to make decisions by interacting with an environment. The agent receives rewards or penalties based on its actions and learns to maximize cumulative reward over time. Deep reinforcement learning combines deep learning with reinforcement learning, enabling agents to learn directly from high-dimensional sensory inputs. Applications of AI span across healthcare, finance, autonomous vehicles, robotics, and many other domains. In healthcare, AI models assist in disease diagnosis, drug discovery, and personalized treatment plans. Computer vision enables machines to interpret and understand visual information from the world. Natural language processing allows computers to understand, interpret, and generate human language. Generative AI models can create new content, including images, text, music, and even code. Ethical considerations in AI include bias, fairness, transparency, privacy, and accountability.",
    "fileName": "ai-comprehensive-guide.txt",
    "metadata": "{\"author\":\"AI Research Team\",\"category\":\"Technology\",\"tags\":[\"AI\",\"ML\",\"Deep Learning\",\"Neural Networks\",\"NLP\",\"Computer Vision\"],\"date\":\"2024-11-23\",\"wordCount\":2500}"
  }'
```

### PowerShell Examples (Windows)

```powershell
# Upload document
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/rag/documents/upload-content" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"content":"Spring Boot simplifies Java development","fileName":"spring.txt"}'

# Search documents
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/rag/search" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"query":"What is Spring Boot?","topK":3}'
```

### Using Postman

1. **Upload Document Content**:
   - Method: POST
   - URL: `http://localhost:8080/api/v1/rag/documents/upload-content`
   - Headers: `Content-Type: application/json`
   - Body (raw JSON):
   ```json
   {
     "content": "Your document text here",
     "fileName": "test-document.txt",
     "metadata": "{\"author\":\"Test User\"}"
   }
   ```

2. **Search Documents**:
   - Method: POST
   - URL: `http://localhost:8080/api/v1/rag/search`
   - Headers: `Content-Type: application/json`
   - Body (raw JSON):
   ```json
   {
     "query": "Your question here",
     "topK": 3
   }
   ```

## Technology Stack

- **Spring Boot 3.2.0**: Application framework
- **OpenAI Java Client 0.18.2**: OpenAI API integration
- **SpringDoc OpenAPI**: API documentation
- **Lombok**: Boilerplate code reduction
- **Maven**: Build tool

## Best Practices

### Document Upload

1. **Content Length**: 
   - Optimal chunk size: 500-2000 characters
   - Documents are automatically chunked by the service
   - Larger documents provide better context for embeddings

2. **Metadata Usage**:
   - Use JSON format for structured metadata
   - Include relevant fields: author, category, tags, date
   - Metadata helps with document organization and filtering
   - Example: `{"author":"John","category":"Tech","tags":["AI","ML"]}`

3. **File Naming**:
   - Use descriptive filenames
   - Include extensions for clarity (.txt, .md, etc.)
   - Filenames help identify documents in search results

### Search Optimization

1. **TopK Selection**:
   - Default: 3 documents (good for most queries)
   - Use 5-10 for complex questions requiring more context
   - Higher values increase API response time

2. **Query Formulation**:
   - Use natural language questions
   - Be specific and clear
   - Include context when needed
   - Example: "What are the benefits of microservices?" vs "microservices"

3. **Performance**:
   - First search may be slower (model initialization)
   - Subsequent searches are faster (cached embeddings)
   - Consider implementing caching for frequently asked questions

## Error Handling

### Common Errors and Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| 400 Bad Request | Empty content or invalid JSON | Check request body format |
| 401 Unauthorized | Invalid OpenAI API key | Verify API key configuration |
| 429 Too Many Requests | OpenAI rate limit exceeded | Implement retry with backoff |
| 500 Internal Server Error | OpenAI service error | Check OpenAI service status |
| 503 Service Unavailable | Application not ready | Wait for application startup |

### Response Status Codes

- **200 OK**: Request successful
- **400 Bad Request**: Invalid request parameters
- **401 Unauthorized**: Authentication failed
- **404 Not Found**: Resource not found
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server error
- **503 Service Unavailable**: Service temporarily unavailable

## Troubleshooting

### Application Won't Start

```bash
# Check Java version
java -version

# Verify Maven installation
mvn -version

# Clean and rebuild
mvn clean install

# Check application.properties
cat src/main/resources/application.properties
```

### OpenAI API Issues

```bash
# Verify API key is set
echo %OPENAI_API_KEY%  # Windows
echo $OPENAI_API_KEY   # Linux/Mac

# Test API key manually
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer YOUR_API_KEY"
```

### Empty Search Results

- Ensure documents are uploaded successfully
- Check if query is related to uploaded content
- Verify embeddings were generated (check logs)
- Try increasing `topK` parameter

## Performance Considerations

### Embedding Generation

- Each document upload requires API call to OpenAI
- Large documents may take 2-5 seconds to process
- Consider batch processing for multiple documents
- Embeddings are generated once and stored in memory

### Search Performance

- Vector similarity search is fast (milliseconds)
- ChatGPT response generation takes 1-3 seconds
- Total search time: 1-4 seconds depending on complexity
- In-memory storage provides fast retrieval

### Scaling

- Current implementation uses in-memory storage
- For production, consider:
  - Vector databases (Pinecone, Weaviate, Milvus)
  - Persistent storage (PostgreSQL with pgvector)
  - Caching layer (Redis)
  - Load balancing for high traffic

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package -DskipTests
java -jar target/rag-with-embeddings-api-0.0.1-SNAPSHOT.jar
```

### Environment Variables

```bash
# Windows (cmd)
set OPENAI_API_KEY=your-key-here

# Windows (PowerShell)
$env:OPENAI_API_KEY="your-key-here"

# Linux/Mac
export OPENAI_API_KEY=your-key-here
```

## API Limits

### OpenAI Rate Limits

- **text-embedding-ada-002**: 3,000 requests/minute
- **gpt-3.5-turbo**: 3,500 requests/minute (varies by tier)
- Monitor usage in OpenAI dashboard
- Implement exponential backoff for rate limit errors

### Application Limits

- Maximum request body size: 10MB (Spring Boot default)
- Maximum document chunks: No hard limit (memory dependent)
- Concurrent requests: Limited by Spring Boot thread pool

## Future Enhancements

- [ ] Persistent vector storage
- [ ] Document deletion endpoint
- [ ] Metadata filtering in search
- [ ] Batch document upload
- [ ] User authentication and authorization
- [ ] Document versioning
- [ ] Advanced chunking strategies
- [ ] Multi-language support
- [ ] Streaming responses
- [ ] Usage analytics and monitoring

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is open source and available under the MIT License.

## Support

For questions, issues, or feature requests:
- Open an issue on GitHub
- Check existing documentation
- Review OpenAI API documentation

## Acknowledgments

- OpenAI for providing the embeddings and chat APIs
- Spring Boot community for excellent documentation
- Contributors and users of this project

