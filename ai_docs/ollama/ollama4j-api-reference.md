# Ollama4j API Reference

**Library Version:** 1.1.4 (Latest as of 2026-02-07)
**Java Version Required:** 17+
**Ollama Version Required:** 0.11.10+

## Table of Contents
1. [Installation](#installation)
2. [Initialization](#initialization)
3. [Chat API](#chat-api)
4. [Generation API](#generation-api)
5. [Tool Calling](#tool-calling)
6. [Embeddings](#embeddings)
7. [Model Management](#model-management)
8. [Streaming](#streaming)
9. [Authentication](#authentication)
10. [Configuration](#configuration)

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.ollama4j</groupId>
    <artifactId>ollama4j</artifactId>
    <version>1.1.4</version>
</dependency>
```

### Gradle (Groovy)

```groovy
implementation 'io.github.ollama4j:ollama4j:1.1.4'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.github.ollama4j:ollama4j:1.1.4")
```

**Maven Central Repository:** https://mvnrepository.com/artifact/io.github.ollama4j/ollama4j

---

## Initialization

### Basic Initialization

```java
import io.github.ollama4j.Ollama;

// Default localhost:11434
Ollama api = new Ollama();

// Custom host
Ollama api = new Ollama("http://192.168.1.100:11434");
```

### With Authentication

```java
import io.github.ollama4j.Ollama;

Ollama api = new Ollama("http://localhost:11434");

// Basic auth
api.setBasicAuth("username", "password");

// Bearer token
api.setBearerToken("your-token-here");
```

---

## Chat API

### Basic Chat

```java
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;

OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("user", "What is the capital of France?")
    .build();

OllamaChatResult result = api.chat(request);
String response = result.getMessage().getContent();
```

### Multi-Turn Conversation

```java
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;

OllamaChatRequestBuilder builder = OllamaChatRequest.builder()
    .model("llama3.1");

// First message
builder.message("user", "Hello!");
OllamaChatResult result1 = api.chat(builder.build());

// Second message (preserves history)
builder.message("assistant", result1.getMessage().getContent());
builder.message("user", "Tell me a joke");
OllamaChatResult result2 = api.chat(builder.build());
```

### Chat with Images (Vision Models)

```java
import java.util.List;

OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llava")
    .message("user", "What's in this image?")
    .images(List.of("path/to/image.jpg"))  // Can be file paths or base64
    .build();

OllamaChatResult result = api.chat(request);
```

### Chat with System Prompt

```java
OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("system", "You are a helpful programming assistant.")
    .message("user", "How do I reverse a string in Java?")
    .build();
```

---

## Generation API

### Basic Generation (Completion)

```java
import io.github.ollama4j.models.generate.OllamaGenerateRequest;
import io.github.ollama4j.models.generate.OllamaGenerateResult;

OllamaGenerateRequest request = OllamaGenerateRequest.builder()
    .model("llama3.1")
    .prompt("Once upon a time")
    .build();

OllamaGenerateResult result = api.generate(request);
String completion = result.getResponse();
```

### Generation with Options

```java
import io.github.ollama4j.models.generate.OllamaGenerateOptions;

OllamaGenerateOptions options = OllamaGenerateOptions.builder()
    .temperature(0.8)
    .topP(0.9)
    .topK(40)
    .numPredict(100)  // Max tokens
    .stop(List.of("\n", "###"))
    .build();

OllamaGenerateRequest request = OllamaGenerateRequest.builder()
    .model("llama3.1")
    .prompt("Write a haiku about programming:")
    .options(options)
    .build();
```

### Async Generation

```java
import java.util.concurrent.CompletableFuture;

CompletableFuture<OllamaGenerateResult> future = api.generateAsync(request);

future.thenAccept(result -> {
    System.out.println(result.getResponse());
});
```

---

## Tool Calling

### Manual Tool Registration

```java
import io.github.ollama4j.tools.ToolSpec;
import io.github.ollama4j.tools.ToolFunction;

// Define tool spec
ToolSpec weatherSpec = ToolSpec.builder()
    .name("get_weather")
    .description("Get current weather for a city")
    .addProperty("city", "string", "City name", true)  // true = required
    .build();

// Define tool function
ToolFunction weatherFunc = (args) -> {
    String city = args.get("city").toString();
    // Your weather API call here
    return String.format("Weather in %s: Sunny, 72°F", city);
};

// Register tool
api.registerTool(weatherSpec, weatherFunc);
```

### Annotation-Based Tools

```java
import io.github.ollama4j.annotations.Tool;
import io.github.ollama4j.annotations.ToolParam;

public class CalculatorTools {

    @Tool(
        name = "add",
        description = "Add two numbers"
    )
    public int add(
        @ToolParam(name = "a", description = "First number", required = true) int a,
        @ToolParam(name = "b", description = "Second number", required = true) int b
    ) {
        return a + b;
    }

    @Tool(
        name = "multiply",
        description = "Multiply two numbers"
    )
    public int multiply(
        @ToolParam(name = "a", description = "First number", required = true) int a,
        @ToolParam(name = "b", description = "Second number", required = true) int b
    ) {
        return a * b;
    }
}

// Register all annotated tools
api.registerAnnotatedTools(new CalculatorTools());
```

### Using Tools in Chat

```java
OllamaChatRequest request = OllamaChatRequest.builder()
    .model("mistral")  // Must be a tool-capable model
    .message("user", "What's the weather in Tokyo?")
    .withTools()  // Enable tool calling
    .build();

OllamaChatResult result = api.chat(request);

// Check if tools were called
if (result.getMessage().hasToolCalls()) {
    List<ToolCall> toolCalls = result.getMessage().getToolCalls();
    // Process tool calls
}
```

### Tool Calling Models

**Supported models for tool calling:**
- `mistral` (7B+)
- `llama3.1` (8B, 70B, 405B)
- `llama3.2` (1B, 3B)
- `llama3.3` (70B)
- `qwen2.5` (all sizes)
- `qwen3` (all sizes)
- `functiongemma` (270M - specialized)

---

## Embeddings

### Generate Embeddings

```java
import io.github.ollama4j.models.embeddings.OllamaEmbeddingsRequest;
import io.github.ollama4j.models.embeddings.OllamaEmbeddingsResult;

OllamaEmbeddingsRequest request = OllamaEmbeddingsRequest.builder()
    .model("nomic-embed-text")
    .prompt("Hello world")
    .build();

OllamaEmbeddingsResult result = api.embeddings(request);
double[] embedding = result.getEmbedding();
```

### Batch Embeddings

```java
import java.util.List;

List<String> texts = List.of(
    "First document",
    "Second document",
    "Third document"
);

List<double[]> embeddings = texts.stream()
    .map(text -> {
        OllamaEmbeddingsRequest req = OllamaEmbeddingsRequest.builder()
            .model("nomic-embed-text")
            .prompt(text)
            .build();
        return api.embeddings(req).getEmbedding();
    })
    .toList();
```

---

## Model Management

### List Models

```java
import io.github.ollama4j.models.Model;
import java.util.List;

List<Model> models = api.listModels();

for (Model model : models) {
    System.out.println(model.getName());
    System.out.println(model.getSize());
    System.out.println(model.getModifiedAt());
}
```

### Pull Model

```java
// Pull a model from Ollama library
api.pullModel("llama3.1");

// Pull with progress callback
api.pullModel("llama3.1", (status) -> {
    System.out.println(status.getStatus());
    System.out.println(status.getCompleted() + "/" + status.getTotal());
});
```

### Delete Model

```java
api.deleteModel("llama3.1");
```

### Create Custom Model

```java
import io.github.ollama4j.models.create.CreateModelRequest;

String modelfile = """
    FROM llama3.1
    SYSTEM You are a helpful coding assistant
    PARAMETER temperature 0.7
    """;

CreateModelRequest request = CreateModelRequest.builder()
    .name("my-custom-model")
    .modelfile(modelfile)
    .build();

api.createModel(request);
```

### Check Model Exists

```java
boolean exists = api.listModels().stream()
    .anyMatch(m -> m.getName().equals("llama3.1"));
```

---

## Streaming

### Stream Chat Responses

```java
import io.github.ollama4j.handlers.StreamHandler;

OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("user", "Tell me a long story")
    .stream(true)
    .build();

api.chatStream(request, new StreamHandler() {
    @Override
    public void onChunk(String chunk) {
        System.out.print(chunk);
    }

    @Override
    public void onComplete() {
        System.out.println("\n[Stream complete]");
    }

    @Override
    public void onError(Throwable error) {
        System.err.println("Error: " + error.getMessage());
    }
});
```

### Stream Generation

```java
OllamaGenerateRequest request = OllamaGenerateRequest.builder()
    .model("llama3.1")
    .prompt("Write a poem")
    .stream(true)
    .build();

api.generateStream(request, new StreamHandler() {
    @Override
    public void onChunk(String chunk) {
        System.out.print(chunk);
    }

    @Override
    public void onComplete() {
        System.out.println("\n[Done]");
    }

    @Override
    public void onError(Throwable error) {
        error.printStackTrace();
    }
});
```

---

## Authentication

### Basic Authentication

```java
Ollama api = new Ollama("http://localhost:11434");
api.setBasicAuth("username", "password");
```

### Bearer Token

```java
Ollama api = new Ollama("http://localhost:11434");
api.setBearerToken("your-bearer-token");
```

### Custom Headers

```java
import java.util.Map;

Ollama api = new Ollama("http://localhost:11434");
api.setCustomHeaders(Map.of(
    "X-Custom-Header", "value",
    "Authorization", "Bearer custom-token"
));
```

---

## Configuration

### Request Timeouts

```java
Ollama api = new Ollama("http://localhost:11434");
api.setRequestTimeout(60000);  // 60 seconds in milliseconds
```

### Global Options

```java
import io.github.ollama4j.models.generate.OllamaGenerateOptions;

// Set default options for all requests
OllamaGenerateOptions defaultOptions = OllamaGenerateOptions.builder()
    .temperature(0.7)
    .topP(0.9)
    .numPredict(500)
    .build();

// Use in request
OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("user", "Hello")
    .options(defaultOptions)
    .build();
```

### Common Options Reference

| Option | Type | Description | Default |
|--------|------|-------------|---------|
| `temperature` | double | Randomness (0.0-2.0) | 0.8 |
| `topP` | double | Nucleus sampling (0.0-1.0) | 0.9 |
| `topK` | int | Top-K sampling | 40 |
| `numPredict` | int | Max tokens to generate | 128 |
| `numCtx` | int | Context window size | 2048 |
| `stop` | List<String> | Stop sequences | null |
| `seed` | int | Random seed for reproducibility | Random |
| `repeatPenalty` | double | Penalty for repetition | 1.1 |

---

## Prometheus Metrics

### Enable Metrics

```java
import io.github.ollama4j.metrics.PrometheusMetrics;

Ollama api = new Ollama("http://localhost:11434");

// Enable Prometheus metrics
PrometheusMetrics metrics = new PrometheusMetrics();
api.enableMetrics(metrics);

// Access metrics
System.out.println("Total requests: " + metrics.getTotalRequests());
System.out.println("Average latency: " + metrics.getAverageLatency());
System.out.println("Error rate: " + metrics.getErrorRate());
```

---

## Error Handling

### Common Exceptions

```java
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.exceptions.ModelNotFoundException;
import io.github.ollama4j.exceptions.ConnectionException;

try {
    OllamaChatResult result = api.chat(request);
} catch (ModelNotFoundException e) {
    System.err.println("Model not found: " + e.getMessage());
    // Suggest pulling the model
} catch (ConnectionException e) {
    System.err.println("Cannot connect to Ollama: " + e.getMessage());
    // Check if Ollama is running
} catch (OllamaException e) {
    System.err.println("Ollama error: " + e.getMessage());
    e.printStackTrace();
}
```

---

## Best Practices

### 1. Reuse API Instance

```java
// Good - reuse
private static final Ollama api = new Ollama();

// Bad - creates new instance every time
public Ollama getApi() {
    return new Ollama();
}
```

### 2. Use Async for Long Operations

```java
// Good - non-blocking
CompletableFuture<OllamaGenerateResult> future = api.generateAsync(request);
future.thenAccept(result -> processResult(result));

// Bad - blocks thread
OllamaGenerateResult result = api.generate(request);  // Blocks
```

### 3. Handle Streaming Properly

```java
// Good - proper error handling
api.chatStream(request, new StreamHandler() {
    private StringBuilder buffer = new StringBuilder();

    @Override
    public void onChunk(String chunk) {
        buffer.append(chunk);
    }

    @Override
    public void onComplete() {
        processFullResponse(buffer.toString());
    }

    @Override
    public void onError(Throwable error) {
        log.error("Stream error", error);
        handleError(error);
    }
});
```

### 4. Model Validation

```java
// Check if model exists before using
private boolean ensureModelExists(String modelName) {
    boolean exists = api.listModels().stream()
        .anyMatch(m -> m.getName().equals(modelName));

    if (!exists) {
        log.info("Pulling model: {}", modelName);
        api.pullModel(modelName);
    }

    return true;
}
```

### 5. Graceful Degradation

```java
public String chat(String message) {
    try {
        OllamaChatRequest request = OllamaChatRequest.builder()
            .model("llama3.1")
            .message("user", message)
            .build();

        return api.chat(request).getMessage().getContent();

    } catch (OllamaException e) {
        log.error("Ollama error", e);
        return "I'm having trouble connecting to the AI service. Please try again.";
    }
}
```

---

## Additional Resources

- **GitHub Repository:** https://github.com/ollama4j/ollama4j
- **Official Documentation:** https://ollama4j.github.io/ollama4j/
- **Code Examples:** https://github.com/ollama4j/ollama4j-examples
- **Javadoc:** https://ollama4j.github.io/ollama4j/apidocs/
- **Maven Central:** https://mvnrepository.com/artifact/io.github.ollama4j/ollama4j
- **Issue Tracker:** https://github.com/ollama4j/ollama4j/issues

---

**Document Version:** 1.0
**Last Updated:** 2026-02-07
**Library Version:** 1.1.4
