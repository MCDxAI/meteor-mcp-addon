# Ollama Quick Reference

**Last Updated:** 2026-02-07

## Installation & Setup

### Install Ollama
```bash
# Download from https://ollama.com/download
# Or use package manager
```

### Start Ollama Server
```bash
ollama serve
# Runs on http://localhost:11434
```

### Pull a Model
```bash
ollama pull llama3.1       # Default size
ollama pull llama3.1:8b    # Specific size
ollama pull llama3.1:70b   # Large model
```

### List Local Models
```bash
ollama list
```

### Remove a Model
```bash
ollama rm llama3.1
```

---

## Ollama4j Dependency

### Maven
```xml
<dependency>
    <groupId>io.github.ollama4j</groupId>
    <artifactId>ollama4j</artifactId>
    <version>1.1.4</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
implementation("io.github.ollama4j:ollama4j:1.1.4")
```

### With Shading (for Minecraft mods)
```kotlin
include(implementation("io.github.ollama4j:ollama4j:1.1.4")!!)
```

---

## Basic Usage

### Initialize Client
```java
import io.github.ollama4j.OllamaAPI;

OllamaAPI api = new OllamaAPI("http://localhost:11434");
```

### Simple Chat
```java
import io.github.ollama4j.models.chat.OllamaChatRequest;

OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("user", "Hello!")
    .build();

String response = api.chat(request).getMessage().getContent();
```

### With System Prompt
```java
OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("system", "You are a helpful assistant")
    .message("user", "Hello!")
    .build();
```

---

## Tool Calling

### Define Tool with Annotations
```java
import io.github.ollama4j.annotations.Tool;
import io.github.ollama4j.annotations.ToolParam;

public class WeatherTools {
    @Tool(
        name = "get_weather",
        description = "Get current weather"
    )
    public String getWeather(
        @ToolParam(name = "city", required = true) String city
    ) {
        return "Weather in " + city + ": Sunny, 72°F";
    }
}
```

### Register Tools
```java
api.registerAnnotatedTools(new WeatherTools());
```

### Use Tools in Chat
```java
OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")  // Must support tools
    .message("user", "What's the weather in Tokyo?")
    .withTools()
    .build();

OllamaChatResult result = api.chat(request);
```

---

## Streaming

### Stream Chat
```java
import io.github.ollama4j.handlers.StreamHandler;

OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("user", "Tell me a story")
    .stream(true)
    .build();

api.chatStream(request, new StreamHandler() {
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

## Model Management

### List Models
```java
import io.github.ollama4j.models.Model;
import java.util.List;

List<Model> models = api.listModels();
models.forEach(m -> System.out.println(m.getName()));
```

### Pull Model
```java
api.pullModel("llama3.1");
```

### Delete Model
```java
api.deleteModel("llama3.1");
```

### Check if Model Exists
```java
boolean exists = api.listModels().stream()
    .anyMatch(m -> m.getName().equals("llama3.1"));
```

---

## Configuration

### Request Options
```java
import io.github.ollama4j.models.generate.OllamaGenerateOptions;

OllamaGenerateOptions options = OllamaGenerateOptions.builder()
    .temperature(0.7)      // Randomness (0.0-2.0)
    .topP(0.9)             // Nucleus sampling
    .topK(40)              // Top-K sampling
    .numPredict(500)       // Max tokens
    .seed(12345)           // Reproducibility
    .stop(List.of("\n"))   // Stop sequences
    .build();

OllamaChatRequest request = OllamaChatRequest.builder()
    .model("llama3.1")
    .message("user", "Hello")
    .options(options)
    .build();
```

### Common Options

| Option | Default | Range | Description |
|--------|---------|-------|-------------|
| `temperature` | 0.8 | 0.0-2.0 | Randomness |
| `topP` | 0.9 | 0.0-1.0 | Nucleus sampling |
| `topK` | 40 | 1-100 | Top-K sampling |
| `numPredict` | 128 | 1-∞ | Max output tokens |
| `numCtx` | 2048 | 1-∞ | Context window |

---

## Models Reference

### Recommended Models (2026)

| Model | Size | Use Case | Tool Support |
|-------|------|----------|--------------|
| `llama3.1:8b` | 4.7GB | General chat | ✅ Excellent |
| `llama3.2:3b` | 2.0GB | Fast responses | ✅ Good |
| `llama3.3:70b` | 40GB | Complex reasoning | ✅ Excellent |
| `mistral:7b` | 4.1GB | Balanced | ✅ Very Good |
| `qwen2.5:0.5b` | 400MB | Minimal RAM | ❌ No |
| `qwen2.5:7b` | 4.7GB | Code/reasoning | ✅ Good |
| `qwen3:14b` | 8.3GB | Latest Qwen | ✅ Very Good |
| `functiongemma` | 270MB | Tool calling only | ✅ Specialized |

### Pull Command
```bash
ollama pull llama3.1:8b
```

---

## REST API

### Chat Completion
```bash
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.1",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "stream": false
}'
```

### OpenAI-Compatible Endpoint
```bash
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ollama" \
  -d '{
    "model": "llama3.1",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

### Generate (Completion)
```bash
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.1",
  "prompt": "Once upon a time",
  "stream": false
}'
```

### Embeddings
```bash
curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text",
  "prompt": "Hello world"
}'
```

---

## Error Handling

```java
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.exceptions.ModelNotFoundException;
import io.github.ollama4j.exceptions.ConnectionException;

try {
    OllamaChatResult result = api.chat(request);
    System.out.println(result.getMessage().getContent());

} catch (ModelNotFoundException e) {
    System.err.println("Model not found. Try: ollama pull " + request.getModel());

} catch (ConnectionException e) {
    System.err.println("Can't connect to Ollama. Is it running?");
    System.err.println("Start with: ollama serve");

} catch (OllamaException e) {
    System.err.println("Ollama error: " + e.getMessage());
}
```

---

## Async Execution

```java
import java.util.concurrent.CompletableFuture;

CompletableFuture<OllamaChatResult> future = api.chatAsync(request);

future.thenAccept(result -> {
    System.out.println(result.getMessage().getContent());
}).exceptionally(error -> {
    System.err.println("Error: " + error.getMessage());
    return null;
});
```

---

## Best Practices

### 1. Reuse API Instance
```java
// Good
private static final OllamaAPI api = new OllamaAPI();

// Bad
public void chat() {
    OllamaAPI api = new OllamaAPI();  // Creates new instance every call
}
```

### 2. Check Model Exists
```java
public void ensureModel(String model) {
    boolean exists = api.listModels().stream()
        .anyMatch(m -> m.getName().equals(model));

    if (!exists) {
        System.out.println("Pulling model: " + model);
        api.pullModel(model);
    }
}
```

### 3. Use Streaming for Long Responses
```java
// Long responses (stories, articles)
request.stream(true);
api.chatStream(request, handler);

// Short responses (answers, facts)
request.stream(false);
api.chat(request);
```

### 4. Set Appropriate Timeouts
```java
api.setRequestTimeout(60000);  // 60 seconds for slow models
```

### 5. Handle Errors Gracefully
```java
try {
    return api.chat(request).getMessage().getContent();
} catch (OllamaException e) {
    return "Sorry, I'm having trouble connecting to the AI service.";
}
```

---

## Testing Connection

### Check if Ollama is Running
```java
public boolean isOllamaRunning() {
    try {
        OllamaAPI api = new OllamaAPI();
        api.listModels();
        return true;
    } catch (ConnectionException e) {
        return false;
    }
}
```

### Test Chat
```java
public void testChat() {
    OllamaAPI api = new OllamaAPI();

    OllamaChatRequest request = OllamaChatRequest.builder()
        .model("llama3.1")
        .message("user", "Say 'test successful'")
        .build();

    try {
        String response = api.chat(request).getMessage().getContent();
        System.out.println("✓ Ollama working: " + response);
    } catch (Exception e) {
        System.err.println("✗ Ollama test failed: " + e.getMessage());
    }
}
```

---

## Common Issues

### "Connection refused"
```
Error: Connection refused
Fix: Start Ollama server with 'ollama serve'
```

### "Model not found"
```
Error: Model 'llama3.1' not found
Fix: Pull the model with 'ollama pull llama3.1'
```

### "Out of memory"
```
Error: Out of memory
Fix: Use a smaller model (e.g., llama3.2:3b instead of llama3.3:70b)
```

### "Timeout"
```
Error: Request timeout
Fix: Increase timeout or use a faster model
api.setRequestTimeout(120000);  // 2 minutes
```

### "Tool calling not working"
```
Error: Model doesn't respond to tools
Fix: Ensure model supports tools (llama3.1, mistral, etc.)
```

---

## Links

- **Ollama:** https://ollama.com/
- **Ollama4j:** https://github.com/ollama4j/ollama4j
- **Docs:** https://ollama4j.github.io/ollama4j/
- **Models:** https://ollama.com/library
- **API Docs:** https://docs.ollama.com/

---

**Cheat Sheet Version:** 1.0
**Last Updated:** 2026-02-07
