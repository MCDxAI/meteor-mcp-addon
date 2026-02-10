# Ollama4j 1.1.4 API Reference

**Source**: Direct analysis of [ollama4j GitHub repository](https://github.com/ollama4j/ollama4j)
**Version**: 1.1.4 (Latest: 1.1.6)
**Package**: `io.github.ollama4j`

## Key Findings

### 1. Main API Class: `Ollama` (NOT `OllamaAPI`)

**Package**: `io.github.ollama4j.Ollama`

```java
import io.github.ollama4j.Ollama;

// Constructors
Ollama ollama = new Ollama(); // Default: http://localhost:11434
Ollama ollama = new Ollama("http://localhost:11434");

// Configuration
ollama.setRequestTimeoutSeconds(30);
ollama.setMaxChatToolCallRetries(3);
ollama.setBasicAuth("username", "password");
ollama.setBearerAuth("token");
```

### 2. Chat Request Building

**Package**: `io.github.ollama4j.models.chat.OllamaChatRequest`

**Builder Pattern** (Fluent API, NOT separate builder class):

```java
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;

OllamaChatRequest request = OllamaChatRequest.builder()
    .withModel("llama3.2")  // Use withModel(), NOT model()
    .withMessage(OllamaChatMessageRole.USER, "Hello!")
    .withOptions(options)
    .withKeepAlive("5m")
    .build();  // NO build() method needed - returns OllamaChatRequest directly
```

**Key Methods**:
- `static OllamaChatRequest builder()` - Returns new `OllamaChatRequest` (not a separate builder)
- `withModel(String model)` - Set model name (returns `this`)
- `withMessage(OllamaChatMessageRole role, String content)` - Add message
- `withMessage(role, content, toolCalls)` - Add message with tool calls
- `withMessages(List<OllamaChatMessage> messages)` - Set all messages
- `withOptions(Options options)` - Set generation options
- `withGetJsonResponse()` - Request JSON format
- `withTemplate(String template)` - Set prompt template
- `withStreaming()` - Enable streaming
- `withKeepAlive(String keepAlive)` - Set keep-alive duration

**Important**: All `with*()` methods return `OllamaChatRequest` (the object itself), NOT a builder.

### 3. Chat Response Handling

**Package**: `io.github.ollama4j.models.chat.OllamaChatResult`

```java
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.models.chat.OllamaChatMessage;

OllamaChatResult result = ollama.chat(request);

// Access response - Chain through getResponseModel().getMessage().getResponse()
String responseText = result.getResponseModel().getMessage().getResponse();

// Access other data
OllamaChatResponseModel responseModel = result.getResponseModel();
String model = responseModel.getModel();
String doneReason = responseModel.getDoneReason();
Long totalDuration = responseModel.getTotalDuration();
String error = responseModel.getError();

// Access message details
OllamaChatMessage message = responseModel.getMessage();
OllamaChatMessageRole role = message.getRole();
String content = message.getResponse();  // Note: field is called "response"
String thinking = message.getThinking();
List<OllamaChatToolCalls> toolCalls = message.getToolCalls();

// Access chat history
List<OllamaChatMessage> history = result.getChatHistory();
```

**OllamaChatResult Fields**:
- `getResponseModel()` - Returns `OllamaChatResponseModel`
- `getChatHistory()` - Returns `List<OllamaChatMessage>` (includes user messages + assistant response)

**OllamaChatResponseModel Fields**:
- `getMessage()` - Returns `OllamaChatMessage` (the assistant's response)
- `getModel()` - Model name
- `getCreatedAt()` - Timestamp
- `getDoneReason()` - Completion reason
- `isDone()` - Completion status
- `getContext()` - Context array
- `getTotalDuration()`, `getLoadDuration()`, `getPromptEvalDuration()`, `getEvalDuration()`
- `getPromptEvalCount()`, `getEvalCount()`
- `getError()` - Error message if any

**OllamaChatMessage Fields**:
- `getRole()` - Returns `OllamaChatMessageRole` (USER, ASSISTANT, SYSTEM, TOOL)
- `getResponse()` - Message content (NOT `getMessage()`)
- `getThinking()` - Model thinking process (if enabled)
- `getToolCalls()` - List of tool calls made by model
- `getImages()` - List of images as byte arrays

### 4. Executing Chat Requests

**Package**: `io.github.ollama4j.Ollama`

```java
// Synchronous chat (blocking)
OllamaChatResult result = ollama.chat(OllamaChatRequest request) throws OllamaException;

// Chat with streaming token handler
OllamaChatResult result = ollama.chat(
    OllamaChatRequest request,
    OllamaChatTokenHandler tokenHandler
) throws OllamaException;

// Streaming example
ollama.chat(request, token -> {
    System.out.print(token);
});
```

### 5. Tool Calling API

**Packages**:
- `io.github.ollama4j.tools.Tools` - Tool definitions
- `io.github.ollama4j.tools.annotations.ToolSpec` - Annotation for auto-discovery
- `io.github.ollama4j.tools.ToolFunction` - Function implementation interface

#### Tool Structure

```java
import io.github.ollama4j.tools.Tools;
import io.github.ollama4j.tools.Tools.Tool;
import io.github.ollama4j.tools.Tools.ToolSpec;
import io.github.ollama4j.tools.Tools.Parameters;
import io.github.ollama4j.tools.Tools.Property;
import io.github.ollama4j.tools.ToolFunction;

// Define tool specification
ToolSpec toolSpec = ToolSpec.builder()
    .name("get_weather")
    .description("Get current weather for a city")
    .parameters(Parameters.of(Map.of(
        "city", Property.builder()
            .type("string")
            .description("City name")
            .required(true)
            .build()
    )))
    .build();

// Define tool function
ToolFunction toolFunction = (Map<String, Object> arguments) -> {
    String city = (String) arguments.get("city");
    // ... implement function logic
    return Map.of("temperature", 72, "conditions", "sunny");
};

// Create tool
Tool tool = Tool.builder()
    .toolSpec(toolSpec)
    .toolFunction(toolFunction)
    .build();

// Register tool
ollama.registerTool(tool);
```

#### Tool Registration Methods

```java
// Register single tool
ollama.registerTool(Tool tool);

// Register multiple tools
ollama.registerTools(List<Tool> tools);

// Get registered tools
List<Tool> tools = ollama.getRegisteredTools();

// Clear all tools
ollama.deregisterTools();

// Auto-register annotated tools (see annotations section)
ollama.registerAnnotatedTools(Class<?> providerClass);
```

#### Annotation-Based Tool Definition

```java
import io.github.ollama4j.tools.annotations.ToolSpec;
import io.github.ollama4j.tools.annotations.ToolProperty;
import io.github.ollama4j.tools.annotations.OllamaToolService;

@OllamaToolService
public class MyTools {

    @ToolSpec(
        name = "get_weather",
        desc = "Get current weather for a city"
    )
    public Map<String, Object> getWeather(
        @ToolProperty(name = "city", desc = "City name", required = true)
        String city
    ) {
        // Implementation
        return Map.of("temperature", 72, "conditions", "sunny");
    }
}

// Register all annotated tools from the class
ollama.registerAnnotatedTools(MyTools.class);
```

#### Tool Calling in Chat

**Automatic Tool Execution** (default):

```java
OllamaChatRequest request = OllamaChatRequest.builder()
    .withModel("llama3.2")
    .withMessage(OllamaChatMessageRole.USER, "What's the weather in Paris?");
    // Tools are automatically added from registry when useTools=true (default)

OllamaChatResult result = ollama.chat(request);
// Tools are called automatically, result contains final response
```

**Manual Tool Handling**:

```java
OllamaChatRequest request = OllamaChatRequest.builder()
    .withModel("llama3.2")
    .withMessage(OllamaChatMessageRole.USER, "What's the weather in Paris?");

// Disable automatic tool execution
request.setUseTools(false);

OllamaChatResult result = ollama.chat(request);

// Check for tool calls
List<OllamaChatToolCalls> toolCalls = result.getResponseModel()
    .getMessage()
    .getToolCalls();

if (toolCalls != null && !toolCalls.isEmpty()) {
    for (OllamaChatToolCalls toolCall : toolCalls) {
        String toolName = toolCall.getFunction().getName();
        Map<String, Object> args = toolCall.getFunction().getArguments();
        // Execute tool manually
        // ... add tool result to chat history and continue
    }
}
```

#### Tool Classes

**Tools.Tool**:
- `toolSpec` (ToolSpec) - Tool specification for LLM
- `type` (String) - Always "function"
- `toolFunction` (ToolFunction) - Implementation
- `isMCPTool` (boolean) - Internal flag for MCP tools
- `mcpServerName` (String) - MCP server name (if applicable)

**Tools.ToolSpec**:
- `name` (String) - Tool name
- `description` (String) - Tool description
- `parameters` (Parameters) - Parameter definitions

**Tools.Parameters**:
- `properties` (Map<String, Property>) - Parameter definitions
- `required` (List<String>) - Required parameter names
- `toString()` - Returns JSON Schema representation

**Tools.Property**:
- `type` (String) - Parameter type ("string", "number", "boolean", "object", "array")
- `description` (String) - Parameter description
- `enumValues` (List<String>) - Allowed values for enum types
- `required` (boolean) - Whether parameter is required (used when building Parameters)

### 6. Options Configuration

**Package**: `io.github.ollama4j.utils.Options`

```java
import io.github.ollama4j.utils.Options;

Options options = Options.builder()
    .temperature(0.7)
    .topK(40)
    .topP(0.9)
    .numPredict(100)
    .build();

request.withOptions(options);
```

### 7. Error Handling

```java
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.exceptions.ToolInvocationException;
import io.github.ollama4j.exceptions.RoleNotFoundException;

try {
    OllamaChatResult result = ollama.chat(request);
} catch (OllamaException e) {
    // Handle Ollama API errors
    System.err.println("Error: " + e.getMessage());
}
```

## Complete Working Example

```java
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.*;
import io.github.ollama4j.tools.Tools.*;
import io.github.ollama4j.tools.ToolFunction;
import java.util.Map;

public class OllamaExample {
    public static void main(String[] args) throws OllamaException {
        // Initialize client
        Ollama ollama = new Ollama("http://localhost:11434");
        ollama.setRequestTimeoutSeconds(30);

        // Define and register a tool
        ToolSpec weatherSpec = ToolSpec.builder()
            .name("get_weather")
            .description("Get weather for a city")
            .parameters(Parameters.of(Map.of(
                "city", Property.builder()
                    .type("string")
                    .description("City name")
                    .required(true)
                    .build()
            )))
            .build();

        ToolFunction weatherFunc = (args) -> {
            String city = (String) args.get("city");
            return Map.of(
                "temperature", 72,
                "conditions", "sunny",
                "city", city
            );
        };

        Tool weatherTool = Tool.builder()
            .toolSpec(weatherSpec)
            .toolFunction(weatherFunc)
            .build();

        ollama.registerTool(weatherTool);

        // Build chat request
        OllamaChatRequest request = OllamaChatRequest.builder()
            .withModel("llama3.2")
            .withMessage(OllamaChatMessageRole.USER, "What's the weather in Paris?");

        // Execute chat (tools called automatically)
        OllamaChatResult result = ollama.chat(request);

        // Get response
        String response = result.getResponseModel().getMessage().getResponse();
        System.out.println("Assistant: " + response);

        // Access metadata
        System.out.println("Model: " + result.getResponseModel().getModel());
        System.out.println("Duration: " + result.getResponseModel().getTotalDuration() + "ns");
    }
}
```

## Common Mistakes to Avoid

1. **Wrong main class**: Use `Ollama`, NOT `OllamaAPI`
2. **Wrong builder method**: Use `.withModel()`, NOT `.model()`
3. **Wrong response accessor**: Use `result.getResponseModel().getMessage().getResponse()`, NOT `result.getMessage()`
4. **Calling build()**: Builder methods return `OllamaChatRequest` directly, no `.build()` needed
5. **Wrong message field**: `OllamaChatMessage` has `getResponse()`, NOT `getContent()` or `getMessage()`
6. **Tool classes**: Import from `io.github.ollama4j.tools.Tools.*`, NOT separate top-level classes

## Migration from Documentation Errors

If you have code based on incorrect documentation:

```java
// WRONG (from bad docs)
OllamaAPI api = new OllamaAPI("http://localhost:11434");
OllamaChatRequest request = new OllamaChatRequestBuilder()
    .model("llama3.2")  // Wrong method name
    .build();  // Unnecessary
String response = result.getMessage();  // Wrong accessor

// CORRECT (actual API)
Ollama ollama = new Ollama("http://localhost:11434");
OllamaChatRequest request = OllamaChatRequest.builder()
    .withModel("llama3.2")  // Correct method
    // Returns OllamaChatRequest directly, no .build()
String response = result.getResponseModel().getMessage().getResponse();
```

## Version Information

- **Current Analysis**: Based on main branch (latest)
- **Package Maven Coordinates**: `io.github.ollama4j:ollama4j:1.1.4`
- **Latest Release**: 1.1.6 (as of 2025)
- **Minimum Java Version**: 17
- **Minimum Ollama Version**: 0.11.10

## Additional Resources

- GitHub Repository: https://github.com/ollama4j/ollama4j
- Examples Repository: https://github.com/ollama4j/ollama4j-examples
- Official Documentation: https://ollama4j.github.io/ollama4j/
- JavaDoc (Latest): https://ollama4j.github.io/ollama4j/apidocs/
- Maven Central: https://central.sonatype.com/artifact/io.github.ollama4j/ollama4j
