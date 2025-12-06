<div align="center">
  <h1>Meteor MCP Addon</h1>
  <p><strong>Model Context Protocol & Gemini integration for Meteor Client</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Meteor_Client-Latest-8a11b6?style=flat" alt="Meteor Client">    
    <img src="https://img.shields.io/badge/Minecraft-1.21.10-00800f?style=flat" alt="Minecraft">
    <img src="https://img.shields.io/badge/Fabric-0.17.3-3d5dff?style=flat" alt="Fabric">
    <img src="https://img.shields.io/badge/Java-21-e28655?style=flat" alt="Java">
  </p>

<p>
    <img src="https://img.shields.io/badge/modelcontextprotocol/java--sdk-0.14.1-e28655?style=flat" alt="MCP">
    <img src="https://img.shields.io/badge/googleapis/java--genai-1.21.0-e28655?style=flat" alt="Gemini">
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
      <td>Automatically registered <code>.serverName:toolName</code> commands with help and tab completion</td>
    </tr>
    <tr>
      <td><strong>Gemini AI (Optional)</strong></td>
      <td>Direct requests with <code>.gemini</code> or MCP-enhanced requests with <code>.gemini-mcp</code></td>
    </tr>
  </table>

  <h2>Usage</h2>

  <h3>Connecting MCP Servers</h3>

  <table>
    <tr><td><strong>Via GUI:</strong></td></tr>
    <tr><td>1. Open Meteor GUI (Right Shift) and navigate to the MCP tab</td></tr>
    <tr><td>2. Click "Add Server"</td></tr>
    <tr><td>3. Enter server name and command (e.g., <code>node /path/to/spotify-mcp-server/dist/index.js</code>)</td></tr>
    <tr><td>4. Enable "Auto Connect" (optional)</td></tr>
    <tr><td>5. Save and Connect</td></tr>
  </table>

  <p><strong>Example Server: Spotify MCP</strong></p>
  <p>For a practical example, see <a href="https://github.com/LLMTooling/spotify-mcp-server">Spotify MCP Server</a> (requires local setup).</p>

  <table>
    <tr>
      <th>Server</th>
      <th>Command</th>
      <th>Use Case</th>
    </tr>
    <tr>
      <td>Spotify</td>
      <td><code>node .../dist/index.js</code></td>
      <td>Control playback, view track info</td>
    </tr>
  </table>

  <h3>StarScript Examples</h3>

  <table>
    <tr><td>After connecting servers, tools become available in any StarScript context:</td></tr>
    <tr><td><code>{spotify.spotify_get_track_name()}</code></td></tr>
    <tr><td><code>{spotify.spotify_get_track_progress_formatted()}</code></td></tr>
    <tr><td>Use these expressions in:</td></tr>
    <tr><td>- HUD text elements</td></tr>
    <tr><td>- Chat macros</td></tr>
    <tr><td>- Anywhere Starscript placeholders are used</td></tr>
  </table>

  <h3>Chat Command Examples</h3>

  <table>
    <tr><td><code># Control playback</code></td></tr>
    <tr><td><code>.spotify:spotify_next</code></td></tr>
    <tr><td><code>.spotify:spotify_set_volume volume_percent=50</code></td></tr>
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
    <tr><td><code>.gemini "Explain what StarScript is"</code></td></tr>
    <tr><td><code>{gemini("What is the current Minecraft version?")}</code></td></tr>
    <tr><td><strong>MCP-enhanced prompts:</strong></td></tr>
    <tr><td><code>.gemini-mcp "Play some jazz music"</code></td></tr>
    <tr><td><code>{gemini_mcp("What song is currently playing?")}</code></td></tr>
    <tr><td>The <code>.gemini-mcp</code> command allows Gemini to automatically discover and call any connected MCP tool. Tool usage is reported in the response.</td></tr>
  </table>
</div>
