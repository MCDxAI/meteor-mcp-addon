# Ollama Integration Recommendations for meteor-mcp-addon

**Date:** 2026-02-07
**Context:** Adding Ollama support to the existing Gemini-based meteor-mcp-addon

## Executive Summary

Based on research into Ollama's Java ecosystem, this document provides recommendations for integrating Ollama as an alternative/additional LLM provider alongside the existing Google Gemini integration.

---

## Quick Answer to Your Questions

### 1. Google GenAI SDK Compatibility with Ollama

**Answer: Technically possible, but NOT recommended.**

- The Google GenAI Java SDK supports custom endpoints via `HttpOptions.builder().baseUrl()`
- Ollama has an OpenAI-compatible endpoint at `http://localhost:11434/v1`
- **However:** Google's API format differs significantly from OpenAI's, especially for function calling
- **Verdict:** Would require extensive adapter code and lose benefits of both systems

### 2. Ollama's API Compatibility

**Answer: YES, Ollama exposes an OpenAI-compatible API.**

- **Endpoint:** `http://localhost:11434/v1`
- **Supported:** Chat completions, completions, embeddings, tool calling
- **Format:** Drop-in replacement for OpenAI API (with some limitations)
- **API Key:** Dummy key `"ollama"` required but unused

### 3. Best Java Library for Ollama

**Answer: Ollama4j (io.github.ollama4j:ollama4j:1.1.4)**

**Why Ollama4j:**
- Purpose-built for Ollama
- Active development (474 GitHub stars)
- Complete feature support (chat, tools, vision, embeddings)
- Annotation-based tool calling (very clean API)
- MCP support explicitly mentioned
- Java 17+ (matches Minecraft mod requirements)

**Maven/Gradle:**
```gradle
implementation("io.github.ollama4j:ollama4j:1.1.4")
```

### 4. Function Calling Support

**Answer: YES, Ollama supports function calling.**

**Supported Models (2026):**
- Llama 3.1 (8B, 70B, 405B) - Best overall
- Llama 3.2 (1B, 3B) - Efficient
- Llama 3.3 (70B) - Latest
- Mistral (7B+) - Good performance
- Qwen 2.5/3.x - Wide range
- FunctionGemma (270M) - Specialized for functions

### 5. REST API Format

**Endpoint:** `POST http://localhost:11434/api/chat` or `POST http://localhost:11434/v1/chat/completions`

**Tool Calling Request:**
```json
{
  "model": "llama3.1",
  "messages": [{"role": "user", "content": "What's the weather?"}],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get weather for a city",
        "parameters": {
          "type": "object",
          "required": ["city"],
          "properties": {
            "city": {"type": "string"}
          }
        }
      }
    }
  ]
}
```

**Response with Tool Call:**
```json
{
  "message": {
    "role": "assistant",
    "tool_calls": [
      {
        "type": "function",
        "function": {
          "name": "get_weather",
          "arguments": {"city": "Tokyo"}
        }
      }
    ]
  }
}
```

---

## Integration Strategy for meteor-mcp-addon

### Current Architecture

```
GeminiConfig (NBT-persisted)
    ↓
GeminiClientManager (cached client)
    ↓
GeminiExecutor (manual function calling loop)
    ↓
MCPToGeminiBridge (schema conversion + routing)
    ↓
MCPServerConnection.callTool()
```

### Recommended Ollama Architecture

**Option 1: Parallel System (Recommended)**

Create a separate, parallel system for Ollama:

```
OllamaConfig (NBT-persisted, separate from GeminiConfig)
    ↓
OllamaClientManager (wraps OllamaAPI instance)
    ↓
OllamaExecutor (manual function calling loop, similar to GeminiExecutor)
    ↓
MCPToOllamaBridge (schema conversion, similar to MCPToGeminiBridge)
    ↓
MCPServerConnection.callTool() (same as Gemini)
```

**Benefits:**
- Clean separation of concerns
- Users can enable Gemini, Ollama, or both
- Different models for different use cases (Gemini for complex reasoning, Ollama for fast local)
- Easier to maintain/debug

**Option 2: Unified Provider System**

Abstract both Gemini and Ollama behind a common interface:

```
LLMProvider interface
    ├─ GeminiProvider
    └─ OllamaProvider

MCPToolCaller (provider-agnostic)
    ↓
LLMProvider.callWithTools()
    ↓
MCPServerConnection.callTool()
```

**Benefits:**
- Easier to add more providers later
- Cleaner code structure
- Single StarScript interface

**Cons:**
- More upfront refactoring
- Need to handle provider-specific features

---

## Implementation Plan

### Phase 1: Add Ollama4j Dependency

**build.gradle.kts:**
```kotlin
dependencies {
    // Existing dependencies
    include(implementation("com.google.genai:google-genai:1.23.0")!!)

    // Add Ollama4j
    include(implementation("io.github.ollama4j:ollama4j:1.1.4")!!)

    // ... rest of dependencies
}
```

### Phase 2: Create Ollama Configuration

**OllamaConfig.java:**
```java
public class OllamaConfig {
    private boolean enabled = false;
    private String host = "http://localhost:11434";
    private String defaultModel = "llama3.1";
    private int timeout = 60000;  // 60 seconds

    // NBT serialization
    public NbtCompound toTag() { ... }
    public static OllamaConfig fromTag(NbtCompound tag) { ... }
}
```

**Add to MCPServers.java:**
```java
public class MCPServers extends System<MCPServers> {
    private final Map<String, MCPServerConnection> servers;
    private final Map<String, MCPServerConfig> configs;

    private GeminiConfig geminiConfig;
    private OllamaConfig ollamaConfig;  // ADD THIS

    // ... rest of implementation
}
```

### Phase 3: Create OllamaExecutor

**OllamaExecutor.java:**
```java
public class OllamaExecutor {
    private final OllamaAPI api;

    public OllamaExecutor(String host) {
        this.api = new OllamaAPI(host);
    }

    // Simple prompt (no tools)
    public String execute(String model, String prompt) {
        OllamaChatRequest request = OllamaChatRequest.builder()
            .model(model)
            .message("user", prompt)
            .build();

        return api.chat(request).getMessage().getContent();
    }

    // MCP-enhanced loop (with tool calling)
    public String executeWithMCP(String model, String prompt, List<MCPServerConnection> servers) {
        // Register MCP tools
        for (MCPServerConnection server : servers) {
            registerServerTools(server);
        }

        // Chat with tools enabled
        OllamaChatRequest request = OllamaChatRequest.builder()
            .model(model)
            .message("user", prompt)
            .withTools()
            .build();

        OllamaChatResult result = api.chat(request);

        // Manual function calling loop
        while (result.getMessage().hasToolCalls()) {
            // Execute tool calls
            List<ToolCall> toolCalls = result.getMessage().getToolCalls();
            List<String> toolResults = new ArrayList<>();

            for (ToolCall call : toolCalls) {
                String serverName = extractServerName(call.getFunction().getName());
                String toolName = extractToolName(call.getFunction().getName());
                Map<String, Object> args = call.getFunction().getArguments();

                MCPServerConnection server = findServer(serverName);
                CallToolResult mcpResult = server.callTool(toolName, args);

                toolResults.add(formatToolResult(mcpResult));
            }

            // Send tool results back
            // ... (similar to GeminiExecutor pattern)
        }

        return result.getMessage().getContent();
    }

    private void registerServerTools(MCPServerConnection server) {
        for (Tool tool : server.listTools()) {
            ToolSpec spec = convertMCPToolToOllamaSpec(tool);
            api.registerTool(spec, (args) -> {
                CallToolResult result = server.callTool(tool.name(), args);
                return formatToolResult(result);
            });
        }
    }
}
```

### Phase 4: Add StarScript Integration

**Update GeminiStarScriptIntegration.java → LLMStarScriptIntegration.java:**

```java
public class LLMStarScriptIntegration {

    public static void register() {
        registerGemini();
        registerOllama();  // ADD THIS
    }

    private static void registerOllama() {
        // {ollama("prompt")} - Simple prompt
        MeteorStarscript.ss.set("ollama", Value.function(args -> {
            if (args.size() != 1) return Value.null_();

            OllamaConfig config = MCPServers.get().getOllamaConfig();
            if (!config.isEnabled()) {
                return Value.string("[Ollama disabled]");
            }

            String prompt = args.get(0).toString();
            OllamaExecutor executor = new OllamaExecutor(config.getHost());

            // Async execution
            MeteorExecutor.execute(() -> {
                try {
                    String result = executor.execute(config.getDefaultModel(), prompt);
                    cachedResults.put("ollama_" + prompt, result);
                } catch (Exception e) {
                    cachedResults.put("ollama_" + prompt, "[Error: " + e.getMessage() + "]");
                }
            });

            return Value.string(cachedResults.getOrDefault("ollama_" + prompt, "Loading..."));
        }));

        // {ollama_mcp("prompt")} - With MCP tools
        MeteorStarscript.ss.set("ollama_mcp", Value.function(args -> {
            if (args.size() != 1) return Value.null_();

            OllamaConfig config = MCPServers.get().getOllamaConfig();
            if (!config.isEnabled()) {
                return Value.string("[Ollama disabled]");
            }

            String prompt = args.get(0).toString();
            OllamaExecutor executor = new OllamaExecutor(config.getHost());

            MeteorExecutor.execute(() -> {
                try {
                    List<MCPServerConnection> servers = getConnectedServers();
                    String result = executor.executeWithMCP(
                        config.getDefaultModel(),
                        prompt,
                        servers
                    );
                    cachedResults.put("ollama_mcp_" + prompt, result);
                } catch (Exception e) {
                    cachedResults.put("ollama_mcp_" + prompt, "[Error: " + e.getMessage() + "]");
                }
            });

            return Value.string(cachedResults.getOrDefault("ollama_mcp_" + prompt, "Loading..."));
        }));
    }
}
```

### Phase 5: Add Chat Commands

**OllamaCommand.java:**
```java
public class OllamaCommand extends Command {
    public OllamaCommand() {
        super("ollama", "Chat with Ollama AI");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("chat").then(argument("prompt", greedyString())
            .executes(context -> {
                String prompt = getString(context, "prompt");
                OllamaConfig config = MCPServers.get().getOllamaConfig();

                if (!config.isEnabled()) {
                    error("Ollama is disabled. Enable it in Meteor GUI > MCP > Ollama Settings");
                    return SINGLE_SUCCESS;
                }

                info("Asking Ollama...");

                MeteorExecutor.execute(() -> {
                    try {
                        OllamaExecutor executor = new OllamaExecutor(config.getHost());
                        String result = executor.execute(config.getDefaultModel(), prompt);
                        info(result);
                    } catch (Exception e) {
                        error("Ollama error: " + e.getMessage());
                    }
                });

                return SINGLE_SUCCESS;
            })
        ));
    }
}
```

**OllamaMCPCommand.java:**
```java
// Similar to GeminiMCPCommand but uses OllamaExecutor
```

### Phase 6: Add GUI Settings

**OllamaSettingsScreen.java:**
```java
public class OllamaSettingsScreen extends WindowScreen {
    private final OllamaConfig config;

    // Input fields
    private WTextBox hostInput;
    private WTextBox modelInput;
    private WIntSlider timeoutSlider;
    private WCheckbox enabledCheckbox;

    // Model selector dropdown
    private WDropdown<String> modelDropdown;

    @Override
    public void initWidgets() {
        // Enabled toggle
        enabledCheckbox = add(theme.checkbox(config.isEnabled())).widget();

        // Host input
        hostInput = add(theme.textBox(config.getHost())).widget();

        // Model dropdown (popular models)
        modelDropdown = add(theme.dropdown(
            config.getDefaultModel(),
            "llama3.1", "llama3.2", "llama3.3",
            "mistral", "qwen2.5", "qwen3"
        )).widget();

        // Timeout slider
        timeoutSlider = add(theme.slider(
            config.getTimeout() / 1000,  // Convert to seconds
            5, 300  // 5 seconds to 5 minutes
        )).widget();

        // Test connection button
        WButton testButton = add(theme.button("Test Connection")).widget();
        testButton.action = this::testConnection;

        // Save button
        WButton saveButton = add(theme.button("Save")).widget();
        saveButton.action = this::save;
    }

    private void testConnection() {
        OllamaAPI api = new OllamaAPI(hostInput.get());

        MeteorExecutor.execute(() -> {
            try {
                List<Model> models = api.listModels();
                Minecraft.getInstance().execute(() -> {
                    info("Connected! Found " + models.size() + " models");
                });
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> {
                    error("Connection failed: " + e.getMessage());
                });
            }
        });
    }

    private void save() {
        config.setEnabled(enabledCheckbox.checked);
        config.setHost(hostInput.get());
        config.setDefaultModel(modelDropdown.get());
        config.setTimeout(timeoutSlider.get() * 1000);

        MCPServers.get().save();
        close();
    }
}
```

### Phase 7: Update GUI Navigation

**MCPTab.java:**
```java
public class MCPTab extends Tab {
    @Override
    public void build(WContainer parent) {
        // Existing buttons
        parent.add(theme.button("Servers")).action(() ->
            mc.setScreen(new MCPServersScreen()));

        parent.add(theme.button("Gemini Settings")).action(() ->
            mc.setScreen(new GeminiSettingsScreen()));

        // ADD THIS
        parent.add(theme.button("Ollama Settings")).action(() ->
            mc.setScreen(new OllamaSettingsScreen()));
    }
}
```

---

## Dependency Shading Configuration

**build.gradle.kts:**
```kotlin
tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())

    // Existing relocations
    relocate("io.modelcontextprotocol", "com.cope.meteormcp.shaded.mcp")
    relocate("reactor", "com.cope.meteormcp.shaded.reactor")
    relocate("org.reactivestreams", "com.cope.meteormcp.shaded.reactivestreams")

    // ADD THESE for Ollama4j
    relocate("io.github.ollama4j", "com.cope.meteormcp.shaded.ollama4j")
    relocate("com.fasterxml.jackson", "com.cope.meteormcp.shaded.jackson")
    relocate("okhttp3", "com.cope.meteormcp.shaded.okhttp3")
    relocate("okio", "com.cope.meteormcp.shaded.okio")
}
```

---

## Comparison: Gemini vs Ollama

| Feature | Gemini | Ollama |
|---------|--------|---------|
| **Location** | Cloud (Google) | Local machine |
| **API Key** | Required | None |
| **Cost** | Paid (with free tier) | Free |
| **Speed** | Network latency | Local (very fast) |
| **Quality** | Very high (Gemini Pro/Flash) | Good (depends on model) |
| **Privacy** | Data sent to Google | Fully local |
| **Context Window** | 128k-1M tokens | 2k-128k (model dependent) |
| **Multimodal** | Yes (images, video, audio) | Yes (images only) |
| **Function Calling** | Native support | Native support |
| **Internet Required** | Yes | No |
| **Model Choice** | Google models only | 100+ models |

---

## Use Case Recommendations

### Use Gemini When:
- Complex reasoning required
- Large context windows needed (>32k tokens)
- Multimodal (video/audio) required
- Best-in-class quality needed
- Internet connection reliable

### Use Ollama When:
- Privacy critical (local data only)
- No internet connection
- Cost is a concern (free)
- Fast response time critical
- Experimenting with different models
- Running on powerful local hardware

### Use Both When:
- Fallback required (Gemini → Ollama if API fails)
- Different tasks (Gemini for complex, Ollama for simple)
- A/B testing model performance
- Users want choice

---

## StarScript Examples

### Simple Prompts
```
{gemini("What is 2+2?")}
{ollama("What is 2+2?")}
```

### MCP-Enhanced
```
{gemini_mcp("Check my system resources and create a performance report")}
{ollama_mcp("Check my system resources and create a performance report")}
```

### HUD Usage
```
Health: {mc.player.health}
AI Tip: {ollama("Give a Minecraft survival tip")}
```

### Conditional Usage
```
AI: {if(config.gemini_enabled, gemini("Hello"), ollama("Hello"))}
```

---

## Testing Strategy

### Unit Tests
```java
@Test
public void testOllamaConnection() {
    OllamaAPI api = new OllamaAPI("http://localhost:11434");
    List<Model> models = api.listModels();
    assertFalse(models.isEmpty());
}

@Test
public void testOllamaChat() {
    OllamaExecutor executor = new OllamaExecutor("http://localhost:11434");
    String result = executor.execute("llama3.1", "Say hello");
    assertNotNull(result);
    assertTrue(result.length() > 0);
}

@Test
public void testOllamaToolCalling() {
    // Test MCP tool integration
}
```

### Integration Tests
1. Start Ollama server: `ollama serve`
2. Pull test model: `ollama pull llama3.1`
3. Run Minecraft with mod
4. Test StarScript: `{ollama("test")}`
5. Test chat command: `.ollama chat test`
6. Test MCP tools: `.ollama-mcp "use the weather tool"`

---

## Performance Considerations

### Async Execution Critical
```java
// WRONG - blocks render thread
String result = executor.execute(model, prompt);

// CORRECT - async via MeteorExecutor
MeteorExecutor.execute(() -> {
    String result = executor.execute(model, prompt);
    cachedResults.put(key, result);
});
```

### Result Caching
```java
// Cache results to prevent duplicate calls
private static final Map<String, String> cachedResults = new ConcurrentHashMap<>();

// In StarScript function
String cacheKey = "ollama_" + prompt;
if (cachedResults.containsKey(cacheKey)) {
    return Value.string(cachedResults.get(cacheKey));
}

// Spawn async fetch
MeteorExecutor.execute(() -> {
    String result = executor.execute(model, prompt);
    cachedResults.put(cacheKey, result);
});

return Value.string("Loading...");
```

### Connection Pooling
```java
// Reuse OllamaAPI instance (it's thread-safe)
public class OllamaClientManager {
    private static OllamaAPI cachedApi;
    private static String cachedHost;

    public static OllamaAPI getClient(String host) {
        if (cachedApi == null || !host.equals(cachedHost)) {
            cachedApi = new OllamaAPI(host);
            cachedHost = host;
        }
        return cachedApi;
    }
}
```

---

## Potential Issues & Solutions

### Issue 1: Ollama Not Running
**Solution:** Add connection check in GUI, show clear error message

### Issue 2: Model Not Installed
**Solution:** Detect ModelNotFoundException, suggest `ollama pull <model>`

### Issue 3: Slow Response Times
**Solution:** Use streaming, show "Thinking..." indicator

### Issue 4: Tool Calling Not Working
**Solution:** Validate model supports tools, show warning if not

### Issue 5: Memory Usage
**Solution:** Clear cached results periodically, limit cache size

---

## Recommended Models for Minecraft

| Use Case | Model | Size | Reason |
|----------|-------|------|--------|
| **Chat Commands** | llama3.1:8b | 4.7GB | Best balance speed/quality |
| **StarScript HUD** | qwen2.5:0.5b | 400MB | Fastest, minimal RAM |
| **MCP Tool Use** | llama3.1:8b | 4.7GB | Excellent tool calling |
| **Code Generation** | qwen2.5-coder:7b | 4.7GB | Specialized for code |
| **Creative Text** | mistral:7b | 4.1GB | Good storytelling |

---

## Next Steps

1. **Immediate:** Add Ollama4j dependency to build.gradle.kts
2. **Phase 1:** Implement OllamaConfig and basic executor
3. **Phase 2:** Add StarScript functions `{ollama()}` and `{ollama_mcp()}`
4. **Phase 3:** Add chat commands `.ollama` and `.ollama-mcp`
5. **Phase 4:** Add GUI settings screen
6. **Phase 5:** Write documentation and examples
7. **Future:** Consider unified LLMProvider abstraction

---

## References

- **Ollama4j GitHub:** https://github.com/ollama4j/ollama4j
- **Ollama4j Docs:** https://ollama4j.github.io/ollama4j/
- **Ollama API Docs:** https://docs.ollama.com/
- **Tool Calling Guide:** https://docs.ollama.com/capabilities/tool-calling
- **Model Library:** https://ollama.com/library

**See also:**
- `AI_DOCS/ollama/ollama-java-integration.md` - Comprehensive integration guide
- `AI_DOCS/ollama/ollama4j-api-reference.md` - Complete API reference

---

**Document Version:** 1.0
**Last Updated:** 2026-02-07
**Prepared For:** meteor-mcp-addon integration planning
