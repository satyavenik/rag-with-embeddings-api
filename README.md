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

## API Endpoints

### Upload Document (File)

**POST** `/api/v1/rag/documents/upload`

Upload a text file to the vector database.

**Request:**
- Content-Type: `multipart/form-data`
- Body: file (multipart file)

**Response:**
```json
{
  "message": "Document uploaded successfully",
  "documentId": "uuid",
  "chunkCount": 5
}
```

### Upload Document (Content)

**POST** `/api/v1/rag/documents/upload-content`

Upload document content directly as JSON.

**Request:**
```json
{
  "content": "Your document text here...",
  "fileName": "document.txt",
  "metadata": "optional metadata"
}
```

**Response:**
```json
{
  "message": "Document content uploaded successfully",
  "documentId": "uuid",
  "chunkCount": 5
}
```

### Search Documents (POST)

**POST** `/api/v1/rag/search`

Search documents and get AI-generated answers using RAG.

**Request:**
```json
{
  "query": "What is the main topic?",
  "topK": 3
}
```

**Response:**
```json
{
  "answer": "Based on the documents, the main topic is...",
  "relevantDocuments": [
    "Document chunk 1...",
    "Document chunk 2...",
    "Document chunk 3..."
  ],
  "query": "What is the main topic?"
}
```

### Search Documents (GET)

**GET** `/api/v1/rag/search?query=your+question&topK=3`

Alternative GET endpoint for searching.

### Health Check

**GET** `/api/v1/rag/health`

Check if the API is running.

**Response:**
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

### Using curl

```bash
# Upload document content
curl -X POST http://localhost:8080/api/v1/rag/documents/upload-content \
  -H "Content-Type: application/json" \
  -d '{
    "content": "The Spring Framework provides comprehensive infrastructure support for developing Java applications. Spring handles the infrastructure so you can focus on your application.",
    "fileName": "spring-intro.txt"
  }'

# Search and get answer
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What does Spring Framework provide?",
    "topK": 3
  }'
```

## Technology Stack

- **Spring Boot 3.2.0**: Application framework
- **OpenAI Java Client 0.18.2**: OpenAI API integration
- **SpringDoc OpenAPI**: API documentation
- **Lombok**: Boilerplate code reduction
- **Maven**: Build tool

## License

This project is open source and available under the MIT License.
