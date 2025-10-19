# Gemini API Integration - Implementation Spec

## Overview
Add Google Gemini API integration to enable AI-powered interactions with MCP tools through StarScript expressions in Minecraft. This will support both standalone Gemini calls and enhanced Gemini calls that can intelligently use connected MCP server tools.

---

## 1. Dependency Management

### 1.1 Add Gemini SDK to build.gradle.kts

**Location**: `build.gradle.kts` (lines 26-47)

**Changes Needed**:
```kotlin
dependencies {
    // ... existing dependencies ...

    // Google Gemini API
    val geminiVersion = properties["gemini_version"] as String
    modImplementation("com.google.genai:google-genai:${geminiVersion}")!!.let { include(it) }
}
```

**gradle.properties additions**:
```properties
gemini_version=1.23.0
```

**Why**: The Gemini Java SDK needs to be shaded into the final JAR just like the MCP SDK, ensuring all dependencies are bundled for Fabric runtime.

---

## 2. Configuration System Extensions

### 2.1 Create GeminiConfig Class

**New File**: `src/main/java/com/cope/meteormcp/systems/GeminiConfig.java`

**Purpose**: Store global Gemini API settings

**Fields**:
- `String apiKey` - Gemini API key (encrypted in NBT)
- `GeminiModel model` - Selected model (enum)
- `int maxOutputTokens` - Max AI output limit (default: 2048)
- `float temperature` - Generation temperature (default: 0.7)
- `boolean enabled` - Global toggle for Gemini features

**Enum**: `GeminiModel`
- `GEMINI_2_5_PRO("gemini-2.5-pro")`
- `GEMINI_2_5_FLASH("gemini-2.5-flash")`
- `GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite")`
- `GEMINI_FLASH_LATEST("gemini-flash-latest")`
- `GEMINI_FLASH_LITE_LATEST("gemini-flash-lite-latest")`

**Methods**:
- `isValid()` - Validate API key exists and model selected
- `toTag()` / `fromTag()` - NBT serialization
- `getModelId()` - Get string model ID for API calls

**Why**: Follows the existing pattern used in `MCPServerConfig.java` for consistent configuration management.

---

### 2.2 Extend MCPServers System

**File**: `src/main/java/com/cope/meteormcp/systems/MCPServers.java`

**Add Field**:
```java
private GeminiConfig geminiConfig = new GeminiConfig();
```

**Add Methods**:
```java
public GeminiConfig getGeminiConfig() { return geminiConfig; }
public void setGeminiConfig(GeminiConfig config) { this.geminiConfig = config; }
```

**Update Serialization**:
- `toTag()`: Add `tag.put("gemini", geminiConfig.toTag())`
- `fromTag()`: Load gemini config from NBT

**Why**: Centralize all configuration in the existing `MCPServers` system rather than creating a separate system.

---

## 3. Gemini Client Management

### 3.1 Create GeminiClientManager Class

**New File**: `src/main/java/com/cope/meteormcp/gemini/GeminiClientManager.java`

**Purpose**: Singleton manager for Gemini API client lifecycle

**Fields**:
- `Client geminiClient` - Cached Gemini client instance
- `GeminiConfig currentConfig` - Last used config for cache invalidation
- `ReentrantLock clientLock` - Thread-safe client access

**Methods**:
- `Client getClient()` - Get or create client (lazy initialization)
- `void invalidateClient()` - Force client recreation on config change
- `boolean isConfigured()` - Check if valid config exists
- `void shutdown()` - Cleanup on addon disable

**Client Creation Logic**:
```java
Client client = Client.builder()
    .apiKey(config.getApiKey())
    .build();
```

**Why**: Centralized client management prevents creating multiple clients and handles configuration changes cleanly.

---

### 3.2 Create GeminiExecutor Class

**New File**: `src/main/java/com/cope/meteormcp/gemini/GeminiExecutor.java`

**Purpose**: Execute Gemini API calls with error handling and result conversion

**Methods**:

**Simple Call (No MCP)**:
```java
public static String executeSimplePrompt(String prompt)
```
- Validates config
- Gets client from manager
- Calls `client.models.generateContent()`
- Returns `response.text()`
- Handles errors gracefully (returns error message)

**Enhanced Call (With MCP Tools)**:
```java
public static String executeWithMCPTools(String prompt, Set<String> mcpServerNames)
```
- Converts MCP tool declarations to Gemini `FunctionDeclaration` format
- Uses automatic function calling pattern from SDK
- Handles multi-turn function call loop
- Returns final response text

**Helper Methods**:
- `convertMCPToolToFunctionDeclaration(Tool mcpTool)` - Schema conversion
- `executeMCPTool(String serverName, String toolName, Map<String, Object> args)` - Route to MCPServerConnection
- `handleFunctionCallResponse(FunctionCall call)` - Execute and format results

**Error Handling**:
- Catch all exceptions
- Log errors via `MeteorMCPAddon.LOG`
- Return user-friendly error messages (never crash StarScript)

**Why**: Separates business logic from StarScript integration, enables testing, and provides clean error boundaries.

---

## 4. StarScript Integration

### 4.1 Create GeminiStarScriptIntegration Class

**New File**: `src/main/java/com/cope/meteormcp/starscript/GeminiStarScriptIntegration.java`

**Purpose**: Register Gemini functions in StarScript namespace

**Functions to Register**:

**1. `gemini(prompt)` - Simple Call**
```java
Value.function(arguments -> {
    if (!GeminiClientManager.getInstance().isConfigured()) {
        return Value.string("⚠ Gemini not configured");
    }

    String prompt = arguments.get(0).toString();
    String result = GeminiExecutor.executeSimplePrompt(prompt);
    return Value.string(result);
})
```

**Usage**: `{gemini("What is the capital of France?")}`

**2. `gemini_mcp(prompt)` - Enhanced Call with Auto-Detection**

This is the natural approach that leverages the existing MCP tool infrastructure:

```java
Value.function(arguments -> {
    if (!GeminiClientManager.getInstance().isConfigured()) {
        return Value.string("⚠ Gemini not configured");
    }

    String prompt = arguments.get(0).toString();

    // Automatically include ALL connected MCP servers
    Set<String> connectedServers = MCPServers.get().getAllConnections()
        .stream()
        .filter(MCPServerConnection::isConnected)
        .map(conn -> conn.getConfig().getName())
        .collect(Collectors.toSet());

    String result = GeminiExecutor.executeWithMCPTools(prompt, connectedServers);
    return Value.string(result);
})
```

**Usage Examples**:
```javascript
// Simple Gemini call (no MCP tools)
{gemini("What is 2+2?")}

// Gemini with ALL connected MCP tools available
{gemini_mcp("Check the weather in London and schedule a meeting")}

// User already has weather-server and calendar-server connected,
// so Gemini can automatically call weather-server.get_forecast()
// and calendar-server.schedule_meeting() as needed
```

**Why This Approach is Better**:
1. **No redundant server specification**: User already connected servers via GUI, no need to repeat names
2. **Natural usage**: Just like how `{server.tool()}` works - it's available because it's connected
3. **Zero boilerplate**: Single function call, Gemini decides which tools to use
4. **Consistent with MCP philosophy**: Tools are "available" when servers are connected
5. **Smart tool selection**: Gemini's intelligence chooses relevant tools from all available options

**Alternative Syntax** (if needed for advanced control):
```java
// Could add optional server filter later:
{gemini_mcp("Get weather", ["weather-server"])}  // Only use specific servers

// But default behavior is: use everything connected
{gemini_mcp("Get weather")}  // Gemini picks from all connected servers
```

**Registration Location**: `MeteorMCPAddon.onInitialize()`
```java
MeteorStarscript.ss.set("gemini", createGeminiFunction());
MeteorStarscript.ss.set("gemini_mcp", createGeminiMCPFunction());
```

**Why**: Follows the same pattern as `MCPToolExecutor.createToolFunction()` for consistency with existing MCP tool registration.

---

## 5. GUI Extensions

### 5.1 Extend MCPServersScreen

**File**: `src/main/java/com/cope/meteormcp/gui/screens/MCPServersScreen.java`

**Add Gemini Settings Section** (after line 34):

```java
// Gemini Settings Section
add(theme.horizontalSeparator()).expandX();
add(theme.label("Gemini API Settings")).expandX();

WButton geminiSettingsBtn = add(theme.button("Configure Gemini API")).expandX().widget();
geminiSettingsBtn.action = () -> mc.setScreen(new GeminiSettingsScreen(theme, this));

GeminiConfig geminiConfig = MCPServers.get().getGeminiConfig();
String status = geminiConfig.isValid() ? "✓ Configured" : "⚠ Not Configured";
add(theme.label("Status: " + status)).expandX();

add(theme.horizontalSeparator()).expandX();
```

**Why**: Adds entry point to Gemini settings without cluttering the main server list.

---

### 5.2 Create GeminiSettingsScreen

**New File**: `src/main/java/com/cope/meteormcp/gui/screens/GeminiSettingsScreen.java`

**Layout**:

1. **API Key Field** (password-style input)
   - `WTextBox` with masking enabled
   - Save to config on change

2. **Model Dropdown**
   - `WDropdown<GeminiModel>` with all enum values
   - Default: GEMINI_2_5_FLASH

3. **Max Output Tokens Slider**
   - `WIntEdit` range: 256-8192
   - Default: 2048

4. **Temperature Slider** (optional, advanced)
   - `WDoubleEdit` range: 0.0-2.0
   - Default: 0.7

5. **Enable/Disable Toggle**
   - `WCheckbox` for quick disable without losing config

6. **Action Buttons**
   - "Test Connection" - Make simple API call to validate
   - "Save" - Persist to NBT and close
   - "Cancel" - Discard changes

**Test Connection Logic**:
```java
try {
    // Temporarily create client with entered settings
    String testResult = GeminiExecutor.executeSimplePrompt("Say 'test successful'");
    // Show success message in GUI
} catch (Exception e) {
    // Show error message in GUI
}
```

**Why**: Provides user-friendly interface for configuration, matches existing screen patterns (`AddMCPServerScreen`, `EditMCPServerScreen`).

---

## 6. MCP-Gemini Function Calling Bridge

### 6.1 Create MCPToGeminiBridge Class

**New File**: `src/main/java/com/cope/meteormcp/gemini/MCPToGeminiBridge.java`

**Purpose**: Convert between MCP and Gemini function calling schemas

**Key Method**:
```java
public static FunctionDeclaration convertMCPToolToGemini(io.modelcontextprotocol.sdk.Tool mcpTool, String serverName)
```

**Conversion Logic**:
1. Extract MCP tool name, description
2. Prefix tool name with server namespace: `serverName_toolName` (e.g., `weather_get_forecast`)
3. Convert MCP `JsonSchema` to Gemini `Schema` format
4. Map parameter types:
   - `string` → `Type.STRING`
   - `integer/number` → `Type.NUMBER`
   - `boolean` → `Type.BOOLEAN`
   - `array` → `Type.ARRAY`
   - `object` → `Type.OBJECT`
5. Handle required parameters
6. Return Gemini `FunctionDeclaration`

**Why Prefix Tool Names?**
- Prevents name collisions between servers (multiple servers might have `get_data`)
- Enables routing back to correct server during execution
- Example: `weather_get_forecast` clearly maps to `weather` server

**Example**:
```java
// MCP Tool Schema (from weather server)
{
  "name": "get_forecast",
  "description": "Get weather forecast",
  "inputSchema": {
    "type": "object",
    "properties": {
      "location": {"type": "string"},
      "days": {"type": "integer"}
    },
    "required": ["location"]
  }
}

// Converts to Gemini FunctionDeclaration
FunctionDeclaration.newBuilder()
  .setName("weather_get_forecast")  // Prefixed with server name
  .setDescription("Get weather forecast (from weather server)")
  .setParameters(Schema.newBuilder()
    .setType(Type.OBJECT)
    .putProperties("location", Schema.newBuilder().setType(Type.STRING).build())
    .putProperties("days", Schema.newBuilder().setType(Type.NUMBER).build())
    .addRequired("location")
    .build())
  .build()
```

**Tool Routing Helper**:
```java
public static class ToolCallRoute {
    public final String serverName;
    public final String toolName;

    public static ToolCallRoute fromGeminiFunctionName(String geminiName) {
        // Parse "weather_get_forecast" → serverName="weather", toolName="get_forecast"
        String[] parts = geminiName.split("_", 2);
        return new ToolCallRoute(parts[0], parts[1]);
    }
}
```

**Why**: Enables Gemini to understand and call MCP tools by bridging the schema formats, with proper routing back to the source server.

---

### 6.2 Function Call Execution Flow

**In GeminiExecutor.executeWithMCPTools()**:

1. **Gather MCP Tools from Connected Servers**:
   ```java
   List<FunctionDeclaration> functionDeclarations = new ArrayList<>();

   for (String serverName : mcpServerNames) {
       MCPServerConnection conn = MCPServers.get().getConnection(serverName);
       if (conn != null && conn.isConnected()) {
           for (Tool tool : conn.listTools()) {
               FunctionDeclaration geminiFunc = MCPToGeminiBridge.convertMCPToolToGemini(tool, serverName);
               functionDeclarations.add(geminiFunc);
           }
       }
   }
   ```

2. **Create Function Implementations** (for Automatic Function Calling):
   ```java
   // Map function names to implementations
   Map<String, java.lang.reflect.Method> functionImpls = new HashMap<>();

   for (String serverName : mcpServerNames) {
       MCPServerConnection conn = MCPServers.get().getConnection(serverName);
       if (conn != null) {
           for (Tool tool : conn.listTools()) {
               String geminiName = serverName + "_" + tool.name();

               // Create wrapper method for this specific tool
               Method method = createMCPToolWrapper(serverName, tool.name());
               functionImpls.put(geminiName, method);
           }
       }
   }
   ```

3. **Configure Gemini Request with Tools**:
   ```java
   GenerateContentConfig config = GenerateContentConfig.builder()
       .tools(Tool.builder().functions(functionImpls.values().toArray(Method[]::new)))
       .maxOutputTokens(geminiConfig.getMaxOutputTokens())
       .temperature(geminiConfig.getTemperature())
       .build();
   ```

4. **Execute with Automatic Function Calling**:
   ```java
   GenerateContentResponse response = client.models.generateContent(
       geminiConfig.getModelId(),
       prompt,
       config
   );
   ```

5. **SDK Handles**:
   - Function call detection
   - Execution routing (calls our wrapper methods)
   - Multi-turn conversation
   - Final response generation

6. **Return Final Text**:
   ```java
   return response.text();
   ```

**MCP Tool Wrapper Creation**:
```java
private static Method createMCPToolWrapper(String serverName, String toolName) {
    // Dynamically create a method that routes to MCPServerConnection.callTool()
    // This is complex - alternative approach: use Manual Function Calling instead of AFC
}
```

**Alternative: Manual Function Calling** (Simpler Implementation):

Instead of using Automatic Function Calling (AFC) which requires Java reflection, use manual function call handling:

```java
// 1. Send prompt with function declarations
GenerateContentResponse response = client.models.generateContent(
    geminiConfig.getModelId(),
    prompt,
    config
);

// 2. Check if model wants to call functions
while (response.candidates[0].content.parts[0].functionCall != null) {
    FunctionCall call = response.candidates[0].content.parts[0].functionCall;

    // 3. Route to MCP server
    ToolCallRoute route = ToolCallRoute.fromGeminiFunctionName(call.name);
    MCPServerConnection conn = MCPServers.get().getConnection(route.serverName);
    CallToolResult mcpResult = conn.callTool(route.toolName, call.args);

    // 4. Send result back to Gemini
    Content functionResponse = Content.fromParts(
        Part.fromFunctionResponse(call.name, mcpResult)
    );

    // 5. Continue conversation
    response = client.models.generateContent(
        geminiConfig.getModelId(),
        List.of(previousContent, functionResponse),
        config
    );
}

// 6. Return final text response
return response.text();
```

**Recommended Approach**: Manual Function Calling (simpler, more control, easier debugging)

**Why**: Enables seamless integration between Gemini's intelligence and MCP's tool ecosystem.

---

## 7. Error Handling & Edge Cases

### 7.1 Configuration Validation

**When**:
- User enters invalid API key
- Selects incompatible model
- Network unavailable

**Handling**:
- Validate API key format (not empty, reasonable length)
- Test connection button shows detailed errors
- StarScript functions return error strings instead of crashing

**Example**:
```java
if (!geminiConfig.isValid()) {
    return Value.string("⚠ Gemini not configured. Open Meteor GUI → MCP → Configure Gemini API");
}
```

---

### 7.2 API Rate Limiting

**Strategy**:
- No built-in rate limiting (rely on Gemini API's own limits)
- Log API errors for debugging
- Return user-friendly messages on rate limit errors

---

### 7.3 MCP Tool Execution Failures

**Scenarios**:
- MCP server disconnected mid-call
- Tool execution throws exception
- Invalid parameters from Gemini

**Handling**:
```java
try {
    CallToolResult result = connection.callTool(toolName, arguments);
    return formatResultForGemini(result);
} catch (Exception e) {
    LOG.error("MCP tool execution failed: {}", e.getMessage());
    return Map.of("error", e.getMessage());
}
```

**Why**: Ensures robust operation even when MCP servers misbehave.

---

### 7.4 StarScript Thread Safety

**Issue**: StarScript expressions can be evaluated from render thread

**Solution**:
- All Gemini API calls block on main thread (acceptable for StarScript)
- Simple synchronous execution (no async complexity needed initially)

**Why**: Initial implementation can block (StarScript is typically used for non-critical display), optimize later if needed.

---

## 8. File Structure Summary

### New Files
```
src/main/java/com/cope/meteormcp/
├── systems/
│   └── GeminiConfig.java (NEW)
├── gemini/ (NEW PACKAGE)
│   ├── GeminiClientManager.java
│   ├── GeminiExecutor.java
│   └── MCPToGeminiBridge.java
├── starscript/
│   └── GeminiStarScriptIntegration.java (NEW)
└── gui/screens/
    └── GeminiSettingsScreen.java (NEW)
```

### Modified Files
```
build.gradle.kts - Add Gemini dependency
gradle.properties - Add gemini_version
src/main/java/com/cope/meteormcp/
├── MeteorMCPAddon.java - Register Gemini StarScript functions
├── systems/MCPServers.java - Add GeminiConfig field + serialization
└── gui/screens/MCPServersScreen.java - Add Gemini settings button
```

---

## 9. Implementation Order

**Recommended sequence** (each step builds on previous):

1. **Dependency Setup**
   - Add to build.gradle.kts
   - Add to gradle.properties
   - Test build

2. **Configuration System**
   - Create GeminiConfig
   - Extend MCPServers serialization
   - Test save/load

3. **Client Management**
   - Create GeminiClientManager
   - Test client initialization

4. **Simple Execution**
   - Create GeminiExecutor (simple prompt only)
   - Test API calls outside GUI

5. **StarScript Integration**
   - Create GeminiStarScriptIntegration
   - Register `gemini()` function
   - Test in HUD element

6. **GUI - Settings Screen**
   - Create GeminiSettingsScreen
   - Implement all fields and validation
   - Add test connection button

7. **GUI - Main Screen Extension**
   - Update MCPServersScreen
   - Add settings button and status

8. **MCP Bridge**
   - Create MCPToGeminiBridge
   - Implement schema conversion
   - Test with sample MCP tools

9. **Enhanced Execution**
   - Extend GeminiExecutor for MCP tools
   - Implement manual function calling loop
   - Register `gemini_mcp()` function

10. **Testing & Refinement**
    - Run through manual test checklist
    - Fix bugs
    - Improve error messages

---

## 10. Usage Examples

### Simple Gemini Calls
```javascript
// Basic text generation
{gemini("What is the capital of France?")}

// Math/reasoning
{gemini("Calculate the factorial of 5")}

// Creative tasks
{gemini("Write a haiku about Minecraft")}
```

### MCP-Enhanced Calls

**Scenario 1: Weather Query**
```javascript
// User has weather-server connected with get_forecast tool
{gemini_mcp("What's the weather like in Tokyo?")}

// Gemini automatically:
// 1. Identifies weather-server.get_forecast is relevant
// 2. Calls weather_get_forecast(location="Tokyo")
// 3. Formats result: "The weather in Tokyo is sunny, 25°C"
```

**Scenario 2: Multi-Tool Workflow**
```javascript
// User has weather-server and calendar-server connected
{gemini_mcp("If it's sunny in London tomorrow, schedule a picnic at 2pm")}

// Gemini automatically:
// 1. Calls weather_get_forecast(location="London", days=1)
// 2. Evaluates condition (is it sunny?)
// 3. If yes, calls calendar_schedule_meeting(time="14:00", topic="Picnic")
// 4. Returns: "I've scheduled a picnic for 2pm tomorrow - weather looks perfect!"
```

**Scenario 3: Data Analysis**
```javascript
// User has database-server connected with query_sales tool
{gemini_mcp("What were our top 3 selling products last month?")}

// Gemini automatically:
// 1. Calls database_query_sales(period="last_month")
// 2. Analyzes returned data
// 3. Formats response: "Your top sellers were: 1) Widget X (500 units)..."
```

---

## 11. Key Design Decisions

### Why Auto-Include All Connected Servers?
- **Consistency**: Servers are already "active" because user connected them
- **Simplicity**: Zero boilerplate in StarScript expressions
- **Intelligence**: Gemini decides which tools are relevant, not the user
- **Flexibility**: User can disconnect servers they don't want available

### Why Manual Function Calling Instead of AFC?
- **Simpler**: No complex Java reflection wrapper generation
- **More Control**: Full visibility into function call loop
- **Better Debugging**: Easy to log each function call step
- **MCP Compatibility**: Direct mapping to MCP tool execution

### Why Two Functions (`gemini` vs `gemini_mcp`)?
- **Performance**: Simple calls skip tool declaration overhead
- **Clarity**: User explicitly chooses enhanced vs simple mode
- **Debugging**: Easy to trace which execution path is used
- **Flexibility**: Can use Gemini without any MCP servers

### Why Prefix Tool Names with Server?
- **Collision Prevention**: Multiple servers can have identically named tools
- **Routing**: Enables finding source server during execution
- **Clarity**: User sees which server provided the tool (in logs/debug)

---

## 12. Security Considerations

### API Key Storage
- **Current**: Plain text in NBT (matches MCP server configs)
- **Acceptable**: Local single-player environment

### Prompt Injection
- **Risk**: User-controlled StarScript → Gemini prompts
- **Mitigation**: None in initial version (trusted local environment)

### Rate Limiting
- **Current**: None (rely on Gemini API limits)

---

## 13. Documentation Updates Needed

**CLAUDE.md** - Add section:
```markdown
## Gemini API Integration

### Configuration
1. Open Meteor GUI (Right Shift)
2. Navigate to MCP tab
3. Click "Configure Gemini API"
4. Enter API key, select model
5. Click "Test Connection" then "Save"

### StarScript Usage

#### Simple Calls
{gemini("What is the weather like?")}

#### MCP-Enhanced Calls
// Automatically uses ALL connected MCP servers
{gemini_mcp("Check weather in London and schedule a meeting if it's sunny")}

### How It Works
- `gemini()`: Direct LLM call, no tools
- `gemini_mcp()`: Gemini can call ANY tool from connected MCP servers
- Gemini intelligently chooses which tools to use based on your prompt

### Architecture
- GeminiClientManager: Singleton client lifecycle
- GeminiExecutor: API call execution + manual function calling loop
- MCPToGeminiBridge: Schema conversion (MCP → Gemini)
- Manual Function Calling: Full control over multi-turn tool execution
```

**README.md** - Add to features list:
```markdown
- ✅ Gemini API integration for AI-powered prompts
- ✅ Automatic MCP tool discovery for connected servers
- ✅ Intelligent function calling (Gemini picks relevant tools)
- ✅ Simple and enhanced modes via StarScript
```

---

## Summary

This spec provides a complete blueprint for adding Gemini API integration to your Meteor MCP addon. The implementation:

✅ **Follows existing patterns** (MCPServerConfig, MCPServers system, Meteor GUI)
✅ **Uses official Google GenAI Java SDK** (v1.23.0)
✅ **Natural usage**: `gemini_mcp()` auto-includes all connected servers
✅ **Smart tool selection**: Gemini picks relevant tools from available pool
✅ **Clean StarScript API**: Two functions for simple/enhanced modes
✅ **Manual function calling**: Full control and easier debugging
✅ **Comprehensive GUI**: Settings screen with test connection
✅ **Robust error handling**: Never crashes StarScript
✅ **Ready for Gradle/Fabric**: Proper dependency shading

**Key Innovation**: Unlike manually specifying server names, `gemini_mcp()` leverages the fact that users already connected their desired servers via the GUI. The connected state = available to Gemini. This matches the MCP philosophy perfectly.
