# Meteor MCP Addon 🔌

**Model Context Protocol & Gemini integration for Meteor Client**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-0.16.14-blue?style=flat-square)
![Meteor](https://img.shields.io/badge/Meteor_Client-1.21.8--56-blueviolet?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![MCP](https://img.shields.io/badge/MCP-0.14.1-red?style=flat-square)

A Meteor Client addon that bridges the Model Context Protocol (MCP) with Minecraft. Connect to MCP servers and call their tools directly from StarScript expressions, chat commands, and optionally via Gemini AI.

## Features

| Feature | Description |
|---------|-------------|
| **MCP Server Management** | Connect multiple MCP servers with persistent configuration and auto-connect on startup |
| **StarScript Integration** | Access tools as `{serverName.toolName(args)}` in HUD elements, chat macros, and anywhere starscript placeholders are used. |
| **Dynamic Chat Commands** | Automatically registered `/serverName:toolName` commands with help and tab completion |
| **Gemini AI (Optional)** | Direct requests withi `/gemini` or MCP-enhanced requests with `/gemini-mcp` |


## 📋 Usage

### Connecting MCP Servers

**Via GUI:**
1. Open Meteor GUI (Right Shift) and navigate to the MCP tab
2. Click "Add Server"
3. Enter server name and command (e.g., `npx -y @modelcontextprotocol/server-time`)
4. Enable "Auto Connect" (optional)
5. Save and Connect

**Common MCP Servers:**

| Server | Command | Use Case |
|--------|---------|----------|
| Time | `npx -y @modelcontextprotocol/server-time` | Time zone conversion, current time |

### StarScript Examples

After connecting servers, tools become available in any StarScript context:

```
{time.get_current_time("America/New_York")}
```

Use these expressions in:
- HUD text elements
- Chat macros
- Anywhere Starscript placeholders are used

### Chat Command Examples

```bash
# Time queries
/time:get_current_time timezone="UTC"
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


