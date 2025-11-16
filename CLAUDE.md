# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Meteor Client addon** that bridges the **Model Context Protocol (MCP)** with **Minecraft** via **StarScript**. It enables:
- MCP servers to run and expose tools inside Minecraft
- StarScript expressions to call MCP tools in real-time (e.g., HUD elements, chat macros, Discord presence)
- Gemini AI integration to intelligently use MCP tools from within Minecraft
- Chat commands for direct MCP tool invocation and AI-powered conversations

### Current Status
**🎉 All Core Features Complete!**

- ✅ **MCP Integration**: Full STDIO transport support with server management, tool discovery, and execution
- ✅ **StarScript Integration**: Dynamic tool registration, argument conversion, result handling
- ✅ **Gemini AI Integration**: Multi-turn conversations with automatic MCP tool calling
- ✅ **Chat Commands**: Dynamic `/server:tool`, `/gemini`, and `/gemini-mcp` with help system
- ✅ **GUI System**: Complete configuration screens for servers, tools, and Gemini settings
- ✅ **Persistence**: NBT-based storage for all configurations

### Future Enhancements
- 🔄 **SSE/HTTP Transport**: Additional transport types for MCP servers (currently STDIO only)

## Technology Stack

### Core Dependencies
- **Java 21**: Target and source compatibility
- **Minecraft 1.21.10** with **Fabric Loader 0.17.3**
- **Meteor Client 1.21.10-32**: Bundled locally in `libs/` for development
- **Gradle 8.x** with Kotlin DSL (`build.gradle.kts`)

### MCP Integration
- **MCP Java SDK 0.14.1** (`io.modelcontextprotocol.sdk:mcp`)
  - `mcp-core`, `mcp-json`, `mcp-json-jackson2` modules
  - STDIO transport (fully implemented)
  - SSE/HTTP transport (planned for future release)
- **Reactor Core 3.6.5**: Reactive streams for async MCP communication
- **JSON Schema Validator 1.5.7**: Schema validation for MCP tool definitions

### Meteor Ecosystem
- **StarScript 0.2.3** (`org.meteordev:starscript`): Expression language for dynamic text/macros
- **Meteor Systems**: Persistent NBT-based configuration storage

### Gemini AI Integration
- **Google GenAI Java SDK** (`com.google.genai:google-genai:1.23.0`) bundled and shaded into the addon.
- **Client Management**: `GeminiClientManager` handles API key caching, client lifecycle, and test connections.
- **Execution Flow**: `GeminiExecutor` supports simple prompts and multi-turn MCP-enhanced loops via manual function calling.
- **StarScript Helpers**: `GeminiStarScriptIntegration` exposes `{gemini()}` and `{gemini_mcp()}` for in-game usage.

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
  - `onInitialize()`: Registers systems, tabs, Gemini commands, connects auto-connect servers
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
  - `TransportType`: STDIO (fully implemented), SSE/HTTP (planned)
  - Validation: `isValid()` ensures required fields per transport type
  - NBT serialization for Meteor's system storage

- **`MCPServerConnection.java`**: MCP client wrapper
  - Wraps `McpSyncClient` from MCP Java SDK
  - STDIO process management via `StdioClientTransport`
  - Tool discovery: `listTools()` → `List<Tool>`
  - Execution: `callTool(toolName, arguments)` → `CallToolResult`
  - Lifecycle: `connect()`, `disconnect()`, `reconnect()`
  - Rate limiting: 5-second reconnect cooldown

#### Command Integration (`commands/`)
- **`CommandUtils.java`**: Shared key=value/JSON parser plus chat output helpers for tool results
- **`MCPToolCommand.java`**: Dynamic Brigadier command per MCP tool (`/server:tool …`) with help + suggestions
- **`GeminiCommand.java`**: `/gemini "prompt"` chat command with async execution and per-player cooldown
- **`GeminiMCPCommand.java`**: `/gemini-mcp "prompt"` with automatic MCP tool discovery and usage reporting

`MCPServers` mirrors StarScript registration by adding/removing `MCPToolCommand` instances whenever servers connect or disconnect, refreshing the Brigadier dispatcher to keep the chat tree accurate.

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

## Gemini AI Integration Details

### Execution Flow
```
StarScript call ({gemini()} or {gemini_mcp()})
    ↓
GeminiStarScriptIntegration (argument extraction, server discovery)
    ↓
GeminiExecutor (simple prompt or MCP-enhanced loop)
    ↓
GeminiClientManager (cached Client, request config)
    ↓
Gemini SDK generateContent()
    ↓
MCPToGeminiBridge converts tool schemas + routes function calls
    ↓
MCPServerConnection.callTool()
    ↓
CallToolResult mapped back to Gemini → final response surfaced via StarScript
```

### Key Components
1. **GeminiClientManager** – lazy client creation, invalidation on config changes, connection testing helper.
2. **GeminiExecutor** – synchronous prompt handling plus manual function-calling loop for MCP tools.
3. **MCPToGeminiBridge** – JSON Schema conversion and server/tool routing metadata.
4. **GeminiStarScriptIntegration** – exposes `{gemini()}` and `{gemini_mcp()}` functions to the global StarScript instance.
5. **GeminiSettingsScreen** – GUI for API key, model selection, token/temperature limits, and enable toggle.

### Configuration & Dependencies
- `build.gradle.kts` shades `com.google.genai:google-genai:1.23.0` into the released jar.
- `MCPServers` stores `GeminiConfig` alongside server configs; persisted via NBT.
- Settings screen masks API key input, supports live "Test Connection" using GeminiExecutor.

### Behaviour Highlights
- Manual function calling provides complete control over tool execution order and error handling.
- Gemini automatically sees every connected MCP server when `{gemini_mcp()}` is used.
- Tool results are normalized to simple JSON payloads before being fed back into Gemini.
- All responses (success or failure) return string values so they can render safely in StarScript contexts.

## Reference Documentation

### Local Documentation (`ai_docs/`)
Comprehensive Gemini API and Genkit documentation copied from `gemini-discord-bot` reference codebase:
- **Gemini API Guides**: `ai_docs/gemini/` (text generation, function calling, multimodal, etc.)
- **Genkit Guides**: `ai_docs/gemini/genkit-*.md` (NOTE: Genkit is TypeScript/Python/Go only, not used in this Java addon)
- **MCP + Genkit**: `ai_docs/genkit-ai-mcp.md` (TypeScript reference for MCP integration patterns)

**Important**: The Genkit docs are for **reference only** to understand MCP integration patterns. We use **Google GenAI Java SDK** directly, not Genkit.

### Local Reference Repositories (`ai_reference/`)

**IMPORTANT**: The `ai_reference/` directory is **git-ignored** but contains complete clones of key Meteor Client repositories for development reference. Claude Code has full read access to these files.

**Location**: `C:\Users\Cope\Documents\GitHub\meteor-mcp-addon\ai_reference/`

#### How to Access ai_reference Files

Claude Code can read these files using standard tools:

1. **List directory structure**:
   ```bash
   ls -la ai_reference/
   find ai_reference -maxdepth 3 -type d
   ```

2. **Find specific files**:
   ```bash
   find ai_reference/meteor-client/src -name "Module.java" -type f
   find ai_reference/MeteorPlus/src -name "*Gemini*" -type f
   ```

3. **Read files directly**:
   ```
   Read tool: C:\Users\Cope\Documents\GitHub\meteor-mcp-addon\ai_reference\meteor-client\src\main\java\meteordevelopment\meteorclient\systems\modules\Module.java
   ```

4. **Search for code patterns**:
   ```bash
   grep -r "EventHandler" ai_reference/meteor-rejects/src/main/java/
   grep -r "StarScript" ai_reference/MeteorPlus/src/main/java/
   ```

#### Available Reference Repositories

The `ai_reference/` directory contains git clones of:

| Repository | Path | Purpose |
|------------|------|---------|
| **meteor-client** | `ai_reference/meteor-client/` | Core framework, base classes, event system |
| **starscript** | `ai_reference/starscript/` | Expression language implementation |
| **orbit** | `ai_reference/orbit/` | Event system library |
| **meteor-rejects** | `ai_reference/meteor-rejects/` | Feature-rich addon examples |
| **meteor-villager-roller** | `ai_reference/meteor-villager-roller/` | Minimal addon template |
| **MeteorPlus** | `ai_reference/MeteorPlus/` | Advanced addon with mode systems |

**Index File**: `ai_reference/INDEX.md` contains a comprehensive catalog of all repositories, their structure, key files, and usage patterns.

#### Best Practices for Using ai_reference

1. **Read INDEX.md first**: Always start with `ai_reference/INDEX.md` to understand what's available
2. **Use absolute paths**: When reading files, use full Windows paths: `C:\Users\Cope\Documents\GitHub\meteor-mcp-addon\ai_reference\...`
3. **Find before reading**: Use `find` or `grep` to locate relevant files before reading them
4. **Reference specific examples**: The INDEX.md file lists exact file paths for common development tasks
5. **Git operations**: The `.git` directories are present but should generally be ignored

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
- Transport types: STDIO fully supported, SSE/HTTP planned for future

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
