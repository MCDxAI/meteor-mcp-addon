# Ollama Java Integration Guide

**Last Updated:** 2026-02-07
**Sources:** Official Ollama documentation, Ollama4j library documentation, community research

## Table of Contents
1. [Overview](#overview)
2. [Google GenAI SDK Compatibility](#google-genai-sdk-compatibility)
3. [OpenAI Compatibility](#openai-compatibility)
4. [Java Libraries for Ollama](#java-libraries-for-ollama)
5. [Function Calling / Tool Use](#function-calling--tool-use)
6. [REST API Reference](#rest-api-reference)
7. [Implementation Examples](#implementation-examples)

---

## Overview

**Ollama** is a platform for running large language models locally. It provides:
- Local model execution (no cloud API required)
- OpenAI-compatible API endpoint
- Support for tool/function calling
- Streaming responses
- Vision/multimodal capabilities

**Default Endpoint:** `http://localhost:11434`
**OpenAI-Compatible Endpoint:** `http://localhost:11434/v1`

---

## Google GenAI SDK Compatibility

### Can Google GenAI Java SDK work with Ollama?

**YES, with custom endpoint configuration.**

The Google GenAI Java SDK supports custom endpoints through `HttpOptions`:

```java
import com.google.genai.HttpOptions;

HttpOptions httpOptions = HttpOptions.builder()
    .baseUrl("http://localhost:11434/v1")  // Point to Ollama
    .headers(ImmutableMap.of("key", "value"))
    .timeout(600)
    .build();
```

### Important Considerations

1. **API Compatibility**: Ollama's OpenAI-compatible endpoint may not support all Google GenAI SDK features
2. **Function Calling Format**: Google's function calling format differs from OpenAI's - you'll need to adapt
3. **Recommended Approach**: Use Ollama4j or OpenAI Java SDK instead for better compatibility

**Sources:**
- [Google GenAI Java SDK GitHub](https://github.com/googleapis/java-genai)
- [Building AI Agents with Google ADK and Ollama](https://medium.com/@debasish.sahu/building-ai-agents-with-google-adk-agent-development-kit-for-java-and-ollama-part-1-895c4e3620c6)

---

## OpenAI Compatibility

### OpenAI-Compatible Endpoint

Ollama provides built-in compatibility with the OpenAI Chat Completions API.

**Endpoint:** `http://localhost:11434/v1/chat/completions`
**API Key:** `"ollama"` (dummy key, required but unused)

### Supported Features

| Feature | Supported | Notes |
|---------|-----------|-------|
| Chat Completions | ✅ | Full support |
| Streaming | ✅ | Enabled by default |
| JSON Mode | ✅ | Structured outputs |
| Vision/Images | ✅ | Base64-encoded images |
| Tool Calling | ✅ | Function calling support |
| Embeddings | ✅ | `/v1/embeddings` endpoint |
| Image Generation | ⚠️ | Experimental, base64 only |
| Completions | ✅ | `/v1/completions` endpoint |
| Logprobs | ❌ | Not supported |
| Tool Choice | ❌ | Cannot specify which tool to use |
| Logit Bias | ❌ | Not supported |

### Differences from OpenAI

1. **Models must be pre-installed** via `ollama pull <model>`
2. **No conversation history** - stateless implementation only
3. **Missing advanced features**: logprobs, best_of, tool_choice
4. **Local execution only** - no cloud fallback

### Configuration Example

```java
// For OpenAI Java SDK pointing to Ollama
OpenAiApi api = new OpenAiApi("http://localhost:11434/v1", "ollama");

// For Spring AI with Ollama
OpenAiChatModel chatModel = new OpenAiChatModel(
    new OpenAiApi("http://localhost:11434", "ollama")
);
```

**Sources:**
- [Ollama OpenAI Compatibility Docs](https://docs.ollama.com/api/openai-compatibility)
- [Ollama OpenAI Compatibility Blog](https://ollama.com/blog/openai-compatibility)

---

## Java Libraries for Ollama

### 1. Ollama4j (Recommended)

**The primary community-maintained Java library for Ollama.**

#### Maven Dependency
```xml
<dependency>
    <groupId>io.github.ollama4j</groupId>
    <artifactId>ollama4j</artifactId>
    <version>1.1.4</version>
</dependency>
```

#### Gradle Dependency
```groovy
implementation 'io.github.ollama4j:ollama4j:1.1.4'
```

#### Requirements
- Java 17+
- Ollama 0.11.10+

#### Key Features

1. **Text Generation** - Standard chat and completion
2. **Multi-turn Chat** - Conversation history management
3. **Tool/Function Calling** - Annotation-based and manual registration
4. **MCP Tools Support** - Integration with Model Context Protocol
5. **Multimodal/Vision** - Image processing for vision models
6. **Embeddings** - Vector operations
7. **Model Management** - List, pull, create, delete models
8. **Authentication** - Basic auth and bearer tokens
9. **Streaming** - Real-time response streaming
10. **Async Generation** - Non-blocking operations
11. **Prometheus Metrics** - Request monitoring

#### Supported Tool Calling Models

- Mistral (7B and larger)
- Llama 3.1, 3.2, 3.3 (8B and larger)
- Qwen 2.5, 3.x
- FunctionGemma

#### Resources

- **GitHub:** https://github.com/ollama4j/ollama4j
- **Documentation:** https://ollama4j.github.io/ollama4j/
- **Examples:** https://github.com/ollama4j/ollama4j-examples
- **Javadoc:** https://ollama4j.github.io/ollama4j/apidocs/
- **Maven Central:** https://mvnrepository.com/artifact/io.github.ollama4j

**Sources:**
- [Ollama4j GitHub](https://github.com/ollama4j/ollama4j)
- [Ollama4j Documentation](https://ollama4j.github.io/ollama4j/)
- [Maven Central - Ollama4j](https://mvnrepository.com/artifact/io.github.ollama4j)

### 2. OpenAI Java SDK (with Ollama endpoint)

Any OpenAI-compatible Java SDK can work with Ollama by pointing to `http://localhost:11434/v1`.

**Pros:**
- Established ecosystem
- Familiar API
- Well-documented

**Cons:**
- Not optimized for Ollama-specific features
- May have compatibility issues with newer Ollama features

### 3. Spring AI Ollama Module

Spring AI provides native Ollama support:

```java
@Configuration
public class OllamaConfig {
    @Bean
    public OllamaChatModel ollamaChatModel() {
        return new OllamaChatModel(OllamaApi.create("http://localhost:11434"));
    }
}
```

**Source:** [Spring AI Ollama Chat Documentation](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)

### 4. Custom HTTP Client

For simple use cases, you can use standard Java HTTP clients (HttpClient, OkHttp, etc.) to call Ollama's REST API directly.

---

## Function Calling / Tool Use

### Overview

Ollama supports **tool calling** (also called function calling), enabling models to invoke tools and incorporate results into responses.

### Supported Models

Models that support tool calling (as of 2026):

| Model | Size | Performance | Notes |
|-------|------|-------------|-------|
| Llama 3.1 | 8B, 70B, 405B | Excellent | Best overall choice |
| Llama 3.2 | 1B, 3B | Good | Efficient for smaller tasks |
| Llama 3.3 | 70B | Excellent | Latest improvements |
| Mistral | 7B+ | Very Good | Lower resource requirements |
| Qwen 2.5 | 0.5B-72B | Good | Wide range of sizes |
| Qwen 3.x | Various | Very Good | Latest Qwen series |
| FunctionGemma | 270M | Specialized | Fine-tuned for function calling |

**Note:** Models are tagged with "Tools" category on https://ollama.com/search

**Sources:**
- [Ollama Tool Support Blog](https://ollama.com/blog/tool-support)
- [Ollama Tool Calling Docs](https://docs.ollama.com/capabilities/tool-calling)
- [Best Ollama Models for Function Calling 2025](https://collabnix.com/best-ollama-models-for-function-calling-tools-complete-guide-2025/)

### REST API Format for Tool Calling

#### Request Format

**Endpoint:** `POST /api/chat` or `POST /v1/chat/completions`

```json
{
  "model": "qwen3",
  "messages": [
    {
      "role": "user",
      "content": "What's the weather in New York?"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get the current weather for a location",
        "parameters": {
          "type": "object",
          "required": ["location"],
          "properties": {
            "location": {
              "type": "string",
              "description": "City name"
            }
          }
        }
      }
    }
  ]
}
```

#### Response Format

```json
{
  "model": "qwen3",
  "created_at": "2024-01-01T00:00:00Z",
  "message": {
    "role": "assistant",
    "content": "",
    "tool_calls": [
      {
        "type": "function",
        "function": {
          "index": 0,
          "name": "get_weather",
          "arguments": {
            "location": "New York"
          }
        }
      }
    ]
  },
  "done": true
}
```

#### Sending Tool Results Back

```json
{
  "model": "qwen3",
  "messages": [
    {
      "role": "user",
      "content": "What's the weather in New York?"
    },
    {
      "role": "assistant",
      "content": "",
      "tool_calls": [...]
    },
    {
      "role": "tool",
      "tool_name": "get_weather",
      "content": "{\"temperature\": 72, \"condition\": \"sunny\"}"
    }
  ]
}
```

### Tool Calling Patterns

1. **Single-shot**: One tool call → result → final response
2. **Parallel**: Multiple tool calls in one response → all results → final response
3. **Agent Loop**: Continuous iteration until model decides it's done
4. **Streaming**: Accumulate chunks before constructing complete messages

**Sources:**
- [Ollama Tool Calling Documentation](https://docs.ollama.com/capabilities/tool-calling)
- [Tool Calling and Function Execution Guide](https://deepwiki.com/ollama/ollama/7.2-tool-calling-and-function-execution)

---

## REST API Reference

### Base Endpoint

**Native Ollama API:** `http://localhost:11434/api`
**OpenAI-Compatible API:** `http://localhost:11434/v1`

### Key Endpoints

#### 1. Chat Completions

**Native:** `POST /api/chat`
**OpenAI:** `POST /v1/chat/completions`

```bash
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.1",
  "messages": [
    {
      "role": "user",
      "content": "Hello!"
    }
  ]
}'
```

#### 2. Generate (Completion)

**Native:** `POST /api/generate`
**OpenAI:** `POST /v1/completions`

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.1",
  "prompt": "Once upon a time"
}'
```

#### 3. Embeddings

**Native:** `POST /api/embeddings`
**OpenAI:** `POST /v1/embeddings`

```bash
curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text",
  "prompt": "Hello world"
}'
```

#### 4. Model Management

**List models:** `GET /api/tags`
**Pull model:** `POST /api/pull`
**Delete model:** `DELETE /api/delete`
**Create model:** `POST /api/create`

### Streaming

Streaming is **enabled by default** in the REST API.

To disable streaming:
```json
{
  "stream": false
}
```

Streaming responses are JSON objects separated by newlines (NDJSON format).

**Sources:**
- [Ollama API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Ollama Streaming Documentation](https://docs.ollama.com/capabilities/streaming)

---

## Implementation Examples

### Example 1: Ollama4j Basic Chat

```java
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;

public class OllamaChatExample {
    public static void main(String[] args) {
        OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434");

        OllamaChatRequest request = OllamaChatRequest.builder()
            .model("llama3.1")
            .message("user", "What is the capital of France?")
            .build();

        OllamaChatResult result = ollamaAPI.chat(request);
        System.out.println(result.getMessage().getContent());
    }
}
```

### Example 2: Ollama4j Tool Calling

```java
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.tools.ToolFunction;
import io.github.ollama4j.tools.ToolSpec;

// Define a tool
public class WeatherTool extends ToolFunction {
    public String getCurrentWeather(String city) {
        // Your implementation
        return String.format("Weather in %s: Sunny, 72°F", city);
    }
}

// Register and use
OllamaAPI api = new OllamaAPI("http://localhost:11434");

ToolSpec weatherSpec = ToolSpec.builder()
    .name("get_weather")
    .description("Get current weather for a city")
    .addProperty("city", "string", "City name", true)
    .build();

WeatherTool weatherTool = new WeatherTool();

// Register tool
api.registerTool(weatherSpec, weatherTool::getCurrentWeather);

// Use with chat
OllamaChatRequest request = OllamaChatRequest.builder()
    .model("mistral")
    .message("user", "What's the weather in Tokyo?")
    .withTools()  // Enable tool calling
    .build();

OllamaChatResult result = api.chat(request);
```

### Example 3: Annotation-Based Tool Registration

```java
import io.github.ollama4j.annotations.Tool;
import io.github.ollama4j.annotations.ToolParam;

public class MyTools {

    @Tool(
        name = "calculate_sum",
        description = "Calculate the sum of two numbers"
    )
    public int calculateSum(
        @ToolParam(name = "a", description = "First number", required = true) int a,
        @ToolParam(name = "b", description = "Second number", required = true) int b
    ) {
        return a + b;
    }
}

// Register all annotated tools
OllamaAPI api = new OllamaAPI("http://localhost:11434");
api.registerAnnotatedTools(new MyTools());
```

### Example 4: Direct REST API Call (Java HttpClient)

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class OllamaRestExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String json = """
            {
              "model": "llama3.1",
              "messages": [
                {"role": "user", "content": "Hello!"}
              ],
              "stream": false
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(response.body());
    }
}
```

### Example 5: OpenAI-Compatible Endpoint

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class OllamaOpenAIExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String json = """
            {
              "model": "llama3.1",
              "messages": [
                {"role": "user", "content": "Hello!"}
              ]
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ollama")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(response.body());
    }
}
```

---

## Key Takeaways

### For Google GenAI SDK Users

1. **Not recommended** - Use Ollama4j instead for better compatibility
2. **Custom endpoints supported** - But API differences will cause issues
3. **Function calling format differs** - Google's format != OpenAI's format

### For OpenAI SDK Users

1. **Works well** - Point base URL to `http://localhost:11434/v1`
2. **Use dummy API key** - Set to `"ollama"`
3. **Check feature support** - Not all OpenAI features are available

### For Ollama4j Users (Recommended)

1. **Best Java integration** - Purpose-built for Ollama
2. **Active development** - 474 stars, actively maintained
3. **Complete feature set** - Tools, vision, embeddings, streaming
4. **Easy tool calling** - Annotation-based or manual registration
5. **MCP support** - Integrates with Model Context Protocol

---

## Additional Resources

- [Ollama Official Documentation](https://docs.ollama.com/)
- [Ollama GitHub Repository](https://github.com/ollama/ollama)
- [Ollama4j GitHub](https://github.com/ollama4j/ollama4j)
- [Ollama4j Documentation](https://ollama4j.github.io/ollama4j/)
- [Ollama4j Examples](https://github.com/ollama4j/ollama4j-examples)
- [Ollama Models Library](https://ollama.com/library)
- [Ollama Blog](https://ollama.com/blog)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-07
**Maintained by:** Claude Code AI Documentation Researcher
