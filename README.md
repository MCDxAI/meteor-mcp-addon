# Meteor MCP 🔌

**Model Context Protocol integration for Meteor Client**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-0.16.14-blue?style=flat-square)
![Meteor](https://img.shields.io/badge/Meteor_Client-1.21.8--56-blueviolet?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![MCP](https://img.shields.io/badge/MCP-0.14.1-red?style=flat-square)

A Meteor Client addon that bridges the Model Context Protocol (MCP) with Minecraft. Connect to MCP servers and call their tools directly from StarScript expressions, chat commands, and optionally via Gemini AI.

## 🎯 Core Features

| Feature | Description |
|---------|-------------|
| **MCP Server Management** | Connect multiple MCP servers with persistent configuration and auto-connect on startup |
| **StarScript Integration** | Access tools as `{serverName.toolName(args)}` in HUD elements, chat macros, and Discord presence |
| **Dynamic Chat Commands** | Automatically registered `/serverName:toolName` commands with help and tab completion |
| **Gemini AI (Optional)** | Simple queries via `/gemini` or MCP-enhanced orchestration via `/gemini-mcp` |

## 🚀 Getting Started

### Prerequisites

- Node.js 18+ (for NPM-based MCP servers)
- Minecraft 1.21.8 with Fabric Loader 0.16.14
- Meteor Client 1.21.8-56
- Java 21 (for building)

### Installation

```bash
# 1. Clone or download the repository
git clone https://github.com/GhostTypes/meteor-mcp-addon.git

# 2. Build the addon
./gradlew clean build

# 3. Copy the jar to your mods folder
cp build/libs/meteor-mcp-0.1.0-fabric.jar ~/.minecraft/mods/

# 4. Launch Minecraft with Meteor Client
```

The MCP tab will appear in the Meteor GUI (Right Shift).

## 📋 Usage

### Connecting MCP Servers

**Via GUI:**
1. Open Meteor GUI (Right Shift) and navigate to the MCP tab
2. Click "Add Server"
3. Enter server name and command (e.g., `npx -y @modelcontextprotocol/server-filesystem /path/to/directory`)
4. Enable "Auto Connect" (optional)
5. Save and Connect

**Common MCP Servers:**

| Server | Command | Use Case |
|--------|---------|----------|
| Filesystem | `npx -y @modelcontextprotocol/server-filesystem /path` | Read/write files, list directories |
| Git | `npx -y @modelcontextprotocol/server-git /repo` | Git operations (status, commit, log) |
| Time | `npx -y @modelcontextprotocol/server-time` | Time zone conversion, current time |

### StarScript Examples

After connecting servers, tools become available in any StarScript context:

```
{fs.read_file("config.json")}
{git.status()}
{time.get_current_time("America/New_York")}
```

Use these expressions in:
- HUD text elements
- Chat macros
- Discord Rich Presence
- Custom Meteor modules

### Chat Command Examples

```bash
# File operations
/fs:read_file path="mods/meteor.json"
/fs:list_directory path="/home/user/minecraft"

# Git operations
/git:status
/git:commit message="Update configuration"

# Time queries
/time:get_current_time timezone="UTC"

# JSON arguments
/database:query {"table":"users","limit":10}

# Get help for any tool
/fs:read_file help
```

## 🤖 Gemini AI Integration (Optional)

### Setup

1. Navigate to MCP tab and click "Configure Gemini API"
2. Enter API key from [ai.google.dev](https://ai.google.dev)
3. Select model (Gemini 2.5 Pro, Flash, or Flash Lite)
4. Test connection and save

### Usage

**Simple prompts:**
```bash
/gemini "Explain what StarScript is"
{gemini("What is the current Minecraft version?")}
```

**MCP-enhanced prompts:**
```bash
/gemini-mcp "Read my config.json and explain each setting"
/gemini-mcp "Check git status and suggest next steps"
{gemini_mcp("Get the current time in Tokyo")}
```

The `/gemini-mcp` command allows Gemini to automatically discover and call any connected MCP tool. Tool usage is reported in the response.

## 🔧 Technical Details

**Architecture:**
- **Transport**: STDIO fully implemented (SSE/HTTP planned)
- **Dependencies**: MCP Java SDK, Reactor Core, Google GenAI SDK (all shaded into jar)
- **Execution**: Thread-safe, async tool execution with automatic type conversion
- **Storage**: Persistent configuration via Meteor's NBT system

**Supported Argument Formats:**
- Positional: `/tool arg1 arg2 arg3`
- Named: `/tool key1=value1 key2=value2`
- JSON: `/tool {"key": "value", "nested": {...}}`

**Rate Limiting:**
- 5-second reconnect cooldown on MCP server failures
- Per-player cooldown on Gemini chat commands

## 📚 Resources

- [MCP Specification](https://modelcontextprotocol.io/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client)
- [StarScript Documentation](https://github.com/MeteorDevelopment/starscript)
- [Gemini API](https://ai.google.dev/gemini-api/docs)

## 📄 License

This project is licensed under CC0 1.0 Universal (Public Domain). See [LICENSE](LICENSE) for details.

---

**Author:** Cope | **Version:** 0.1.0 | **Mod ID:** meteor-mcp
