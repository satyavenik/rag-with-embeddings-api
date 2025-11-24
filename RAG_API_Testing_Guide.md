# RAG API Testing Guide

## Quick Start Testing

### Prerequisites
1. Ensure the Spring Boot application is running on `http://localhost:8080`
2. OpenAI API key is configured in `application.properties`
3. Wire payment instructions document is available

### Test Sequence

## 1. Health Check
Verify the API is running.

```bash
curl -X GET "http://localhost:8080/api/v1/rag/health"
```

**Expected Response:**
```
RAG API is running
```

## 2. Upload the Wire Payment Instructions Document

### Option A: Upload the text file directly
```bash
curl -X POST "http://localhost:8080/api/v1/rag/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@wire-payment-instructions.txt"
```

### Option B: Upload content via JSON
```bash
curl -X POST "http://localhost:8080/api/v1/rag/documents/upload-content" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "WIRE PAYMENT INSTRUCTIONS\n\nDomestic wire transfers require:\n• Full beneficiary name\n• Bank account number\n• ABA routing number (9 digits)\n• Bank name and address\n\nFees:\n• Same-day wire: $35.00\n• Next-day wire: $25.00\n\nInternational wire transfers require:\n• Beneficiary bank SWIFT code\n• Full beneficiary information\n• Purpose of payment\n• Fee option (OUR/BEN/SHA)\n\nFees:\n• Outgoing international: $45-$65\n• Incoming international: $20\n• Foreign exchange markup: 0.5%-2.5%",
    "fileName": "wire-instructions-sample.txt",
    "metadata": "Banking instructions document"
  }'
```

**Expected Response:**
```json
{
    "message": "Document uploaded successfully",
    "documentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "chunkCount": 2
}
```

## 3. Test Search Functionality

### Test Case 1: Basic Wire Transfer Information
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What information is required for a domestic wire transfer?",
    "topK": 3
  }'
```

**Expected Response:**
```json
{
    "answer": "For a domestic wire transfer, you need to provide the following information:\n\n• Full beneficiary name (as it appears on the bank account)\n• Bank account number\n• ABA routing number (9 digits)\n• Bank name and address\n\nAdditionally, you'll need to provide your own information as the originator, including your account details and the purpose of the payment.",
    "relevantDocuments": [
        "Domestic wire transfers require: • Full beneficiary name • Bank account number • ABA routing number (9 digits) • Bank name and address",
        "ORIGINATOR INFORMATION: • Your Full Name or Company Name • Your Account Number • Your Contact Phone Number • Purpose of Payment (brief description) • Payment Amount (in USD)"
    ],
    "query": "What information is required for a domestic wire transfer?"
}
```

### Test Case 2: Fee Information
```bash
curl -X GET "http://localhost:8080/api/v1/rag/search?query=What%20are%20the%20wire%20transfer%20fees&topK=2"
```

### Test Case 3: International Wire Requirements
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is needed for international wire transfers?",
    "topK": 3
  }'
```

### Test Case 4: Security and Fraud Prevention
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How can I protect myself from wire transfer fraud?",
    "topK": 4
  }'
```

### Test Case 5: Processing Times
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How long do wire transfers take to process?",
    "topK": 2
  }'
```

### Test Case 6: Question Not in Document
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the cryptocurrency trading policies?",
    "topK": 3
  }'
```

**Expected Response:**
```json
{
    "answer": "I cannot answer that question based on the provided context. The documents contain information about wire transfer instructions and procedures, but do not include any information about cryptocurrency trading policies.",
    "relevantDocuments": [
        "Document snippets that were retrieved but don't contain cryptocurrency information..."
    ],
    "query": "What are the cryptocurrency trading policies?"
}
```

## 4. Advanced Testing Scenarios

### Test Empty Vector Store
Before uploading any documents, test search:

```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Any question",
    "topK": 3
  }'
```

**Expected Response:**
```json
{
    "answer": "No relevant documents found.",
    "relevantDocuments": [],
    "query": "Any question"
}
```

### Test Large topK Value
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "wire transfer information",
    "topK": 100
  }'
```

### Test Different Query Styles

#### Formal Question
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the specific requirements and documentation needed to initiate an international wire transfer to a European bank?",
    "topK": 3
  }'
```

#### Casual Question
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "how much does it cost to send money overseas",
    "topK": 3
  }'
```

#### Keywords Only
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SWIFT code international transfer fees",
    "topK": 3
  }'
```

## 5. Error Testing

### Test Invalid JSON
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "test",
    "topK": 3,
  }'
```

### Test Missing Required Fields
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "topK": 3
  }'
```

### Test Empty Query
```bash
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "",
    "topK": 3
  }'
```

### Test Invalid topK Values
```bash
# Negative value
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "test",
    "topK": -1
  }'

# Zero value
curl -X POST "http://localhost:8080/api/v1/rag/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "test",
    "topK": 0
  }'
```

## 6. Performance Testing

### Upload Large Document
Create a large text file and test upload:

```bash
# Create a large document (repeat wire instructions multiple times)
for i in {1..100}; do
  cat wire-payment-instructions.txt >> large-document.txt
done

# Upload the large document
curl -X POST "http://localhost:8080/api/v1/rag/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@large-document.txt"
```

### Concurrent Search Requests
```bash
# Run multiple searches simultaneously
for i in {1..10}; do
  curl -X POST "http://localhost:8080/api/v1/rag/search" \
    -H "Content-Type: application/json" \
    -d "{\"query\": \"test query $i\", \"topK\": 3}" &
done
wait
```

## 7. Postman Collection

Save this as `RAG_API_Tests.postman_collection.json`:

```json
{
    "info": {
        "name": "RAG API Tests",
        "description": "Comprehensive testing for RAG API endpoints"
    },
    "item": [
        {
            "name": "Health Check",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "{{baseUrl}}/api/v1/rag/health",
                    "host": ["{{baseUrl}}"],
                    "path": ["api", "v1", "rag", "health"]
                }
            }
        },
        {
            "name": "Upload Document Content",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"content\": \"Wire transfer instructions...\",\n    \"fileName\": \"wire-instructions.txt\",\n    \"metadata\": \"Banking document\"\n}"
                },
                "url": {
                    "raw": "{{baseUrl}}/api/v1/rag/documents/upload-content",
                    "host": ["{{baseUrl}}"],
                    "path": ["api", "v1", "rag", "documents", "upload-content"]
                }
            }
        },
        {
            "name": "Search Documents",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"query\": \"What are wire transfer fees?\",\n    \"topK\": 3\n}"
                },
                "url": {
                    "raw": "{{baseUrl}}/api/v1/rag/search",
                    "host": ["{{baseUrl}}"],
                    "path": ["api", "v1", "rag", "search"]
                }
            }
        },
        {
            "name": "Search Documents (GET)",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "{{baseUrl}}/api/v1/rag/search?query=wire transfer requirements&topK=2",
                    "host": ["{{baseUrl}}"],
                    "path": ["api", "v1", "rag", "search"],
                    "query": [
                        {
                            "key": "query",
                            "value": "wire transfer requirements"
                        },
                        {
                            "key": "topK",
                            "value": "2"
                        }
                    ]
                }
            }
        }
    ],
    "variable": [
        {
            "key": "baseUrl",
            "value": "http://localhost:8080"
        }
    ]
}
```

## 8. Python Test Script

Save as `test_rag_api.py`:

```python
import requests
import json
import time

BASE_URL = "http://localhost:8080/api/v1/rag"

def test_health():
    """Test health endpoint"""
    response = requests.get(f"{BASE_URL}/health")
    print(f"Health Check: {response.status_code} - {response.text}")
    return response.status_code == 200

def upload_document_content():
    """Upload sample document content"""
    payload = {
        "content": """
        WIRE PAYMENT INSTRUCTIONS
        
        Domestic wire transfers require:
        • Full beneficiary name
        • Bank account number  
        • ABA routing number (9 digits)
        • Bank name and address
        
        Fees:
        • Same-day wire: $35.00
        • Next-day wire: $25.00
        
        International wire transfers require:
        • Beneficiary bank SWIFT code
        • Full beneficiary information
        • Purpose of payment
        • Fee option (OUR/BEN/SHA)
        
        Fees:
        • Outgoing international: $45-$65
        • Incoming international: $20
        • Foreign exchange markup: 0.5%-2.5%
        """,
        "fileName": "wire-instructions-test.txt",
        "metadata": "Test banking document"
    }
    
    response = requests.post(
        f"{BASE_URL}/documents/upload-content",
        json=payload,
        headers={"Content-Type": "application/json"}
    )
    
    print(f"Document Upload: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print(f"Document ID: {result.get('documentId')}")
        print(f"Chunks: {result.get('chunkCount')}")
        return True
    return False

def test_search(query, top_k=3):
    """Test search functionality"""
    payload = {
        "query": query,
        "topK": top_k
    }
    
    response = requests.post(
        f"{BASE_URL}/search",
        json=payload,
        headers={"Content-Type": "application/json"}
    )
    
    print(f"\nSearch Query: '{query}'")
    print(f"Status: {response.status_code}")
    
    if response.status_code == 200:
        result = response.json()
        print(f"Answer: {result.get('answer')[:200]}...")
        print(f"Relevant Documents: {len(result.get('relevantDocuments', []))}")
        return True
    else:
        print(f"Error: {response.text}")
        return False

def main():
    """Run comprehensive tests"""
    print("=== RAG API Testing ===")
    
    # Test 1: Health check
    if not test_health():
        print("Health check failed!")
        return
    
    # Test 2: Upload document
    print("\n--- Uploading Document ---")
    if not upload_document_content():
        print("Document upload failed!")
        return
    
    # Wait a moment for processing
    time.sleep(2)
    
    # Test 3: Various search queries
    print("\n--- Testing Search Queries ---")
    test_queries = [
        "What are the fees for wire transfers?",
        "What information is needed for domestic wires?",
        "How much does an international wire cost?",
        "What is a SWIFT code?",
        "Cryptocurrency policies"  # Should not find relevant info
    ]
    
    for query in test_queries:
        test_search(query)
        time.sleep(1)
    
    print("\n=== Testing Complete ===")

if __name__ == "__main__":
    main()
```

Run the test:
```bash
python test_rag_api.py
```

## 9. Performance Benchmarks

### Expected Response Times
- Health check: < 50ms
- Document upload (small): < 2 seconds
- Document upload (large): < 10 seconds per 100KB
- Search query: < 3 seconds
- Search with large vector store: < 5 seconds

### Memory Usage
- Each document chunk: ~2KB in memory
- Each embedding vector: ~12KB (1536 doubles)
- 1000 document chunks: ~14MB memory usage

### Scalability Limits (In-Memory Store)
- Recommended max documents: 10,000 chunks
- Memory limit: ~140MB for 10K chunks
- Search performance degrades linearly with document count

## 10. Troubleshooting Common Issues

### Issue: "No relevant documents found"
**Cause:** Vector store is empty or no similar documents exist
**Solution:** 
1. Verify documents were uploaded successfully
2. Check if query is semantically similar to document content
3. Increase topK value

### Issue: "Error generating embedding"
**Cause:** OpenAI API issues
**Solutions:**
1. Check OpenAI API key configuration
2. Verify network connectivity
3. Check OpenAI service status
4. Review rate limits

### Issue: Slow search responses
**Cause:** Large vector store or network latency
**Solutions:**
1. Reduce topK value
2. Implement result caching
3. Consider using a dedicated vector database

### Issue: Poor answer quality
**Cause:** Irrelevant context or poor chunking
**Solutions:**
1. Improve document chunking strategy
2. Adjust similarity threshold
3. Refine RAG prompt template
4. Use higher topK for more context

This testing guide provides comprehensive coverage of the RAG API functionality and should help identify any issues during development and deployment.
