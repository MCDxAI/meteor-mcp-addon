# Command Integration - Implementation Spec (Phase 3)

## Overview

Add Minecraft chat command support for direct MCP tool invocation and Gemini AI interactions. This phase builds on the completed Gemini integration (Phase 2) and provides users with command-line access to:

1. **Dynamic MCP Tool Commands**: Invoke any connected MCP server's tools via `.server:tool args`
2. **Gemini Simple Command**: Quick AI queries via `.gemini "prompt"`
3. **Gemini MCP Command**: AI queries with automatic MCP tool access via `.gemini-mcp "prompt"`

**Design Philosophy**: Commands provide an alternative to StarScript expressions, offering:
- Direct tool execution without StarScript syntax
- Chat-based interaction (familiar to Minecraft players)
- Real-time feedback in chat with formatted results
- Dynamic registration/unregistration as servers connect/disconnect

---

## 1. Command Syntax Design

### 1.1 MCP Tool Commands

**Pattern**: `.server_name:tool_name [arguments]`

**Rationale**:
- **Colon separator**: Clear namespace delimiter (inspired by Minecraft's `namespace:id` pattern)
- **Dynamic registration**: Commands auto-register when MCP servers connect
- **No prefix conflicts**: Each server gets its own command namespace

**Examples**:

```bash
# Simple tool with no arguments
/weather:get_current

# Tool with positional arguments
/weather:get_forecast "London" 3

# Tool with named arguments (JSON-style)
/weather:get_forecast location="London" days=3

# Tool with complex JSON argument
/database:query '{"table": "users", "filter": {"active": true}}'
```

**Argument Parsing Strategy**:
1. **Positional arguments**: Map to parameter order from MCP tool schema
2. **Named arguments**: Parse `key=value` pairs, match to parameter names
3. **JSON literals**: Detect quoted JSON objects/arrays, parse directly
4. **Type coercion**: String → Number/Boolean based on schema

**Edge Cases**:
- Multiple servers with same tool name: Each server has unique command (`/weather:get_forecast` vs `/backup-weather:get_forecast`)
- Spaces in arguments: Require quotes (`"New York"`)
- Special characters: Standard Brigadier escaping

---

### 1.2 Gemini Simple Command

**Pattern**: `.gemini "prompt"`

**Purpose**: Quick AI queries without MCP tool access

**Examples**:

```bash
.gemini "What is the capital of France?"
.gemini "Explain quantum computing in simple terms"
.gemini "Write a haiku about Minecraft"
```

**Behavior**:
- Executes `GeminiExecutor.executeSimplePrompt(prompt)`
- Returns response directly to chat
- No MCP tool declarations sent to Gemini
- Fast, low-latency responses

**Chat Output Format**:
```
[Gemini] Paris is the capital of France.
```

---

### 1.3 Gemini MCP Command

**Pattern**: `.gemini-mcp "prompt"`

**Purpose**: AI queries with automatic access to ALL connected MCP server tools

**Examples**:

```bash
# Single tool usage
.gemini-mcp "What's the weather in Tokyo?"
# → Gemini calls weather:get_forecast(location="Tokyo")
# → Returns formatted weather data

# Multi-tool workflow
.gemini-mcp "If it's sunny in London tomorrow, schedule a picnic at 2pm"
# → Gemini calls weather:get_forecast(location="London", days=1)
# → Evaluates condition
# → Calls calendar:schedule_meeting(time="14:00", topic="Picnic")
# → Returns confirmation message

# Data analysis
.gemini-mcp "Summarize our sales data from last month"
# → Gemini calls database:query_sales(period="last_month")
# → Analyzes results
# → Returns formatted summary
```

**Behavior**:
- Executes `GeminiExecutor.executeWithMCPTools(prompt, allConnectedServers)`
- Automatically includes ALL connected MCP servers (same as `{gemini_mcp()}` StarScript)
- Gemini intelligently selects relevant tools
- Returns final response to chat

**Chat Output Format**:
```
[Gemini MCP] The weather in Tokyo is sunny, 25°C with light winds.
[Tools Used] weather:get_forecast
```

---

## 2. Technical Architecture

### 2.1 Command Class Structure

**New Package**: `src/main/java/com/cope/meteormcp/commands/`

```
commands/
├── MCPToolCommand.java         - Dynamic command for single MCP tool
├── GeminiCommand.java          - Simple Gemini query command
├── GeminiMCPCommand.java       - Gemini with MCP tools command
└── CommandUtils.java           - Shared utilities (argument parsing, formatting)
```

---

### 2.2 MCPToolCommand Implementation

**File**: `src/main/java/com/cope/meteormcp/commands/MCPToolCommand.java`

**Purpose**: Dynamically created command for each MCP tool when server connects

**Constructor Pattern**:
```java
public class MCPToolCommand extends Command {
    private final String serverName;
    private final String toolName;
    private final MCPServerConnection connection;
    private final Tool toolSchema;

    public MCPToolCommand(String serverName, String toolName,
                          MCPServerConnection connection, Tool toolSchema) {
        super(
            serverName + ":" + toolName,  // Command name: "weather:get_forecast"
            toolSchema.description().orElse("MCP Tool: " + toolName)
        );
        this.serverName = serverName;
        this.toolName = toolName;
        this.connection = connection;
        this.toolSchema = toolSchema;
    }
}
```

**Build Method**:
```java
@Override
public void build(LiteralArgumentBuilder<CommandSource> builder) {
    // Greedy string argument to capture all remaining input
    builder.then(argument("args", StringArgumentType.greedyString())
        .executes(context -> {
            String argsString = context.getArgument("args", String.class);
            return executeToolCommand(argsString);
        })
    );

    // Also support no arguments
    builder.executes(context -> executeToolCommand(""));
}
```

**Execution Logic**:
```java
private int executeToolCommand(String argsString) {
    try {
        // 1. Parse arguments based on tool schema
        Map<String, Object> arguments = CommandUtils.parseArguments(
            argsString,
            toolSchema.inputSchema()
        );

        // 2. Validate required parameters
        if (!CommandUtils.validateRequiredParams(arguments, toolSchema)) {
            error("Missing required parameters. Usage: " +
                  CommandUtils.generateUsage(toolSchema));
            return 0;
        }

        // 3. Execute MCP tool
        CallToolResult result = connection.callTool(toolName, arguments);

        // 4. Format and display result
        CommandUtils.displayToolResult(this, result);

        return SINGLE_SUCCESS;

    } catch (Exception e) {
        error("Tool execution failed: " + e.getMessage());
        MeteorMCPAddon.LOG.error("MCP tool command failed", e);
        return 0;
    }
}
```

**Key Methods**:
- `parseArguments()`: Convert string args to JSON map
- `validateRequiredParams()`: Check all required params present
- `displayToolResult()`: Format CallToolResult for chat display

---

### 2.3 GeminiCommand Implementation

**File**: `src/main/java/com/cope/meteormcp/commands/GeminiCommand.java`

**Purpose**: Simple Gemini query without MCP tools

**Implementation**:
```java
public class GeminiCommand extends Command {
    public GeminiCommand() {
        super("gemini", "Query Gemini AI without MCP tools");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("prompt", StringArgumentType.greedyString())
            .executes(context -> {
                String prompt = context.getArgument("prompt", String.class);
                return executeGemini(prompt);
            })
        );
    }

    private int executeGemini(String prompt) {
        // Check if Gemini is configured
        if (!GeminiClientManager.getInstance().isConfigured()) {
            error("Gemini is not configured. Open Meteor GUI → MCP → Configure Gemini API");
            return 0;
        }

        // Execute async to prevent blocking chat input
        MeteorExecutor.execute(() -> {
            try {
                info("Querying Gemini...");
                String response = GeminiExecutor.executeSimplePrompt(prompt);

                // Format response in chat
                info(response);

            } catch (Exception e) {
                error("Gemini query failed: " + e.getMessage());
                MeteorMCPAddon.LOG.error("Gemini command failed", e);
            }
        });

        return SINGLE_SUCCESS;
    }
}
```

**Key Features**:
- **Async execution**: Uses `MeteorExecutor` to prevent UI blocking
- **Progress feedback**: Shows "Querying Gemini..." before response
- **Error handling**: Graceful failure with user-friendly messages

---

### 2.4 GeminiMCPCommand Implementation

**File**: `src/main/java/com/cope/meteormcp/commands/GeminiMCPCommand.java`

**Purpose**: Gemini query with automatic MCP tool access

**Implementation**:
```java
public class GeminiMCPCommand extends Command {
    public GeminiMCPCommand() {
        super("gemini-mcp", "Query Gemini AI with access to all connected MCP tools");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("prompt", StringArgumentType.greedyString())
            .executes(context -> {
                String prompt = context.getArgument("prompt", String.class);
                return executeGeminiMCP(prompt);
            })
        );
    }

    private int executeGeminiMCP(String prompt) {
        // Check if Gemini is configured
        if (!GeminiClientManager.getInstance().isConfigured()) {
            error("Gemini is not configured. Open Meteor GUI → MCP → Configure Gemini API");
            return 0;
        }

        // Get all connected servers
        Set<String> connectedServers = MCPServers.get().getAllConnections()
            .stream()
            .filter(MCPServerConnection::isConnected)
            .map(conn -> conn.getConfig().getName())
            .collect(Collectors.toSet());

        if (connectedServers.isEmpty()) {
            warning("No MCP servers connected. Using simple Gemini mode.");
            // Fall back to simple mode
            return new GeminiCommand().executeGemini(prompt);
        }

        // Execute async
        MeteorExecutor.execute(() -> {
            try {
                info("Querying Gemini with " + connectedServers.size() + " MCP servers...");

                String response = GeminiExecutor.executeWithMCPTools(prompt, connectedServers);

                // Format response with tool usage info
                info(response);

                // Optionally show which tools were used
                // This would require extending GeminiExecutor to track tool calls

            } catch (Exception e) {
                error("Gemini MCP query failed: " + e.getMessage());
                MeteorMCPAddon.LOG.error("Gemini MCP command failed", e);
            }
        });

        return SINGLE_SUCCESS;
    }
}
```

**Enhanced Features**:
- **Auto-detection**: Finds all connected MCP servers
- **Fallback**: Uses simple mode if no servers connected
- **Progress info**: Shows server count before execution
- **Tool tracking**: Could log which tools Gemini called (optional enhancement)

---

### 2.5 CommandUtils Helper Class

**File**: `src/main/java/com/cope/meteormcp/commands/CommandUtils.java`

**Purpose**: Shared utilities for argument parsing and result formatting

**Key Methods**:

#### Argument Parsing

```java
public class CommandUtils {

    /**
     * Parse command arguments into JSON map based on tool schema.
     * Supports:
     * - Positional args: "London" 3
     * - Named args: location="London" days=3
     * - JSON literals: '{"key": "value"}'
     */
    public static Map<String, Object> parseArguments(String argsString, JsonNode schema) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (argsString == null || argsString.isBlank()) {
            return result;
        }

        // Try to parse as pure JSON first
        if (argsString.trim().startsWith("{") || argsString.trim().startsWith("[")) {
            try {
                return parseJsonArguments(argsString);
            } catch (JsonProcessingException e) {
                // Fall through to other parsing methods
            }
        }

        // Split on spaces, respecting quotes
        List<String> tokens = tokenizeArguments(argsString);

        // Check if using named arguments (contains '=')
        boolean isNamed = tokens.stream().anyMatch(t -> t.contains("="));

        if (isNamed) {
            return parseNamedArguments(tokens, schema);
        } else {
            return parsePositionalArguments(tokens, schema);
        }
    }

    /**
     * Tokenize argument string, respecting quotes and escapes.
     */
    private static List<String> tokenizeArguments(String argsString) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;

        for (char c : argsString.toCharArray()) {
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    /**
     * Parse named arguments (key=value pairs).
     */
    private static Map<String, Object> parseNamedArguments(
            List<String> tokens, JsonNode schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode properties = schema.get("properties");

        for (String token : tokens) {
            int eqIndex = token.indexOf('=');
            if (eqIndex < 0) continue;

            String key = token.substring(0, eqIndex);
            String value = token.substring(eqIndex + 1);

            // Coerce type based on schema
            Object coerced = coerceValue(value, properties.get(key));
            result.put(key, coerced);
        }

        return result;
    }

    /**
     * Parse positional arguments, mapping to schema parameter order.
     */
    private static Map<String, Object> parsePositionalArguments(
            List<String> tokens, JsonNode schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode properties = schema.get("properties");

        if (properties == null || !properties.isObject()) {
            return result;
        }

        // Get parameter names in schema order (JSON object iteration order)
        Iterator<String> paramNames = properties.fieldNames();
        int index = 0;

        while (paramNames.hasNext() && index < tokens.size()) {
            String paramName = paramNames.next();
            String value = tokens.get(index);

            Object coerced = coerceValue(value, properties.get(paramName));
            result.put(paramName, coerced);
            index++;
        }

        return result;
    }

    /**
     * Coerce string value to appropriate type based on schema.
     */
    private static Object coerceValue(String value, JsonNode paramSchema) {
        if (paramSchema == null) return value;

        String type = paramSchema.get("type").asText("string");

        return switch (type) {
            case "integer" -> Integer.parseInt(value);
            case "number" -> Double.parseDouble(value);
            case "boolean" -> Boolean.parseBoolean(value);
            case "array", "object" -> {
                // Try to parse as JSON
                try {
                    yield new ObjectMapper().readValue(value, Object.class);
                } catch (JsonProcessingException e) {
                    yield value; // Fall back to string
                }
            }
            default -> value; // string
        };
    }

    /**
     * Validate all required parameters are present.
     */
    public static boolean validateRequiredParams(
            Map<String, Object> arguments, Tool toolSchema) {
        JsonNode schema = toolSchema.inputSchema();
        JsonNode required = schema.get("required");

        if (required == null || !required.isArray()) {
            return true; // No required params
        }

        for (JsonNode reqParam : required) {
            String paramName = reqParam.asText();
            if (!arguments.containsKey(paramName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Generate usage string from tool schema.
     */
    public static String generateUsage(Tool toolSchema) {
        StringBuilder usage = new StringBuilder();
        JsonNode schema = toolSchema.inputSchema();
        JsonNode properties = schema.get("properties");
        JsonNode required = schema.get("required");

        if (properties == null || !properties.isObject()) {
            return "<no arguments>";
        }

        Set<String> requiredParams = new HashSet<>();
        if (required != null && required.isArray()) {
            required.forEach(r -> requiredParams.add(r.asText()));
        }

        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String paramName = field.getKey();
            String type = field.getValue().get("type").asText("string");

            if (requiredParams.contains(paramName)) {
                usage.append("<").append(paramName).append(":").append(type).append("> ");
            } else {
                usage.append("[").append(paramName).append(":").append(type).append("] ");
            }
        }

        return usage.toString().trim();
    }
}
```

#### Result Formatting

```java
/**
 * Format and display MCP tool result in chat.
 */
public static void displayToolResult(Command command, CallToolResult result) {
    if (result.isError()) {
        command.error("Tool Error: " + extractErrorMessage(result));
        return;
    }

    List<McpSchema.Content> contents = result.content();
    if (contents.isEmpty()) {
        command.info("Tool executed successfully (no output)");
        return;
    }

    for (McpSchema.Content content : contents) {
        displayContent(command, content);
    }
}

/**
 * Display individual content item.
 */
private static void displayContent(Command command, McpSchema.Content content) {
    if (content instanceof McpSchema.TextContent textContent) {
        // Multi-line text: split and format
        String text = textContent.text();
        String[] lines = text.split("\n");

        for (String line : lines) {
            command.info(line);
        }
    }
    else if (content instanceof McpSchema.ImageContent imageContent) {
        command.info("[Image: " + imageContent.mimeType().orElse("unknown") + "]");
        command.info("Data: " + imageContent.data().substring(0, 50) + "...");
    }
    else if (content instanceof McpSchema.ResourceContents resourceContent) {
        command.info("[Resource: " + resourceContent.uri() + "]");
        command.info(resourceContent.text().orElse("[Binary data]"));
    }
}

/**
 * Extract error message from CallToolResult.
 */
private static String extractErrorMessage(CallToolResult result) {
    if (!result.content().isEmpty()) {
        McpSchema.Content firstContent = result.content().get(0);
        if (firstContent instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
    }
    return "Unknown error";
}
```

---

## 3. Command Registration System

### 3.1 Dynamic Registration on Server Connect

**Location**: `src/main/java/com/cope/meteormcp/systems/MCPServers.java`

**Add Method**: `registerCommandsForServer(String serverName)`

```java
/**
 * Register commands for all tools in a connected MCP server.
 * Called automatically when server connects.
 */
public void registerCommandsForServer(String serverName) {
    MCPServerConnection connection = connections.get(serverName);
    if (connection == null || !connection.isConnected()) {
        MeteorMCPAddon.LOG.warn("Cannot register commands for disconnected server: {}", serverName);
        return;
    }

    try {
        List<Tool> tools = connection.getTools();
        int registered = 0;

        for (Tool tool : tools) {
            MCPToolCommand command = new MCPToolCommand(
                serverName,
                tool.name(),
                connection,
                tool
            );

            Commands.add(command);
            registered++;
        }

        MeteorMCPAddon.LOG.info("Registered {} commands for MCP server '{}'",
                                registered, serverName);

    } catch (Exception e) {
        MeteorMCPAddon.LOG.error("Failed to register commands for server '{}': {}",
                                serverName, e.getMessage());
    }
}
```

**Update Connect Method**: Call registration after successful connection

```java
public boolean connect(String name) {
    // ... existing connection logic ...

    if (connection.connect()) {
        connections.put(name, connection);
        MeteorMCPAddon.registerServerToStarScript(name, connection);

        // NEW: Register commands
        registerCommandsForServer(name);

        return true;
    }

    return false;
}
```

---

### 3.2 Dynamic Unregistration on Server Disconnect

**Add Method**: `unregisterCommandsForServer(String serverName)`

```java
/**
 * Unregister all commands for an MCP server.
 * Called automatically when server disconnects.
 */
public void unregisterCommandsForServer(String serverName) {
    try {
        // Find and remove all commands matching server namespace
        List<Command> toRemove = Commands.COMMANDS.stream()
            .filter(cmd -> cmd.getName().startsWith(serverName + ":"))
            .collect(Collectors.toList());

        for (Command command : toRemove) {
            Commands.COMMANDS.remove(command);
        }

        MeteorMCPAddon.LOG.info("Unregistered {} commands for MCP server '{}'",
                                toRemove.size(), serverName);

    } catch (Exception e) {
        MeteorMCPAddon.LOG.error("Failed to unregister commands for server '{}': {}",
                                serverName, e.getMessage());
    }
}
```

**Update Disconnect Method**:

```java
public boolean disconnect(String name) {
    MCPServerConnection connection = connections.get(name);
    if (connection == null) return false;

    try {
        connection.disconnect();
        connections.remove(name);
        MeteorMCPAddon.unregisterServerFromStarScript(name);

        // NEW: Unregister commands
        unregisterCommandsForServer(name);

        return true;
    } catch (Exception e) {
        LOG.error("Error disconnecting from server {}: {}", name, e.getMessage());
        return false;
    }
}
```

---

### 3.3 Static Gemini Command Registration

**Location**: `src/main/java/com/cope/meteormcp/MeteorMCPAddon.java`

**Update `onInitialize()` Method**:

```java
@Override
public void onInitialize() {
    LOG.info("Initializing Meteor MCP Addon");

    // Initialize MCPServers system (loads saved configurations)
    Systems.add(new MCPServers());
    LOG.info("MCPServers system initialized");

    // Register MCP tab in Meteor GUI
    Tabs.add(new MCPTab());
    LOG.info("MCP tab registered");

    // Register Gemini StarScript functions
    GeminiStarScriptIntegration.register();
    LOG.info("Gemini StarScript functions registered");

    // NEW: Register static Gemini commands
    Commands.add(new GeminiCommand());
    Commands.add(new GeminiMCPCommand());
    LOG.info("Gemini commands registered");

    // Connect to auto-connect servers (will auto-register their commands)
    MCPServers.get().connectAutoConnect();

    LOG.info("Meteor MCP Addon initialized successfully");
}
```

---

## 4. Enhanced Features

### 4.1 Command Help System

**Enhancement**: Add help subcommand for MCP tool commands

**Pattern**: `.server:tool help`

**Implementation** (in `MCPToolCommand.build()`):

```java
@Override
public void build(LiteralArgumentBuilder<CommandSource> builder) {
    // Help subcommand
    builder.then(literal("help").executes(context -> {
        showHelp();
        return SINGLE_SUCCESS;
    }));

    // Regular execution
    builder.then(argument("args", StringArgumentType.greedyString())
        .executes(context -> {
            String argsString = context.getArgument("args", String.class);
            return executeToolCommand(argsString);
        })
    );

    builder.executes(context -> executeToolCommand(""));
}

private void showHelp() {
    info("§6" + getName() + "§r - " + getDescription());
    info("§eUsage:§r /" + getName() + " " + CommandUtils.generateUsage(toolSchema));

    // Show parameter details
    JsonNode properties = toolSchema.inputSchema().get("properties");
    if (properties != null && properties.isObject()) {
        info("§eParameters:§r");
        properties.fields().forEachRemaining(field -> {
            String paramName = field.getKey();
            JsonNode paramSchema = field.getValue();
            String type = paramSchema.get("type").asText("string");
            String description = paramSchema.get("description").asText("No description");

            info("  §b" + paramName + "§r (§7" + type + "§r): " + description);
        });
    }
}
```

---

### 4.2 Tool Call History Tracking

**Enhancement**: Track which MCP tools Gemini called during `gemini-mcp` execution

**Implementation**: Extend `GeminiExecutor.executeWithMCPTools()` to return metadata

```java
public static class GeminiMCPResult {
    public final String response;
    public final List<ToolCallInfo> toolCalls;

    public static class ToolCallInfo {
        public final String serverName;
        public final String toolName;
        public final Map<String, Object> arguments;
        public final long executionTimeMs;
    }
}
```

**Display in Command**:

```java
private int executeGeminiMCP(String prompt) {
    // ... async execution ...

    GeminiMCPResult result = GeminiExecutor.executeWithMCPToolsDetailed(prompt, connectedServers);

    info(result.response);

    if (!result.toolCalls.isEmpty()) {
        info("§7Tools used:§r");
        for (ToolCallInfo call : result.toolCalls) {
            info("  §b" + call.serverName + ":" + call.toolName +
                 "§r (§7" + call.executionTimeMs + "ms§r)");
        }
    }
}
```

---

### 4.3 Command Autocomplete

**Enhancement**: Add Brigadier suggestion providers for better UX

**Location**: `MCPToolCommand.build()`

**Implementation**:

```java
@Override
public void build(LiteralArgumentBuilder<CommandSource> builder) {
    builder.then(argument("args", StringArgumentType.greedyString())
        .suggests((context, suggestionsBuilder) -> {
            // Suggest parameter names
            JsonNode properties = toolSchema.inputSchema().get("properties");
            if (properties != null && properties.isObject()) {
                properties.fieldNames().forEachRemaining(paramName -> {
                    suggestionsBuilder.suggest(paramName + "=");
                });
            }
            return suggestionsBuilder.buildFuture();
        })
        .executes(context -> {
            String argsString = context.getArgument("args", String.class);
            return executeToolCommand(argsString);
        })
    );
}
```

---

## 5. Error Handling & Edge Cases

### 5.1 Server Disconnection During Command Execution

**Scenario**: User runs `/weather:get_forecast "London"` but server disconnects mid-execution

**Handling**:

```java
private int executeToolCommand(String argsString) {
    try {
        // Check connection status before execution
        if (!connection.isConnected()) {
            error("Server '" + serverName + "' is not connected");
            return 0;
        }

        // ... execute tool ...

    } catch (ConnectionException e) {
        error("Lost connection to server '" + serverName + "'");
        return 0;
    }
}
```

---

### 5.2 Invalid Argument Types

**Scenario**: User provides `days="abc"` when schema expects integer

**Handling**: Catch `NumberFormatException` in `CommandUtils.coerceValue()`

```java
private static Object coerceValue(String value, JsonNode paramSchema) {
    String type = paramSchema.get("type").asText("string");

    try {
        return switch (type) {
            case "integer" -> Integer.parseInt(value);
            case "number" -> Double.parseDouble(value);
            case "boolean" -> Boolean.parseBoolean(value);
            default -> value;
        };
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid value for " + type + ": " + value
        );
    }
}
```

---

### 5.3 Gemini API Rate Limiting

**Scenario**: User spams `.gemini` commands, hits API rate limit

**Handling**: Implement client-side cooldown

```java
public class GeminiCommand extends Command {
    private static final Map<UUID, Long> lastCallTime = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000; // 1 second

    private int executeGemini(String prompt) {
        UUID playerId = mc.player.getUuid();
        long now = System.currentTimeMillis();
        Long lastCall = lastCallTime.get(playerId);

        if (lastCall != null && (now - lastCall) < COOLDOWN_MS) {
            warning("Please wait " + ((COOLDOWN_MS - (now - lastCall)) / 1000.0) +
                    " seconds before using this command again");
            return 0;
        }

        lastCallTime.put(playerId, now);

        // ... execute ...
    }
}
```

---

### 5.4 Long-Running Tool Execution

**Scenario**: MCP tool takes 30+ seconds to execute, blocking chat

**Solution**: Already handled via async execution in `MeteorExecutor.execute()`

**Additional Enhancement**: Add timeout handling

```java
MeteorExecutor.execute(() -> {
    try {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() ->
            GeminiExecutor.executeSimplePrompt(prompt)
        );

        String response = future.get(30, TimeUnit.SECONDS);
        info(response);

    } catch (TimeoutException e) {
        error("Request timed out after 30 seconds");
    }
});
```

---

## 6. Testing Strategy

### 6.1 Manual Test Checklist

**MCP Tool Commands**:
- [ ] Connect weather server, verify `/weather:get_forecast` appears in autocomplete
- [ ] Run `/weather:get_forecast "London" 3` (positional args)
- [ ] Run `/weather:get_forecast location="London" days=3` (named args)
- [ ] Run `/weather:get_forecast` with no args (should show error + usage)
- [ ] Run `/weather:get_forecast help` (should show parameter details)
- [ ] Disconnect weather server, verify command removed from autocomplete
- [ ] Try running disconnected command (should fail gracefully)

**Gemini Commands**:
- [ ] Run `.gemini "What is 2+2?"` (simple query)
- [ ] Run `.gemini` without arguments (should show error)
- [ ] Run `.gemini` without API key configured (should show config prompt)
- [ ] Spam `.gemini` commands (verify cooldown works)

**Gemini MCP Commands**:
- [ ] Connect weather server, run `.gemini-mcp "What's the weather in Tokyo?"`
- [ ] Verify Gemini calls `weather:get_forecast` automatically
- [ ] Run with multiple servers connected (weather + calendar)
- [ ] Run complex prompt requiring multiple tool calls
- [ ] Run with no servers connected (should fall back or warn)

**Error Cases**:
- [ ] Invalid argument type: `/weather:get_forecast days="abc"`
- [ ] Missing required parameter
- [ ] Tool execution failure (simulate server error)
- [ ] Network timeout (disconnect internet mid-call)

---

### 6.2 Automated Test Cases

**Unit Tests** (using JUnit):

```java
@Test
public void testArgumentParsing_Positional() {
    String argsString = "\"London\" 3";
    JsonNode schema = createWeatherSchema(); // Mock schema

    Map<String, Object> result = CommandUtils.parseArguments(argsString, schema);

    assertEquals("London", result.get("location"));
    assertEquals(3, result.get("days"));
}

@Test
public void testArgumentParsing_Named() {
    String argsString = "location=\"Paris\" days=5";
    JsonNode schema = createWeatherSchema();

    Map<String, Object> result = CommandUtils.parseArguments(argsString, schema);

    assertEquals("Paris", result.get("location"));
    assertEquals(5, result.get("days"));
}

@Test
public void testValidation_MissingRequired() {
    Map<String, Object> args = Map.of("days", 3); // Missing 'location'
    Tool tool = createWeatherTool();

    boolean valid = CommandUtils.validateRequiredParams(args, tool);

    assertFalse(valid);
}
```

---

## 7. File Structure Summary

### New Files

```
src/main/java/com/cope/meteormcp/
└── commands/ (NEW PACKAGE)
    ├── MCPToolCommand.java
    ├── GeminiCommand.java
    ├── GeminiMCPCommand.java
    └── CommandUtils.java
```

### Modified Files

```
src/main/java/com/cope/meteormcp/
├── MeteorMCPAddon.java
│   └── onInitialize() - Register Gemini commands
├── systems/MCPServers.java
│   ├── registerCommandsForServer() - NEW
│   ├── unregisterCommandsForServer() - NEW
│   ├── connect() - Call registerCommandsForServer()
│   └── disconnect() - Call unregisterCommandsForServer()
└── gemini/GeminiExecutor.java
    └── executeWithMCPToolsDetailed() - NEW (optional, for tool tracking)
```

---

## 8. Implementation Order

**Recommended sequence**:

1. **CommandUtils Foundation** (2-3 hours)
   - Create `CommandUtils.java`
   - Implement `tokenizeArguments()`
   - Implement `parsePositionalArguments()`
   - Implement `parseNamedArguments()`
   - Implement `coerceValue()`
   - Write unit tests for parsing logic

2. **Basic MCP Tool Command** (1-2 hours)
   - Create `MCPToolCommand.java`
   - Implement constructor and `build()`
   - Implement `executeToolCommand()`
   - Test with simple MCP server (no args)

3. **Command Registration System** (1 hour)
   - Add `registerCommandsForServer()` to `MCPServers`
   - Add `unregisterCommandsForServer()` to `MCPServers`
   - Update `connect()` and `disconnect()` methods
   - Test dynamic registration/removal

4. **Argument Validation** (1 hour)
   - Implement `validateRequiredParams()` in `CommandUtils`
   - Implement `generateUsage()` in `CommandUtils`
   - Add validation to `executeToolCommand()`
   - Test error messages

5. **Result Formatting** (1 hour)
   - Implement `displayToolResult()` in `CommandUtils`
   - Implement `displayContent()` for different content types
   - Test with various MCP tool responses

6. **Gemini Simple Command** (30 minutes)
   - Create `GeminiCommand.java`
   - Implement async execution
   - Add cooldown logic
   - Test basic queries

7. **Gemini MCP Command** (1 hour)
   - Create `GeminiMCPCommand.java`
   - Implement server auto-detection
   - Add progress feedback
   - Test multi-tool workflows

8. **Help System** (30 minutes)
   - Add `help` subcommand to `MCPToolCommand`
   - Implement `showHelp()` method
   - Test help display

9. **Autocomplete** (optional, 1 hour)
   - Add suggestion provider to `MCPToolCommand`
   - Test parameter name suggestions

10. **Testing & Polish** (2-3 hours)
    - Run through manual test checklist
    - Fix bugs and edge cases
    - Improve error messages
    - Test with multiple MCP servers

**Total Estimated Time**: 10-14 hours

---

## 9. Usage Examples

### Basic MCP Tool Usage

```bash
# Weather server with get_forecast tool
/weather:get_forecast "Tokyo"
> [Weather] Tokyo: 25°C, Sunny, Light winds

# File system server with read_file tool
/fs:read_file "/path/to/file.txt"
> [FS] File contents:
> Line 1
> Line 2
> Line 3

# Database server with query tool
/database:query '{"table": "users", "limit": 5}'
> [Database] Found 5 results:
> 1. Alice (alice@example.com)
> 2. Bob (bob@example.com)
> ...
```

---

### Gemini Simple Queries

```bash
.gemini "What is the capital of France?"
> [Gemini] The capital of France is Paris.

.gemini "Explain quantum entanglement"
> [Gemini] Quantum entanglement is a phenomenon where two or more
> particles become interconnected in such a way that the state of
> one particle instantly influences the state of the other...

.gemini "Write a haiku about Minecraft"
> [Gemini] Blocks stack high above
> Creepers lurk in darkened caves
> Build and mine, survive
```

---

### Gemini MCP Workflows

```bash
# Single tool call
.gemini-mcp "What's the weather like in London?"
> [Gemini MCP] Querying Gemini with 1 MCP servers...
> [Gemini MCP] The weather in London is currently cloudy with a
> temperature of 15°C and light rain expected later today.
> Tools used: weather:get_forecast

# Multi-tool workflow
.gemini-mcp "Check if it's sunny in Paris tomorrow, and if so, schedule a picnic at 2pm"
> [Gemini MCP] Querying Gemini with 2 MCP servers...
> [Gemini MCP] Good news! The forecast for Paris tomorrow shows sunny
> weather with temperatures around 22°C. I've scheduled a picnic event
> for 2:00 PM tomorrow. Would you like me to send invitations?
> Tools used: weather:get_forecast, calendar:schedule_meeting

# Data analysis
.gemini-mcp "What were our top 3 products by sales last month?"
> [Gemini MCP] Querying Gemini with 1 MCP servers...
> [Gemini MCP] Based on last month's sales data, your top 3 products were:
> 1. Widget Pro X - 1,234 units ($61,700)
> 2. Gadget Lite - 987 units ($29,610)
> 3. Tool Master - 756 units ($45,360)
> Tools used: database:query_sales
```

---

## 10. Key Design Decisions

### Why Dynamic Command Registration?

**Alternative**: Register all possible commands upfront

**Chosen Approach**: Register commands when servers connect

**Rationale**:
- **Cleaner command list**: Only shows commands for active servers
- **No namespace pollution**: Disconnected servers don't clutter autocomplete
- **Accurate help text**: Commands always reflect current connection state
- **Resource efficiency**: Don't hold command objects for disconnected servers

---

### Why Colon Separator for Tool Commands?

**Alternative**: Use underscore (`/weather_get_forecast`) or dash (`/weather-get-forecast`)

**Chosen Approach**: Colon (`/weather:get_forecast`)

**Rationale**:
- **Namespace clarity**: Colon visually separates server from tool
- **Minecraft convention**: Matches `namespace:id` pattern (e.g., `minecraft:stone`)
- **Easy parsing**: Simple string split on first `:`
- **No conflicts**: Colons rarely appear in server/tool names

---

### Why Two Gemini Commands Instead of Flags?

**Alternative**: Single `.gemini` command with `--mcp` flag

**Chosen Approach**: Separate `.gemini` and `.gemini-mcp` commands

**Rationale**:
- **Simpler UX**: No need to remember flags
- **Clear intent**: Command name indicates behavior
- **Faster typing**: No extra characters
- **Better autocomplete**: Each command shows in suggestions

---

### Why Async Execution for Gemini Commands?

**Alternative**: Block on API calls

**Chosen Approach**: Use `MeteorExecutor.execute()` for async

**Rationale**:
- **Responsive UI**: Chat input remains usable during API calls
- **User feedback**: Can show "Querying..." message immediately
- **Timeout handling**: Can implement cancellation if needed
- **Consistent with Meteor patterns**: MeteorPlus GPTCommand uses same approach

---

## 11. Security Considerations

### Command Injection

**Risk**: User-controlled command arguments could contain malicious content

**Mitigation**:
- All arguments parsed by Brigadier (built-in sanitization)
- No shell execution or eval() patterns
- MCP SDK handles JSON serialization securely

---

### API Key Exposure

**Risk**: Gemini API key visible in config

**Current State**: Stored in NBT (same as MCP server configs)

**Acceptable**: Local single-player environment

---

### Rate Limiting

**Risk**: Users spam Gemini commands, rack up API costs

**Mitigation**:
- 1-second cooldown per player
- Optional: Add daily/hourly quota in `GeminiConfig`

---

## 12. Future Enhancements

### Command Aliases

Allow users to create shorter aliases for frequently used commands:

```bash
# Config: alias "wf" → "weather:get_forecast"
/wf "London"  # Expands to /weather:get_forecast "London"
```

---

### Command History

Store recent command executions for quick re-run:

```bash
.gemini-history
> Recent queries:
> 1. "What's the weather in Tokyo?"
> 2. "Schedule a meeting at 2pm"

.gemini-rerun 1  # Re-execute query #1
```

---

### Batch Execution

Run multiple commands in sequence:

```bash
/mcp-batch "weather:get_forecast Tokyo" "calendar:schedule_meeting 14:00 Picnic"
```

---

### Command Output Capture

Store command results for later use:

```bash
/weather:get_forecast "London" --save weather_data
.gemini-mcp "Analyze this weather data: ${weather_data}"
```

---

## 13. Documentation Updates

### CLAUDE.md Updates

Add new section after Gemini integration:

```markdown
## Command System Integration (Phase 3)

### MCP Tool Commands

After connecting an MCP server, its tools become available as commands:

#### Syntax
.server_name:tool_name [arguments]

#### Examples
# Positional arguments
/weather:get_forecast "London" 3

# Named arguments
/weather:get_forecast location="London" days=3

# Help
/weather:get_forecast help

### Gemini Commands

#### Simple Queries (No MCP Tools)
.gemini "What is the capital of France?"

#### MCP-Enhanced Queries (Automatic Tool Access)
.gemini-mcp "What's the weather in Tokyo?"

# Gemini automatically uses ALL connected MCP servers
# to answer your query intelligently

### Command Features

- **Dynamic Registration**: Commands appear/disappear as servers connect/disconnect
- **Autocomplete**: Tab-completion for command names and parameters
- **Help System**: `.command help` shows usage and parameter details
- **Async Execution**: Gemini commands run in background, don't block chat
- **Error Handling**: Clear error messages with suggested fixes

### Architecture

- MCPToolCommand: Dynamic command class, one per MCP tool
- GeminiCommand: Simple AI queries
- GeminiMCPCommand: AI queries with automatic MCP tool access
- CommandUtils: Argument parsing, validation, result formatting
```

---

### README.md Updates

Add to features section:

```markdown
## Features

- ✅ **MCP Server Management**: Connect, configure, and monitor MCP servers via GUI
- ✅ **StarScript Integration**: Use MCP tools in expressions `{server.tool()}`
- ✅ **Gemini AI Integration**: Intelligent prompts with `{gemini()}` and `{gemini_mcp()}`
- ✅ **Command System**: Direct tool execution via `.server:tool` commands
- ✅ **Gemini Commands**: Quick AI queries via `.gemini` and `.gemini-mcp`
- ✅ **Dynamic Registration**: Commands auto-register when servers connect
- ✅ **Autocomplete**: Tab-completion for commands and parameters
- ✅ **Help System**: Inline help with `.command help`

## Usage Examples

### Command-Line Tool Invocation
```bash
# After connecting weather server
/weather:get_forecast "Tokyo" 3
/weather:get_forecast location="Tokyo" days=3
```

### AI-Powered Queries
```bash
# Simple query
.gemini "Explain quantum physics"

# Query with MCP tool access (automatic)
.gemini-mcp "Check weather in London and schedule picnic if sunny"
```

### StarScript Integration
```javascript
// HUD element
{weather.get_forecast("Tokyo", 3)}

// Gemini in HUD
{gemini("Current time?")}
{gemini_mcp("What's my schedule today?")}
```
```

---

## Summary

This specification provides a complete blueprint for adding command system integration to the Meteor MCP addon. The implementation:

✅ **Dynamic Command Registration**: Commands auto-register/unregister with server state
✅ **Flexible Argument Parsing**: Supports positional, named, and JSON arguments
✅ **User-Friendly Syntax**: `.server:tool` pattern inspired by Minecraft conventions
✅ **Gemini Command Access**: Both simple (`.gemini`) and enhanced (`.gemini-mcp`) modes
✅ **Async Execution**: Non-blocking API calls for responsive UI
✅ **Help System**: Inline help with parameter details
✅ **Robust Error Handling**: Graceful failures with clear messages
✅ **Autocomplete Support**: Tab-completion for better UX
✅ **Follows Meteor Patterns**: Uses `Command` base class, `Commands.add()`, `MeteorExecutor`

**Key Innovation**: Dynamic command registration mirrors the StarScript integration pattern - tools are available when servers are connected, and automatically cleaned up on disconnect. This provides a consistent mental model across both interaction methods (commands vs StarScript).

---

## Implementation Checklist

- [ ] Create `commands/` package with 4 new classes
- [ ] Implement `CommandUtils` with argument parsing and validation
- [ ] Implement `MCPToolCommand` with dynamic tool execution
- [ ] Implement `GeminiCommand` with async execution
- [ ] Implement `GeminiMCPCommand` with auto-server detection
- [ ] Update `MCPServers` with registration methods
- [ ] Update `MeteorMCPAddon.onInitialize()` to register Gemini commands
- [ ] Add help system to `MCPToolCommand`
- [ ] Add autocomplete support (optional)
- [ ] Write unit tests for `CommandUtils`
- [ ] Run manual test checklist
- [ ] Update documentation (CLAUDE.md, README.md)

**Estimated Total Time**: 10-14 hours
