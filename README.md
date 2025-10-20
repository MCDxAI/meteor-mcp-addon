# Meteor MCP Addon

Meteor MCP bridges the [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) with the
Meteor Client mod. It lets you register MCP servers inside Meteor and call their tools directly
from StarScript, giving HUD elements, chat macros, Discord presence, and other modules live access
to external data and automation.

## Features
- Configure MCP servers from an in-game GUI (STDIO transport supported today)
- Auto-connect servers on startup and persist settings via Meteor's system storage
- Browse tools, copy StarScript snippets, and execute them in real time
- Register each tool globally as `{serverName.toolName(arg1, arg2)}` in StarScript
- Dynamic chat commands for every connected MCP tool (`/server:tool …`) with help and tab completion
- Gemini chat commands: `/gemini` for quick prompts, `/gemini-mcp` for LLM prompts with automatic MCP tool chaining
- Clean shutdown and reconnect logic that keeps StarScript bindings and commands in sync
- Includes the official MCP Java SDK and required runtime dependencies for zero-fuss installs

## Requirements
- Minecraft 1.21.8 with Fabric Loader 0.16.14
- Meteor Client 1.21.8-56 (bundled locally in `libs/` for development)
- Java 21 (for building and running the addon)
- At least one MCP-compatible server binary or script to connect to

## Installing
1. Build the addon (`./gradlew clean build`). The output jar will be in `build/libs/`.
2. Copy the `meteor-mcp-<version>-fabric.jar` into your Minecraft `mods` directory alongside
   Meteor Client.
3. Launch Minecraft. A new **MCP** tab will appear in the Meteor GUI.

## Configuring Servers
1. Open Meteor (default key: `Right Shift`) and select the **MCP** tab.
2. Press **Add Server** and supply:
   - **Name** – used for display and the StarScript namespace
   - **Command/Args** – executable plus arguments when using STDIO transport
   - Optional environment variables, timeout, and auto-connect flag
3. Save the server, then choose **Connect**. Successful connections will immediately expose the
   remote tools to StarScript.
4. Use **Tools** to browse metadata, copy example usage strings, and verify the connection.

Servers can be edited, removed, or reconnected at any time. Removing or disconnecting a server
automatically unregisters its StarScript functions to avoid stale entries.

## Configuring Gemini
1. Open Meteor (default key: `Right Shift`) and select the **MCP** tab.
2. Click **Configure Gemini API**.
3. Enter your Gemini API key, choose a model, adjust token/temperature limits if needed, and enable the toggle.
4. Use **Test Connection** to verify the credentials, then **Save** to persist the configuration.

The Gemini client is cached and automatically refreshed when you update settings. Disabling the toggle keeps
your settings on disk without attempting any Gemini calls.

## Using StarScript
After connecting a server, the addon registers each tool under the server's name. Example:

```text
{weather.get_forecast(player.pos.x, player.pos.z)}
```

Place the expression anywhere StarScript is supported—HUD text elements, chat commands, macros,
Discord presence, modules, etc. Arguments are automatically converted to the JSON payload expected
by the tool schema, and results are converted back to StarScript-friendly values.

With Gemini enabled you also gain two helper functions:

```text
{gemini("Summarize the latest chat message")}
{gemini_mcp("Fetch the current weather using any connected servers and summarize it")}
```

`gemini` performs a simple LLM call. `gemini_mcp` lets Gemini inspect every connected MCP server and
autonomously invoke tools as part of its reasoning loop.

## Command Interface

### MCP Tool Commands
- Syntax: `/server_name:tool_name [arguments]`
- Supports positional arguments (`/weather:get_forecast "London" 3`)
- Supports named arguments (`/weather:get_forecast location="London" days=3`)
- Accepts raw JSON payloads (`/database:query {"table":"users","limit":10}`)
- Built-in help via `/server:tool help`
- Tab completion suggests parameter names (`days=`, `location=`, …)

Commands register dynamically when a server connects and disappear automatically on disconnect, so the chat list always mirrors your active MCP environment.

### Gemini Commands
- `/gemini "prompt"` – fire-and-forget Gemini prompt (no MCP tools)
- `/gemini-mcp "prompt"` – Gemini with full access to every connected MCP tool

Responses from `/gemini-mcp` show which tools were used:  
`[Tools Used] weather:get_forecast (125ms), calendar:create_event (341ms)`

Gemini commands run asynchronously (they never freeze chat) and include a lightweight cooldown to prevent accidental spam/API overuse. If Gemini isn't configured yet, the commands guide you back to the MCP tab.

## Development

### Prerequisites
- Java 21 (`openjdk-21` or newer JDK 21 distribution)
- Gradle wrapper included with the project

### Building
```bash
./gradlew clean build
```

Gradle downloads the necessary dependencies (Meteor Client jar, MCP SDK, Reactor, etc.) and bundles
them into the addon jar. Build artifacts land in `build/libs/`. The project also supports the usual
IDE run configurations supplied by the standard Meteor addon template if you want to launch a dev
client.

### Source Layout
- `com.cope.meteormcp.MeteorMCPAddon` – addon entry point
- `systems` – persistence, configuration, and connection lifecycle management
- `starscript` – adapters that convert between StarScript values and MCP payloads

Each component is fully documented in-code for easier onboarding.

## Troubleshooting
- Use the Chat or log output to confirm connection success. The addon logs failures with enough
  detail to diagnose command misconfiguration or server crashes.
- If a tool signature changes while connected, reconnect the server (or use auto-connect on startup)
  so the StarScript namespace refreshes.
- Runtime dependencies are shaded into the jar. If you see missing-class errors, rebuild to ensure
  the latest jars are bundled.

## License

Distributed under the license included in `LICENSE`. Refer to that file for details.
