package com.cope.meteormcp.ollama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.utils.Utils;
import org.junit.jupiter.api.Test;

class OllamaCompatibilityTest {
    @Test
    void ignoresUnknownToolCallFieldsInChatResponse() {
        OllamaClientManager.ensureMapperCompatibility();

        String responseJson = """
            {
              "model": "llama3.1",
              "created_at": "2026-02-10T23:13:54Z",
              "done": true,
              "message": {
                "role": "assistant",
                "response": "",
                "tool_calls": [
                  {
                    "id": "call_123",
                    "function": {
                      "name": "weather_get",
                      "arguments": {
                        "city": "New York"
                      }
                    }
                  }
                ]
              }
            }
            """;

        OllamaChatResponseModel parsed = assertDoesNotThrow(
            () -> Utils.getObjectMapper().readValue(responseJson, OllamaChatResponseModel.class)
        );

        assertNotNull(parsed.getMessage());
        assertNotNull(parsed.getMessage().getToolCalls());
        assertEquals(1, parsed.getMessage().getToolCalls().size());
        assertEquals("weather_get", parsed.getMessage().getToolCalls().get(0).getFunction().getName());
    }
}
