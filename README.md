<div align="center">
  <h1>Meteor MCP Addon</h1>
  <p><strong>Model Context Protocol & Gemini integration for Meteor Client</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Minecraft-1.21.10-green?style=for-the-badge" alt="Minecraft">
    <img src="https://img.shields.io/badge/Fabric-0.17.3-blue?style=for-the-badge" alt="Fabric">
    <img src="https://img.shields.io/badge/Meteor_Client-1.21.10--32-blueviolet?style=for-the-badge" alt="Meteor Client">
    <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge" alt="Java">
    <img src="https://img.shields.io/badge/MCP-0.14.1-red?style=for-the-badge" alt="MCP">
  </p>

  <p>A Meteor Client addon that bridges the Model Context Protocol (MCP) with Minecraft. Connect to MCP servers and call their tools directly from StarScript expressions, chat commands, and optionally via Gemini AI.</p>

  <h2>Features</h2>
  <table>
    <tr>
      <th>Feature</th>
      <th>Description</th>
    </tr>
    <tr>
      <td><strong>MCP Server Management</strong></td>
      <td>Connect multiple MCP servers with persistent configuration and auto-connect on startup</td>
    </tr>
    <tr>
      <td><strong>StarScript Integration</strong></td>
      <td>Access tools as <code>{serverName.toolName(args)}</code> in HUD elements, chat macros, and anywhere starscript placeholders are used.</td>
    </tr>
    <tr>
      <td><strong>Dynamic Chat Commands</strong></td>
      <td>Automatically registered <code>/serverName:toolName</code> commands with help and tab completion</td>
    </tr>
    <tr>
      <td><strong>Gemini AI (Optional)</strong></td>
      <td>Direct requests with <code>/gemini</code> or MCP-enhanced requests with <code>/gemini-mcp</code></td>
    </tr>
  </table>

  <h2>Usage</h2>

  <h3>Connecting MCP Servers</h3>

  <table>
    <tr><td><strong>Via GUI:</strong></td></tr>
    <tr><td>1. Open Meteor GUI (Right Shift) and navigate to the MCP tab</td></tr>
    <tr><td>2. Click "Add Server"</td></tr>
    <tr><td>3. Enter server name and command (e.g., <code>npx -y @modelcontextprotocol/server-time</code>)</td></tr>
    <tr><td>4. Enable "Auto Connect" (optional)</td></tr>
    <tr><td>5. Save and Connect</td></tr>
  </table>

  <p><strong>Common MCP Servers:</strong></p>

  <table>
    <tr>
      <th>Server</th>
      <th>Command</th>
      <th>Use Case</th>
    </tr>
    <tr>
      <td>Time</td>
      <td><code>npx -y @modelcontextprotocol/server-time</code></td>
      <td>Time zone conversion, current time</td>
    </tr>
  </table>

  <h3>StarScript Examples</h3>

  <table>
    <tr><td>After connecting servers, tools become available in any StarScript context:</td></tr>
    <tr><td><code>{time.get_current_time("America/New_York")}</code></td></tr>
    <tr><td>Use these expressions in:</td></tr>
    <tr><td>- HUD text elements</td></tr>
    <tr><td>- Chat macros</td></tr>
    <tr><td>- Anywhere Starscript placeholders are used</td></tr>
  </table>

  <h3>Chat Command Examples</h3>

  <table>
    <tr><td><code># Time queries</code></td></tr>
    <tr><td><code>/time:get_current_time timezone="UTC"</code></td></tr>
  </table>

  <h2>Gemini AI Integration (Optional)</h2>

  <h3>Setup</h3>

  <table>
    <tr><td>1. Navigate to MCP tab and click "Configure Gemini API"</td></tr>
    <tr><td>2. Enter API key from <a href="https://ai.google.dev">ai.google.dev</a></td></tr>
    <tr><td>3. Select model (Gemini 2.5 Pro, Flash, or Flash Lite)</td></tr>
    <tr><td>4. Test connection and save</td></tr>
  </table>

  <h3>Usage</h3>

  <table>
    <tr><td><strong>Simple prompts:</strong></td></tr>
    <tr><td><code>/gemini "Explain what StarScript is"</code></td></tr>
    <tr><td><code>{gemini("What is the current Minecraft version?")}</code></td></tr>
    <tr><td><strong>MCP-enhanced prompts:</strong></td></tr>
    <tr><td><code>/gemini-mcp "Read my config.json and explain each setting"</code></td></tr>
    <tr><td><code>{gemini_mcp("Get the current time in Tokyo")}</code></td></tr>
    <tr><td>The <code>/gemini-mcp</code> command allows Gemini to automatically discover and call any connected MCP tool. Tool usage is reported in the response.</td></tr>
  </table>
</div>
