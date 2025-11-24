package com.example.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "openai.api-key=test-key"
})
class RagApplicationTests {

    @Test
    void contextLoads() {
    }
}
