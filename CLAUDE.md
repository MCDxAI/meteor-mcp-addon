# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Meteor Client addon** that bridges the **Model Context Protocol (MCP)** with **Minecraft** via **StarScript**. It enables:
- MCP servers to run and expose tools inside Minecraft
- StarScript expressions to call MCP tools in real-time (e.g., HUD elements, chat macros, Discord presence)
- **Future**: Gemini AI integration to intelligently use MCP tools from within Minecraft

### Current Status
- ✅ **Phase 1 Complete**: MCP server management, StarScript tool registration, GUI for configuration
- 🚧 **Phase 2 (Future)**: Gemini AI integration to use MCP tools intelligently

## Technology Stack

### Core Dependencies
- **Java 21**: Target and source compatibility
- **Minecraft 1.21.8** with **Fabric Loader 0.16.14**
- **Meteor Client 1.21.8-56**: Bundled locally in `libs/` for development
- **Gradle 8.x** with Kotlin DSL (`build.gradle.kts`)

### MCP Integration
- **MCP Java SDK 0.14.1** (`io.modelcontextprotocol.sdk:mcp`)
  - `mcp-core`, `mcp-json`, `mcp-json-jackson2` modules
  - STDIO transport (SSE/HTTP planned but not yet implemented)
- **Reactor Core 3.6.5**: Reactive streams for async MCP communication
- **JSON Schema Validator 1.5.7**: Schema validation for MCP tool definitions

### Meteor Ecosystem
- **StarScript 0.2.3** (`org.meteordev:starscript`): Expression language for dynamic text/macros
- **Meteor Systems**: Persistent NBT-based configuration storage

### Future: Gemini AI Integration
- **Google GenAI Java SDK** (`com.google.genai:google-genai:1.23.0`) - Official Gemini API SDK
- **Function Calling**: Convert MCP tools to Gemini `FunctionDeclaration` or Java `Method` reflection
- **Integration Pattern**: Gemini → Tool Selection → MCP Server Execution → StarScript Display

## Development Commands

### Building
```bash
./gradlew clean build
```
- Output: `build/libs/meteor-mcp-<version>-fabric.jar`
- All dependencies are shaded into the jar (MCP SDK, Reactor, etc.)

### Installation
1. Build the addon
2. Copy `build/libs/meteor-mcp-<version>-fabric.jar` to your Minecraft `mods/` folder
3. Ensure Meteor Client is also installed
4. Launch Minecraft - MCP tab appears in Meteor GUI

### Development Environment
- **IDE Support**: IntelliJ IDEA / Eclipse with Fabric mod development
- **Hot Reload**: Standard Fabric dev client workflow (not documented here)
- **Debugging**: Use IDE debugging with Fabric dev environment

## Architecture

### High-Level Structure
```
MeteorMCPAddon (Entry Point)
    ├─ Systems.add(MCPServers)      # Persistent server configuration
    ├─ Tabs.add(MCPTab)             # GUI registration
    └─ MeteorStarscript.ss.set()    # Global tool registration

StarScript Expression Flow:
{serverName.toolName(arg1, arg2)}
    ↓
MCPToolExecutor.execute()
    ↓
MCPValueConverter (StarScript ↔ JSON)
    ↓
MCPServerConnection.callTool()
    ↓
MCP Java SDK (STDIO Transport)
    ↓
External MCP Server Process
```

### Source Layout (`src/main/java/com/cope/meteormcp/`)

#### Core Components
- **`MeteorMCPAddon.java`**: Entry point, plugin initialization
  - `onInitialize()`: Registers systems, tabs, connects auto-connect servers
  - `registerServerToStarScript()`: Injects tools into global StarScript namespace
  - `unregisterServerFromStarScript()`: Cleanup on disconnect

#### Systems (`systems/`)
- **`MCPServers.java`**: Singleton system manager (Meteor pattern)
  - Manages `Map<String, MCPServerConnection>` for active connections
  - Manages `Map<String, MCPServerConfig>` for stored configurations
  - CRUD operations: `add()`, `remove()`, `update()`, `connect()`, `disconnect()`
  - Persistence: `toTag()` / `fromTag()` for NBT serialization
  - Auto-connect: `connectAutoConnect()` on startup

- **`MCPServerConfig.java`**: Configuration model
  - Stores: name, transport type, command/args, URL, env vars, timeout, auto-connect flag
  - `TransportType`: STDIO (working), SSE/HTTP (planned)
  - Validation: `isValid()` ensures required fields per transport type
  - NBT serialization for Meteor's system storage

- **`MCPServerConnection.java`**: MCP client wrapper
  - Wraps `McpSyncClient` from MCP Java SDK
  - STDIO process management via `StdioClientTransport`
  - Tool discovery: `listTools()` → `List<Tool>`
  - Execution: `callTool(toolName, arguments)` → `CallToolResult`
  - Lifecycle: `connect()`, `disconnect()`, `reconnect()`
  - Rate limiting: 5-second reconnect cooldown

#### StarScript Integration (`starscript/`)
- **`MCPToolExecutor.java`**: Bridges StarScript function calls to MCP
  - `createToolFunction()`: Returns `Value.function()` wrapping MCP tool
  - `execute()`: Extracts args from StarScript stack, calls MCP, converts result
  - `generateExampleSyntax()`: Generates `{server.tool(args)}` snippets for GUI
  - Schema introspection: `getParameterNames()`, `getRequiredParameters()`, `getParameterType()`

- **`MCPValueConverter.java`**: Bidirectional StarScript ↔ JSON conversion
  - `toJson(Value)`: Converts StarScript types (bool, number, string, map) to JSON
  - `toValue(CallToolResult)`: Converts MCP response to StarScript string/value
  - `extractArgumentsAsMap()`: Pops args from StarScript stack, maps to parameter names
  - Handles MCP content types: TextContent, ImageContent, ResourceContents

#### GUI (`gui/`)
- **`tabs/MCPTab.java`**: Meteor GUI tab registration ("MCP" tab)
- **`screens/MCPServersScreen.java`**: Main server list screen
- **`screens/AddMCPServerScreen.java`**: Server creation form
- **`screens/EditMCPServerScreen.java`**: Server editing form
- **`screens/MCPToolsScreen.java`**: Tool browser with copy-to-clipboard StarScript snippets

### Key Patterns

#### Meteor System Pattern
```java
// Singleton access via Systems registry
MCPServers servers = MCPServers.get();
servers.add(config);
servers.connect("serverName");
```

#### StarScript Registration
```java
// Tools become globally accessible in StarScript
ValueMap serverMap = new ValueMap();
serverMap.set("toolName", MCPToolExecutor.createToolFunction(connection, tool));
MeteorStarscript.ss.set("serverName", serverMap);

// Now usable anywhere: {serverName.toolName(args)}
```

#### MCP Tool Execution Flow
1. User types `{weather.get_forecast(x, z)}` in HUD element
2. StarScript evaluates expression, calls registered function
3. `MCPToolExecutor.execute()` extracts args from stack
4. `MCPValueConverter.toJson()` converts StarScript values to JSON
5. `MCPServerConnection.callTool()` sends request to MCP server
6. Server executes tool, returns `CallToolResult`
7. `MCPValueConverter.toValue()` converts result to StarScript string
8. Result displays in HUD

## MCP Server Configuration

### STDIO Transport (Currently Supported)
```java
MCPServerConfig config = new MCPServerConfig("serverName", TransportType.STDIO);
config.setCommand("npx");
config.setArgs(Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "/path"));
config.setTimeout(5000); // 5 seconds
config.setAutoConnect(true); // Connect on startup
```

### GUI Workflow
1. Open Meteor (default: Right Shift)
2. Navigate to "MCP" tab
3. Click "Add Server"
4. Fill in: Name, Command, Arguments, Environment Variables (optional)
5. Enable "Auto Connect" (optional)
6. Save → Connect → Browse Tools

### Tool Usage in StarScript
After connecting a server named "fs" with a tool "readFile":
```
{fs.readFile("/path/to/file.txt")}
```

This works in:
- HUD text elements
- Chat macros
- Discord Rich Presence
- Custom Meteor modules

## Future: Gemini AI Integration

### Planned Architecture
```
User Input (in Minecraft chat/HUD)
    ↓
Gemini Client (com.google.genai)
    ↓
GenerateContentConfig with MCP Tools (as FunctionDeclarations)
    ↓
Gemini selects tool to call
    ↓
GeminiMCPBridge routes to MCPServerConnection.callTool()
    ↓
Result fed back to Gemini
    ↓
Final response displayed in Minecraft
```

### Implementation Strategy

#### Option 1: Automatic Function Calling (AFC)
- Create wrapper methods for each MCP tool
- Use Java reflection to expose methods to Gemini
- Gemini SDK automatically executes functions (up to 10 chained calls)
- Simple but less control over execution flow

```java
// Pseudocode
public static String mcpToolWrapper(String arg1, String arg2) {
    return MCPServers.get().getConnection("server").callTool("tool", args);
}

Method method = Class.getMethod("mcpToolWrapper", String.class, String.class);
GenerateContentConfig config = GenerateContentConfig.builder()
    .tools(Tool.builder().functions(method))
    .build();
```

#### Option 2: Manual Function Calling
- Convert MCP tool schemas to `FunctionDeclaration` objects
- Parse Gemini's function call requests from response
- Manually route to `MCPServerConnection.callTool()`
- Feed results back for next Gemini turn
- More control, better for complex multi-step workflows

```java
// Pseudocode
FunctionDeclaration fd = FunctionDeclaration.newBuilder()
    .setName(tool.name())
    .setDescription(tool.description())
    .setParameters(convertMcpSchemaToGeminiSchema(tool.inputSchema()))
    .build();

Tool geminiTool = Tool.newBuilder().addFunctionDeclarations(fd).build();
// ... parse response for function calls, execute via MCP, iterate
```

### Key Integration Points
1. **Schema Conversion**: MCP `JsonSchema` → Gemini `Schema` (OpenAPI 3.0 format)
2. **Result Mapping**: `CallToolResult` → Gemini-compatible response format
3. **Conversation Context**: Maintain chat history for multi-turn interactions
4. **StarScript Display**: Final Gemini response rendered via StarScript in HUD/chat

### Dependencies to Add
```kotlin
// In build.gradle.kts
implementation("com.google.genai:google-genai:1.23.0")!!.let { include(it) }
```

### Environment Setup
```bash
# Gemini API key (for development)
export GOOGLE_API_KEY='your-api-key'
```

## Reference Documentation

### Local Documentation (`ai_docs/`)
Comprehensive Gemini API and Genkit documentation copied from `gemini-discord-bot` reference codebase:
- **Gemini API Guides**: `ai_docs/gemini/` (text generation, function calling, multimodal, etc.)
- **Genkit Guides**: `ai_docs/gemini/genkit-*.md` (NOTE: Genkit is TypeScript/Python/Go only, not used in this Java addon)
- **MCP + Genkit**: `ai_docs/genkit-ai-mcp.md` (TypeScript reference for MCP integration patterns)

**Important**: The Genkit docs are for **reference only** to understand MCP integration patterns. We use **Google GenAI Java SDK** directly, not Genkit.

### External Resources
- **Meteor Client**: https://github.com/MeteorDevelopment/meteor-client
- **StarScript**: https://github.com/MeteorDevelopment/starscript
- **Orbit Events**: https://github.com/MeteorDevelopment/orbit (Meteor's event system)
- **Meteor Addon Template**: https://github.com/MeteorDevelopment/meteor-addon-template
- **MCP Java SDK**: https://github.com/modelcontextprotocol/java-sdk
- **MCP Specification**: https://modelcontextprotocol.io/sdk/java/mcp-overview
- **Google GenAI Java SDK**: https://github.com/googleapis/java-genai
- **Gemini API Docs**: https://ai.google.dev/gemini-api/docs

### MCP Tools Available via meteor-addon-mcp
Use the `meteor-addon-mcp` MCP server tools when developing:
- `fetch-module-examples`: Search Meteor Client modules for reference implementations
- `search-meteor-utils`: Find utility classes and methods
- `fetch-addon-template`: Get addon structure/boilerplate
- `analyze-events`: Understand Meteor's event system
- `generate-addon-module`: Code generation for new modules

### Web Scraping
When fetching documentation from URLs, prefer `mcp__cloudscraper-mcp__scrape_url` (with `clean_content: true`) over built-in tools for better markdown formatting.

## Common Development Scenarios

### Adding a New MCP Server
1. Ensure server executable is available (e.g., `npx -y @modelcontextprotocol/server-git`)
2. Create config: `new MCPServerConfig("git", TransportType.STDIO)`
3. Set command/args: `config.setCommand("npx")`, `config.setArgs(["..."])`
4. Add to system: `MCPServers.get().add(config)`
5. Connect: `MCPServers.get().connect("git")`
6. Tools auto-register to StarScript namespace `{git.*}`

### Debugging MCP Connection Issues
- Check logs: `MeteorMCPAddon.LOG` outputs connection status
- Verify command: Run MCP server command manually in terminal
- Check timeout: Default 5s, increase if server is slow to start
- Reconnect cooldown: 5 seconds between reconnect attempts
- STDIO transport only: SSE/HTTP not yet implemented

### Testing StarScript Expressions
1. Connect to MCP server in GUI
2. Open "Tools" screen to browse available tools
3. Copy generated StarScript snippet
4. Paste into HUD element text field
5. Verify result displays correctly

### Extending to New Transport Types
1. Implement in `MCPServerConnection.connect()` switch case
2. Add transport-specific client initialization
3. Update `MCPServerConfig.isValid()` validation
4. Test connection lifecycle (connect, disconnect, reconnect)

## Code Style and Conventions

### Naming
- **Classes**: PascalCase (e.g., `MCPServerConnection`)
- **Methods**: camelCase (e.g., `callTool()`, `getParameterNames()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `RECONNECT_COOLDOWN`)
- **Packages**: lowercase (e.g., `com.cope.meteormcp.systems`)

### Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    public static final Logger LOG = LoggerFactory.getLogger("Meteor MCP");

    LOG.info("Connection established: {}", serverName);
    LOG.warn("Retrying connection...");
    LOG.error("Failed to connect: {}", e.getMessage());
}
```

### Error Handling
- **Connection failures**: Log error, return false, avoid throwing exceptions
- **Tool execution failures**: Return `Value.null_()`, log error details
- **NBT deserialization**: Catch exceptions, skip invalid entries, continue loading

### Documentation
- **Javadoc**: Required for public methods and classes
- **Inline comments**: Explain complex logic, edge cases, or non-obvious decisions
- **README.md**: User-facing documentation for installation and usage
- **CLAUDE.md**: This file - for AI assistant context and architecture

## Project Metadata

- **Author**: GhostTypes (Cope)
- **License**: See `LICENSE` file
- **Version**: 0.1.0 (see `gradle.properties`)
- **Mod ID**: `meteor-mcp`
- **Fabric Mod Entry Point**: `com.cope.meteormcp.MeteorMCPAddon` (via `fabric.mod.json`)
- **Meteor Color**: `100,150,255` (custom color in Meteor GUI)

## Important Notes

### Windows Development
- This project is developed on **Windows** (`C:\Users\Cope\...`)
- Use forward slashes in paths for cross-platform compatibility
- Gradle wrapper: `.\gradlew` (Windows) or `./gradlew` (Unix)

### Dependency Management
- **Local JARs**: Meteor Client is in `libs/` directory (via `flatDir` repository)
- **Shading**: MCP SDK and dependencies are included in final jar (`include(it)`)
- **StarScript**: External dependency (not bundled in Meteor Client)

### Reference Codebase
The `gemini-discord-bot` codebase at `C:\Users\Cope\Documents\GitHub\gemini-discord-bot` provides:
- Working examples of Gemini API usage (TypeScript/Genkit patterns)
- MCP + Genkit integration patterns (TypeScript reference)
- Multimodal processing examples (images, PDFs, attachments)
- Function calling and tool usage patterns

**Adaptation Required**: Convert TypeScript/Genkit patterns to Java/GenAI SDK equivalents.

## Getting Help

### MCP Addon Development
- Use `meteor-addon-mcp` MCP tools for Meteor-specific guidance
- Check Meteor Client source code for System/GUI patterns
- Reference addon template for boilerplate structure

### Gemini Integration
- See `ai_docs/gemini/function-calling-guide.md` for conceptual patterns
- Google GenAI Java SDK README: https://github.com/googleapis/java-genai/blob/main/README.md
- Gemini API docs: https://ai.google.dev/gemini-api/docs/function-calling

### MCP Protocol
- Java SDK docs: https://modelcontextprotocol.io/sdk/java/mcp-overview
- Protocol spec: https://modelcontextprotocol.io/
- Official Java SDK: https://github.com/modelcontextprotocol/java-sdk
