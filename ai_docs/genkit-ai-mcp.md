# Comprehensive Guide to @genkit-ai/mcp

This document provides a comprehensive, in-depth guide to using the `@genkit-ai/mcp` library. It is designed for developers of all skill levels, from beginners integrating their first Model Context Protocol (MCP) server to advanced users building complex, multi-server AI agents. The content is structured to be easily parsable by AI agents and developers alike.

**Package Information:**
- **Version**: 1.18.0 (latest at time of writing)
- **License**: Apache-2.0
- **Description**: A Genkit plugin that provides interoperability between Genkit and Model Context Protocol (MCP)

---

## Table of Contents

1.  [Overview & Core Concepts](#1-overview--core-concepts)
    * [What is Genkit?](#what-is-genkit)
    * [What is the Model Context Protocol (MCP)?](#what-is-the-model-context-protocol-mcp)
    * [The Role of `@genkit-ai/mcp`](#the-role-of-genkit-aimcp)
    * [Key Terminology](#key-terminology)
2.  [Getting Started](#2-getting-started)
    * [Prerequisites](#prerequisites)
    * [Installation](#installation)
    * [Basic Genkit Setup](#basic-genkit-setup)
3.  [Core API Functions](#3-core-api-functions)
    * [`createMcpHost`](#createmcphost)
        * [Function Signature](#function-signature)
        * [The `McpHostOptions` Object](#the-mcphostoptions-object)
        * [The `mcpServers` Property: The Heart of the Plugin](#the-mcpservers-property-the-heart-of-the-plugin)
        * [Return Value: `GenkitMcpHost`](#return-value-genkitmcphost)
    * [`createMcpClient`](#createmcpclient)
    * [`createMcpServer`](#createmcpserver)
    * [Configuration Options](#configuration-options)
4.  [Connecting to MCP Servers](#4-connecting-to-mcp-servers)
    * [Connecting to Local Servers](#connecting-to-local-servers)
        * [Example: Filesystem Server](#example-filesystem-server)
        * [Example: Git Server](#example-git-server)
    * [Connecting to Remote Servers](#connecting-to-remote-servers)
        * [Example: URL-based Connection](#example-url-based-connection)
    * [Managing Multiple Servers](#managing-multiple-servers)
5.  [Tool Discovery & Execution](#5-tool-discovery--execution)
    * [The `getActiveTools()` Method](#the-getactivetools-method)
    * [How Tools are Namespaced](#how-tools-are-namespaced)
    * [Integrating Tools with an AI Model](#integrating-tools-with-an-ai-model)
    * [The Execution Flow](#the-execution-flow)
6.  [MCP Prompts and Limitations](#6-mcp-prompts-and-limitations)
7.  [Practical Example: A Multi-Tool Discord Bot](#7-practical-example-a-multi-tool-discord-bot)
    * [Scenario Definition](#scenario-definition)
    * [Step 1: Project Setup](#step-1-project-setup)
    * [Step 2: MCP Host Configuration (`mcp-host.ts`)](#step-2-mcp-host-configuration-mcp-hostts)
    * [Step 3: The Main Bot Logic (`bot.ts`)](#step-3-the-main-bot-logic-botts)
    * [Step 4: Running the Bot](#step-4-running-the-bot)
    * [Full Code Example Breakdown](#full-code-example-breakdown)
8.  [Lifecycle Management](#8-lifecycle-management)
    * [Starting the Host](#starting-the-host)
    * [Closing Connections with `close()`](#closing-connections-with-close)
    * [Best Practices for Graceful Shutdown](#best-practices-for-graceful-shutdown)
9.  [API Reference](#9-api-reference)
    * [Interfaces](#interfaces)
        * [`McpHostOptions`](#mcphostoptions)
        * [`McpServerConfig`](#mcpserverconfig)
        * [`LocalMcpServerConfig`](#localmcpserverconfig)
        * [`RemoteMcpServerConfig`](#remotemcpserverconfig)
        * [`GenkitMcpHost`](#genkitmcphost)
    * [Functions](#functions)
        * [`createMcpHost(options: McpHostOptions): GenkitMcpHost`](#createmcphostoptions-mcphostoptions-genkitmcphost)
10. [Troubleshooting & Common Errors](#10-troubleshooting--common-errors)
    * [Error: `TS2559: Type 'any[]' has no properties in common with type 'McpHostOptions'`](#error-ts2559-type-any-has-no-properties-in-common-with-type-mcphostoptions)
    * [Error: Tool Not Found or Namespace Collision](#error-tool-not-found-or-namespace-collision)
    * [Error: Server Process Fails to Start](#error-server-process-fails-to-start)
    * [Debugging Tips](#debugging-tips)
11. [Best Practices](#11-best-practices)
    * [Use Meaningful Namespaces](#use-meaningful-namespaces)
    * [Manage Environment Variables Securely](#manage-environment-variables-securely)
    * [Implement Graceful Shutdown](#implement-graceful-shutdown)
    * [Centralize Host Configuration](#centralize-host-configuration)
    * [Monitor Server Health](#monitor-server-health)

---

## 1. Overview & Core Concepts

This section introduces the fundamental concepts behind Genkit, MCP, and how the `@genkit-ai/mcp` library fits into the ecosystem.

### What is Genkit?

**Genkit** is an open-source framework designed to help developers build, deploy, and manage production-grade AI-powered applications. It provides a structured way to define AI flows, manage prompts, integrate with various models (like Google's Gemini), and define tools that the AI can use to interact with external systems.

### What is the Model Context Protocol (MCP)?

The **Model Context Protocol (MCP)** is a standardized specification for creating and interacting with "tools" or "skills" for AI models. It defines how an AI agent can discover the tools available on a server and how it should call them. An MCP server is essentially a service that exposes a set of capabilities (e.g., interacting with a filesystem, a git repository, or a database) in a way that AI models can understand and use.

### The Role of `@genkit-ai/mcp`

The `@genkit-ai/mcp` library acts as a **bridge** between the Genkit framework and MCP servers. It is a Genkit plugin that allows your Genkit application (like a Discord bot) to act as an **MCP client**.

Its primary responsibilities are:
1.  **Manage Connections:** It handles the lifecycle of connecting to one or more MCP servers, whether they are running locally as child processes or remotely over a network.
2.  **Discover Tools:** It queries the connected servers to find out what tools they offer.
3.  **Standardize Tools:** It converts the discovered MCP tools into standard Genkit `ToolAction` objects.
4.  **Delegate Execution:** When the AI model decides to use an MCP tool, this library routes the request to the correct MCP server for execution and returns the result to the model.

In short, `@genkit-ai/mcp` lets you seamlessly plug a rich ecosystem of external tools into your Genkit-powered AI.

### Key Terminology

-   **MCP Host:** Your application that uses `@genkit-ai/mcp`. It "hosts" the connections to MCP servers.
-   **MCP Server:** A separate process or service that exposes tools via the Model Context Protocol. Examples include `@modelcontextprotocol/server-filesystem` or `@modelcontextprotocol/server-git`.
-   **Namespace:** A unique identifier you assign to each MCP server connection within the host. This prevents name collisions between tools from different servers.
-   **Tool (`ToolAction`):** A Genkit representation of a capability that an AI model can execute. `@genkit-ai/mcp` converts tools from MCP servers into this format.

---

## 2. Getting Started

This section walks you through the initial setup required to use `@genkit-ai/mcp`.

### Prerequisites

-   Node.js (v18 or higher recommended)
-   npm, yarn, or pnpm
-   A Genkit project. If you don't have one, you can initialize it with `npm create genkit@latest`.
-   Familiarity with TypeScript.

### Installation

Install the necessary packages into your Genkit project. You'll need the core `genkit` library, an AI provider plugin (like Google's), and the MCP plugin itself.

```bash
npm install genkit @genkit-ai/google-genai @genkit-ai/mcp
````

You will also need to install the packages for any MCP servers you intend to use. These are typically installed as development dependencies if you are running them locally.

```bash
npm install -D @modelcontextprotocol/server-filesystem @modelcontextprotocol/server-git
```

### Basic Genkit Setup

Ensure you have a basic `genkit.ts` file to initialize the framework and plugins. The MCP host will be a plugin you add here.

```typescript
// src/genkit.ts
import { genkit } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';
import { configureMcpHost } from './mcp-host'; // We will create this file next

// Initialize the MCP host by calling our configuration function
const mcpHost = configureMcpHost();

export default genkit({
  plugins: [
    googleAI(),
    mcpHost, // Register the mcpHost as a Genkit plugin
  ],
  logSinks: ['file', 'console'],
  enableTracingAndMetrics: true,
});
```

-----

## 3. Core API Functions

The `@genkit-ai/mcp` library provides three main functions for different MCP integration scenarios. Understanding these functions and their options is crucial for effective implementation.

### `createMcpHost`

The primary function for connecting to multiple MCP servers simultaneously. This is the most common use case for Discord bots and applications that need access to multiple tool ecosystems.

#### Function Signature

```typescript
import { createMcpHost } from '@genkit-ai/mcp';

function createMcpHost(options: McpHostOptions): GenkitMcpHost;
```

This function takes a single argument: an `options` object of type `McpHostOptions`, and returns an instance of `GenkitMcpHost`, which is a Genkit plugin.

### `createMcpClient`

Creates a connection to a single MCP server, providing individual client instance management. This is useful when you need fine-grained control over a specific server connection.

```typescript
import { createMcpClient } from '@genkit-ai/mcp';

function createMcpClient(options: McpClientOptions): GenkitMcpClient;
```

Use this when you need to:
- Connect to a single, specific MCP server
- Have more control over the client lifecycle
- Build custom tool discovery logic

### `createMcpServer`

Exposes your Genkit tools and prompts as an MCP server, allowing other applications to use your AI capabilities through the MCP protocol.

```typescript
import { createMcpServer } from '@genkit-ai/mcp';

function createMcpServer(options: McpServerOptions): GenkitMcpServer;
```

This enables you to:
- Share your Genkit flows and tools with other MCP clients
- Build modular AI services that can be composed together
- Create reusable AI capabilities across different applications

### Configuration Options

The library supports several configuration options across all functions:

- **`name`**: Identifier for the plugin/server/client instance
- **`version`**: Plugin version specification
- **`rawToolResponses`**: Control response formatting (boolean)
- **`mcpServers`**: Define server connection parameters (for `createMcpHost`)

For most Discord bot applications, `createMcpHost` is the recommended approach as it provides the most flexibility for integrating multiple external tools.

### The `McpHostOptions` Object

This is the main configuration object. Its structure is as follows:

```typescript
interface McpHostOptions {
  name?: string;
  mcpServers: {
    [namespace: string]: McpServerConfig;
  };
}
```

  - **`name`** (optional): A string to name the plugin instance. Defaults to `'mcp'`. Useful for debugging.
  - **`mcpServers`** (required): An object where you define all the MCP servers you want to connect to.

### The `mcpServers` Property: The Heart of the Plugin

This property is **not an array**. It is an object where each **key** is a custom `namespace` string, and each **value** is an `McpServerConfig` object.

```typescript
// Correct Usage
const options = {
  mcpServers: {
    // 'fs' is the namespace
    fs: {
      command: 'npx',
      args: ['-y', '@modelcontextprotocol/server-filesystem', process.cwd()],
    },
    // 'git' is the namespace
    git: {
      command: 'npx',
      args: ['-y', '@modelcontextprotocol/server-git'],
    },
  },
};
```

This design is intentional and powerful. The namespace you provide (`fs`, `git`) is prepended to all tools from that server, preventing conflicts. A `readFile` tool from the filesystem server becomes `fs/readFile`, and a `currentBranch` tool from the git server becomes `git/currentBranch`.

### Return Value: `GenkitMcpHost`

The function returns an object that conforms to the Genkit `Plugin` interface. This object contains the logic for managing server processes, discovering tools, and defining the actions that Genkit will use. It also has a crucial `close()` method for graceful shutdown.

-----

## 4. Connecting to MCP Servers

`@genkit-ai/mcp` supports two primary ways of connecting to servers: as local child processes or via a remote URL.

### Connecting to Local Servers

For local connections, you provide a command and arguments to start the server as a child process. The library manages this process for you. This is ideal for development and for tools that need access to the local machine's context (like the filesystem).

The configuration object for a local server is `LocalMcpServerConfig`:

```typescript
interface LocalMcpServerConfig {
  command: string;
  args?: string[];
  options?: SpawnOptions; // from child_process
}
```

#### Example: Filesystem Server

This server provides tools to read, write, and list files. `process.cwd()` is passed as an argument to set the server's working directory to your project's root.

```typescript
// In mcpServers object
fileSystem: {
  command: 'npx',
  args: ['-y', '@modelcontextprotocol/server-filesystem', process.cwd()],
}
```

#### Example: Git Server

This server provides tools to interact with a git repository (e.g., check status, list branches).

```typescript
// In mcpServers object
git: {
  command: 'npx',
  args: ['-y', '@modelcontextprotocol/server-git'],
}
```

### Connecting to Remote Servers

For remote connections, you provide the URL of a running MCP server. This is useful for connecting to shared services or third-party APIs exposed via MCP.

The configuration object for a remote server is `RemoteMcpServerConfig`:

```typescript
interface RemoteMcpServerConfig {
  url: string;
}
```

#### Example: URL-based Connection

```typescript
// In mcpServers object
sharedApi: {
  url: '[https://api.example.com/mcp](https://api.example.com/mcp)',
}
```

### Managing Multiple Servers

To connect to multiple servers, simply add more entries to the `mcpServers` object, ensuring each has a unique namespace.

```typescript
const mcpHost = createMcpHost({
  name: 'myMultiServerHost',
  mcpServers: {
    // Local server for file operations
    fs: {
      command: 'npx',
      args: ['-y', '@modelcontextprotocol/server-filesystem', process.cwd()],
    },
    // Local server for git operations
    git: {
      command: 'npx',
      args: ['-y', '@modelcontextprotocol/server-git'],
    },
    // Remote server for a custom API
    billingApi: {
      url: 'https://internal-billing-service/mcp',
    }
  }
});
```

-----

## 5. Tool Discovery & Execution

Once the host is configured, the next step is to make the tools available to your AI model.

### The `getActiveTools()` Method

The `GenkitMcpHost` instance returned by `createMcpHost` has a method called `getActiveTools()`.

```typescript
async getActiveTools(ai: AIToolkit): Promise<ToolAction<any, any>[]>;
```

This asynchronous method connects to all configured MCP servers, fetches their tool definitions, converts them into Genkit `ToolAction` objects, and returns them in a single array. This process happens dynamically, so if a server's tools change, they will be reflected the next time this method is called.

**CRITICAL INSIGHT: Server Association with Tools**

When `getActiveTools()` returns tools, they are automatically namespaced with their server name. Each `ToolAction` object has:
- `name` property: Contains the namespaced tool name (e.g., `"filesystem/readFile"`)
- `description` property: The tool's description
- `inputSchema` property: The tool's input schema
- `run()` method: Function to execute the tool

**There is NO separate server field or property** - the server association is embedded in the tool name itself using the namespace format: `{serverName}/{toolName}`.

### How Tools are Namespaced

As mentioned earlier, namespaces are critical. When `getActiveTools()` runs, it combines the namespace with the tool name from the server.

  - Server Namespace: `fs`
  - Tool on Server: `readFile`
  - Resulting Genkit Tool Name: `fs/readFile`

This ensures that you can use multiple servers without tool name conflicts.

### Extracting Server Information from Tool Names

To determine which server a tool belongs to, you must parse the tool name:

```typescript
// Example from actual codebase implementation
function extractServerName(toolName: string): string {
  // Tools are namespaced like 'filesystem/readFile'
  const parts = toolName.split('/');
  if (parts.length > 1) {
    return parts[0]; // Returns "filesystem"
  }
  return 'unknown';
}

function extractToolName(toolName: string): string {
  // Extract tool name from namespaced name
  const parts = toolName.split('/');
  return parts.length > 1 ? parts.slice(1).join('/') : toolName; // Returns "readFile"
}
```

### Tool Discovery Pattern

```typescript
// Get all tools from all servers
const mcpTools = await mcpHost.getActiveTools(ai);

// Tools are returned as an array of ToolAction objects:
// [
//   { name: "filesystem/readFile", description: "Read file contents", ... },
//   { name: "filesystem/writeFile", description: "Write file contents", ... },
//   { name: "memory/store", description: "Store data in memory", ... },
//   { name: "memory/retrieve", description: "Retrieve stored data", ... }
// ]

// To organize by server:
const toolsByServer = mcpTools.reduce((acc, tool) => {
  const serverName = tool.name.split('/')[0];
  if (!acc[serverName]) acc[serverName] = [];
  acc[serverName].push(tool);
  return acc;
}, {} as Record<string, ToolAction[]>);
```

### NO Per-Server Tool Discovery

**Important**: There is NO built-in way to get tools from a specific server only. The `getActiveTools()` method always returns tools from ALL configured and connected servers. If you need server-specific tools, you must filter the results based on the namespaced tool names.

### Integrating Tools with an AI Model

The array of tools returned by `getActiveTools()` is passed directly into the `tools` property of the `ai.generate()` call in Genkit.

```typescript
import { genkit } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';

async function runFlow(prompt: string) {
  // Assume mcpHost is configured and initialized
  const mcpTools = await mcpHost.getActiveTools(googleAI);

  const llmResponse = await genkit.generate({
    model: googleAI.model('gemini-1.5-pro-latest'),
    prompt: prompt,
    tools: mcpTools, // Pass the discovered tools to the model
  });

  return llmResponse.text();
}
```

### The Execution Flow

1.  You call `genkit.generate()` with the prompt and the MCP tools.
2.  The LLM analyzes the prompt and decides if it needs to use one of the provided tools.
3.  If it decides to use `fs/readFile`, the Genkit framework invokes the corresponding `ToolAction`.
4.  The action, defined by the `@genkit-ai/mcp` library, knows that this tool belongs to the `fs` server.
5.  It sends the execution request (function name and arguments) to the appropriate MCP server (local or remote).
6.  The MCP server executes the function (e.g., reads the file from disk).
7.  The server returns the result to the `@genkit-ai/mcp` host.
8.  The host passes the result back to the Genkit framework, which then sends it back to the LLM for the final response.

### Multiple Server Management

When working with multiple MCP servers, the `createMcpHost` function manages all connections in a single host instance:

```typescript
// Configuration for multiple servers
const mcpHost = createMcpHost({
  name: 'myMultiServerHost',
  mcpServers: {
    // Each key becomes the namespace for tools from that server
    filesystem: {
      command: 'npx',
      args: ['-y', '@modelcontextprotocol/server-filesystem', process.cwd()],
    },
    memory: {
      command: 'npx', 
      args: ['-y', '@modelcontextprotocol/server-memory'],
    },
    database: {
      url: 'https://my-database-mcp-server.com/mcp',
    }
  }
});

// Single call gets tools from ALL servers
const allTools = await mcpHost.getActiveTools(ai);
// Result: [
//   { name: "filesystem/readFile", ... },
//   { name: "filesystem/writeFile", ... },
//   { name: "memory/store", ... },
//   { name: "database/query", ... }
// ]
```

### Tool Execution Patterns

When you need to execute a specific tool, you can either:

1. **Direct execution via ToolAction** (for programmatic use):
```typescript
const allTools = await mcpHost.getActiveTools(ai);
const readFileTool = allTools.find(tool => tool.name === 'filesystem/readFile');
if (readFileTool) {
  const result = await readFileTool.run({ path: '/path/to/file.txt' });
}
```

2. **AI-driven execution** (standard Genkit pattern):
```typescript
const response = await ai.generate({
  prompt: "Read the contents of package.json",
  tools: allTools, // AI will select and call appropriate tool
});
```

### Server Status and Health Monitoring

The MCP host doesn't provide built-in server health monitoring, but you can implement your own:

```typescript
// Example from production codebase
class McpServerManager {
  private connectionStatus: Map<string, boolean> = new Map();
  
  async healthCheck(): Promise<Record<string, 'healthy' | 'unhealthy' | 'disconnected'>> {
    const status: Record<string, 'healthy' | 'unhealthy' | 'disconnected'> = {};
    
    for (const [serverName, isConnected] of this.connectionStatus) {
      if (!isConnected) {
        status[serverName] = 'disconnected';
        continue;
      }
      
      try {
        // Attempt to get tools to verify server health
        const tools = await this.mcpHost.getActiveTools(ai);
        const serverTools = tools.filter(tool => tool.name.startsWith(`${serverName}/`));
        status[serverName] = serverTools.length > 0 ? 'healthy' : 'unhealthy';
      } catch (error) {
        status[serverName] = 'unhealthy';
      }
    }
    
    return status;
  }
}

-----

## 6. MCP Prompts and Limitations

The `@genkit-ai/mcp` library supports MCP prompts, but with specific limitations that developers need to understand for proper implementation.

### MCP Prompt Support

MCP prompts are predefined prompt templates that MCP servers can expose, similar to how they expose tools. These can be discovered and used by AI models for consistent prompt formatting and behavior.

### Key Limitations

When working with MCP prompts in the `@genkit-ai/mcp` library, be aware of these restrictions:

#### 1. Parameter Types
- **Only string parameters are supported**
- Complex objects, arrays, or other data types are not supported
- All prompt parameters must be passed as string values

#### 2. Message Types
- **Only `user` and `model` messages are supported**
- System messages, function messages, or custom message types are not supported
- The conversation flow is limited to these two role types

#### 3. Message Content
- **Single message type per message**
- Mixed content types within a single message are not supported
- Each message must contain only one type of content (text, image, etc.)

### Example MCP Prompt Usage

```typescript
// Correct: Using string parameters only
const promptResult = await mcpHost.executePrompt('serverName', 'promptName', {
  userInput: 'Hello, world!',
  context: 'conversation context as string',
  language: 'en'
});

// Incorrect: Complex parameters not supported
const promptResult = await mcpHost.executePrompt('serverName', 'promptName', {
  userInput: 'Hello, world!',
  context: { // ❌ Objects not supported
    userId: 123,
    channelId: 'abc'
  },
  options: ['option1', 'option2'] // ❌ Arrays not supported
});
```

### Best Practices for MCP Prompts

1. **Keep Parameters Simple**: Always use string values for prompt parameters
2. **Serialize Complex Data**: Convert objects/arrays to JSON strings if needed
3. **Validate Message Flow**: Ensure your conversation only uses `user` and `model` roles
4. **Test Prompt Compatibility**: Verify that your MCP server's prompts work within these constraints

### Impact on Discord Bot Implementation

These limitations mean that when building Discord bots with MCP prompts:

- User context (IDs, channel info) must be passed as strings
- Conversation history needs to be serialized appropriately
- Complex bot state should be managed outside of MCP prompts
- Tool calls remain the primary mechanism for complex operations

Understanding these limitations helps avoid runtime errors and ensures smooth integration between your Discord bot and MCP servers.

-----

## 6A. Key Answers to Common Questions

### Q: How does createMcpHost work with multiple servers?

**Answer**: `createMcpHost` takes a configuration object with a `mcpServers` property that is an **object** (not array) where each key is a namespace and each value is a server configuration:

```typescript
const mcpHost = createMcpHost({
  name: 'myHost',
  mcpServers: {
    // Key = namespace, Value = server config
    filesystem: { command: 'npx', args: [...] },
    memory: { command: 'npx', args: [...] },
    database: { url: 'https://...' }
  }
});
```

Each server runs independently and contributes its tools to the collective pool, with automatic namespacing to prevent conflicts.

### Q: How should getActiveTools() return tools? Are they namespaced?

**Answer**: `getActiveTools()` returns an array of `ToolAction` objects where tools are **automatically namespaced**:
- Each tool's `name` property contains the format: `{serverNamespace}/{originalToolName}`
- Example: `"filesystem/readFile"`, `"memory/store"`, `"database/query"`
- There is **NO separate server field** - server association is embedded in the tool name itself

### Q: Is there a way to get tools per server instead of all tools at once?

**Answer**: **NO** - There is no built-in method to get tools from a specific server only. `getActiveTools()` always returns tools from ALL connected servers. To filter by server, you must parse the tool names:

```typescript
const allTools = await mcpHost.getActiveTools(ai);

// Filter tools by server
const filesystemTools = allTools.filter(tool => tool.name.startsWith('filesystem/'));
const memoryTools = allTools.filter(tool => tool.name.startsWith('memory/'));

// Or organize by server
const toolsByServer = allTools.reduce((acc, tool) => {
  const serverName = tool.name.split('/')[0];
  if (!acc[serverName]) acc[serverName] = [];
  acc[serverName].push(tool);
  return acc;
}, {} as Record<string, ToolAction[]>);
```

### Q: What's the proper way to associate tools with their source servers?

**Answer**: Parse the tool name to extract server information:

```typescript
function getServerFromTool(toolName: string): string {
  return toolName.split('/')[0]; // "filesystem/readFile" → "filesystem"
}

function getToolNameWithoutServer(toolName: string): string {
  const parts = toolName.split('/');
  return parts.slice(1).join('/'); // "filesystem/readFile" → "readFile"
}

// Display tools with server association
const tools = await mcpHost.getActiveTools(ai);
tools.forEach(tool => {
  const serverName = getServerFromTool(tool.name);
  const toolName = getToolNameWithoutServer(tool.name);
  console.log(`Server: ${serverName}, Tool: ${toolName}`);
});
```

### Common Patterns for Tool Management

```typescript
// 1. Get all tools and organize by server for display
async function getToolsByServer(mcpHost: GenkitMcpHost, ai: AIToolkit) {
  const allTools = await mcpHost.getActiveTools(ai);
  
  return allTools.reduce((acc, tool) => {
    const serverName = tool.name.split('/')[0];
    if (!acc[serverName]) {
      acc[serverName] = [];
    }
    acc[serverName].push({
      name: tool.name.split('/').slice(1).join('/'), // Remove server prefix
      fullName: tool.name, // Keep full name for execution
      description: tool.description
    });
    return acc;
  }, {} as Record<string, Array<{name: string, fullName: string, description?: string}>>);
}

// 2. Execute specific tool by server and tool name
async function executeTool(mcpHost: GenkitMcpHost, ai: AIToolkit, serverName: string, toolName: string, params: any) {
  const allTools = await mcpHost.getActiveTools(ai);
  const fullToolName = `${serverName}/${toolName}`;
  const tool = allTools.find(t => t.name === fullToolName);
  
  if (!tool) {
    throw new Error(`Tool ${fullToolName} not found`);
  }
  
  return await tool.run(params);
}
```

**Key Takeaway**: The `@genkit-ai/mcp` library uses namespace-embedded tool names as the primary mechanism for server association. There are no separate methods or properties for server-specific operations - everything is handled through string parsing of the namespaced tool names.

-----

## 7. Practical Example: A Multi-Tool Discord Bot

Let's build a simple bot that can read files and check the current git branch in the project directory.

### Scenario Definition

  - **Goal:** Create a Genkit flow that a Discord bot can call.
  - **Tools Needed:** Filesystem access, Git access.
  - **MCP Servers:** `@modelcontextprotocol/server-filesystem`, `@modelcontextprotocol/server-git`.

### Step 1: Project Setup

Ensure your `package.json` includes the dependencies:

```json
{
  "dependencies": {
    "genkit": "latest",
    "@genkit-ai/google-genai": "latest",
    "@genkit-ai/mcp": "latest"
  },
  "devDependencies": {
    "@modelcontextprotocol/server-filesystem": "latest",
    "@modelcontextprotocol/server-git": "latest"
  }
}
```

### Step 2: MCP Host Configuration (`mcp-host.ts`)

Create a dedicated file to configure and export the MCP host. This keeps your code organized.

```typescript
// src/mcp-host.ts
import { createMcpHost, GenkitMcpHost } from '@genkit-ai/mcp';

let mcpHostInstance: GenkitMcpHost | null = null;

// Use a function to ensure it's a singleton
export function configureMcpHost(): GenkitMcpHost {
  if (mcpHostInstance) {
    return mcpHostInstance;
  }

  console.log('Initializing MCP Host...');
  mcpHostInstance = createMcpHost({
    name: 'discordBotMcpHost',
    mcpServers: {
      // Namespace for filesystem tools
      fs: {
        command: 'npx',
        args: ['-y', '@modelcontextprotocol/server-filesystem', process.cwd()],
      },
      // Namespace for git tools
      git: {
        command: 'npx',
        args: ['-y', '@modelcontextprotocol/server-git'],
      },
    },
  });

  return mcpHostInstance;
}
```

### Step 3: The Main Bot Logic (`bot.ts`)

This file contains the Genkit flow that will be triggered by a Discord message.

```typescript
// src/bot.ts
import { genkit, defineFlow, runFlow } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';
import { configureMcpHost } from './mcp-host';
import * as z from 'zod';

// Initialize Genkit and the MCP Host
const mcpHost = configureMcpHost();

genkit.config({
  plugins: [googleAI(), mcpHost],
  logSinks: ['console'],
});

// Define the main flow for the bot
export const discordBotFlow = defineFlow(
  {
    name: 'discordBotFlow',
    inputSchema: z.string(),
    outputSchema: z.string(),
  },
  async (prompt) => {
    console.log(`Received prompt: "${prompt}"`);

    // Dynamically get the available tools
    const tools = await mcpHost.getActiveTools(googleAI);
    console.log('Available tools:', tools.map(t => t.name));

    const llmResponse = await genkit.generate({
      model: googleAI.model('gemini-1.5-pro-latest'),
      prompt: prompt,
      tools: tools,
    });

    return llmResponse.text();
  }
);

// Example of how to run the flow manually for testing
async function main() {
  const userPrompt = "What is the current git branch and what are the dependencies in package.json?";
  const response = await runFlow(discordBotFlow, userPrompt);
  console.log(`\nFinal Response:\n${response}`);
  
  // IMPORTANT: Clean up server processes
  await mcpHost.close();
  process.exit(0);
}

// Check if the script is run directly to execute the main function
if (require.main === module) {
  main();
}
```

### Step 4: Running the Bot

You can test the flow from the command line:

```bash
npx ts-node src/bot.ts
```

**Expected Output:**

```
Received prompt: "What is the current git branch and what are the dependencies in package.json?"
Initializing MCP Host...
Available tools: [ 'fs/readFile', 'fs/writeFile', 'fs/listFiles', 'git/currentBranch', 'git/status' ]
// ... (tool execution logs) ...

Final Response:
The current git branch is 'main'. The dependencies in package.json are:
{
  "genkit": "latest",
  "@genkit-ai/google-genai": "latest",
  "@genkit-ai/mcp": "latest"
}
```

### Full Code Example Breakdown

  - **`mcp-host.ts`:** Centralizes the `createMcpHost` call. Using a singleton pattern ensures you don't accidentally start multiple sets of server processes.
  - **`bot.ts`:**
      - Imports the configuration and registers the `mcpHost` as a plugin.
      - `discordBotFlow` is the core logic. It's a standard Genkit flow.
      - Crucially, `mcpHost.getActiveTools(googleAI)` is called *inside* the flow. This is best practice as it allows for dynamic tool availability.
      - The `main` function demonstrates how to run the flow and, most importantly, how to call `mcpHost.close()` to terminate the child server processes gracefully.

-----

## 7. Lifecycle Management

Properly managing the lifecycle of the MCP host and its server connections is vital for a stable application.

### Starting the Host

The local MCP server processes are not started when you call `createMcpHost`. They are spawned on-demand the first time `getActiveTools()` is called or a tool from that server is executed. This lazy-loading approach is efficient.

### Closing Connections with `close()`

The `GenkitMcpHost` object has a `close()` method:

```typescript
async close(): Promise<void[]>;
```

This method iterates through all managed local server processes and terminates them gracefully. It is **essential** to call this method when your application is shutting down. Failure to do so will result in orphaned child processes (`npx`, `node`, etc.) that continue to run in the background.

### Best Practices for Graceful Shutdown

In a long-running service like a Discord bot or a web server, you should hook into the process shutdown signals.

```typescript
// In your main application file (e.g., index.ts)
import { mcpHost } from './mcp-host'; // your configured host

function shutdown() {
  console.log('Shutting down gracefully...');
  mcpHost.close().then(() => {
    console.log('MCP host closed.');
    process.exit(0);
  });
}

process.on('SIGINT', shutdown); // Catches Ctrl+C
process.on('SIGTERM', shutdown); // Catches kill signals
```

-----

## 8. API Reference

This section provides a detailed reference for the key interfaces and functions in `@genkit-ai/mcp`.

### Interfaces

#### `McpHostOptions`

The main configuration object for `createMcpHost`.

  - **`name`**: `string` (optional) - An identifier for the plugin instance.
  - **`mcpServers`**: `{[namespace: string]: McpServerConfig;}` (required) - An object mapping string namespaces to server configurations.

#### `McpServerConfig`

A union type representing either a local or remote server config.

```typescript
type McpServerConfig = LocalMcpServerConfig | RemoteMcpServerConfig;
```

#### `LocalMcpServerConfig`

Configuration for a server run as a local child process.

  - **`command`**: `string` (required) - The executable to run (e.g., `'npx'`).
  - **`args`**: `string[]` (optional) - An array of arguments to pass to the command.
  - **`options`**: `SpawnOptions` (optional) - Advanced options from Node.js's `child_process.spawn`.

#### `RemoteMcpServerConfig`

Configuration for connecting to a remote server via HTTP.

  - **`url`**: `string` (required) - The full URL of the remote MCP server endpoint.

#### `GenkitMcpHost`

The object returned by `createMcpHost`. It is a Genkit `Plugin`.

  - **`name`**: `string` - The name of the plugin.
  - **`getActiveTools(ai: AIToolkit)`**: `Promise<ToolAction<any, any>[]>` - Method to discover and return all tools from all configured servers.
  - **`close()`**: `Promise<void[]>` - Method to terminate all managed local server processes.

### Functions

#### `createMcpHost(options: McpHostOptions): GenkitMcpHost`

The primary function of the library.

  - **`options`**: The `McpHostOptions` object to configure the connections.
  - **Returns**: An instance of `GenkitMcpHost` to be used as a Genkit plugin.

-----

## 9. Troubleshooting & Common Errors

Here are solutions to common problems you might encounter.

### Error: `TS2559: Type 'any[]' has no properties in common with type 'McpHostOptions'`

  - **Cause:** This is the most common error for new users. It happens when you pass an array to the `mcpServers` property instead of an object.
  - **Incorrect:**
    ```typescript
    createMcpHost({
      mcpServers: [ // <-- Incorrect, this is an array
        { namespace: 'fs', command: 'npx', ... }
      ]
    });
    ```
  - **Solution:** The `mcpServers` property **must be an object** where the keys are the namespaces.
  - **Correct:**
    ```typescript
    createMcpHost({
      mcpServers: { // <-- Correct, this is an object
        fs: { // <-- The key 'fs' is the namespace
          command: 'npx',
          args: ['...'],
        }
      }
    });
    ```

### Error: Tool Not Found or Namespace Collision

  - **Cause:** The model is trying to call a tool name that doesn't exist, or you have two servers providing a tool with the same name and haven't used namespaces correctly.
  - **Solution:**
    1.  Log the output of `mcpHost.getActiveTools()` at the start of your flow to see the exact names of the available tools (e.g., `fs/readFile`).
    2.  Ensure your prompt is clear enough for the model to choose the correct, namespaced tool name.
    3.  Verify that every key in your `mcpServers` object is unique.

### Error: Server Process Fails to Start

  - **Cause:** The `command` or `args` for a local server are incorrect. This can be due to a missing package or a typo.
  - **Solution:**
    1.  Try running the command directly in your terminal to see the error. For example, run `npx -y @modelcontextprotocol/server-filesystem .` and check for errors.
    2.  Ensure the MCP server package (e.g., `@modelcontextprotocol/server-filesystem`) is installed in your `devDependencies`.
    3.  Check for typos in the command and arguments.

### Debugging Tips

  - **Enable Genkit Tracing:** In your `genkit.config()` or `genkit()` call, set `enableTracingAndMetrics: true`. This will give you detailed logs of each step in the flow, including tool requests and responses.
  - **Inspect Server Logs:** The `@genkit-ai/mcp` library will pipe the `stdout` and `stderr` of local server processes to the main host's console. Look for error messages from the child processes themselves.
  - **Isolate the Problem:** If you have multiple servers, try configuring the host with only one server to confirm it works correctly before adding more.

-----

## 10. Best Practices

Follow these practices to build robust and maintainable applications with `@genkit-ai/mcp`.

### Use Meaningful Namespaces

Avoid generic namespaces like `server1`. Use names that describe the tools' domain, such as `filesystem`, `git`, `database`, `userApi`. This makes tool names self-documenting (e.g., `database/queryUser`).

### Manage Environment Variables Securely

If your MCP servers require API keys or other secrets (passed via `args` or `options.env`), use a library like `dotenv` to load them from a `.env` file and do not commit them to source control.

### Implement Graceful Shutdown

As detailed in the [Lifecycle Management](https://www.google.com/search?q=%237-lifecycle-management) section, always call `mcpHost.close()` during your application's shutdown sequence to prevent orphaned processes. This is critical for server-based applications.

### Centralize Host Configuration

Create a single file (e.g., `mcp-host.ts`) to configure and export your `mcpHost` instance. Import this instance wherever it's needed. This singleton approach prevents accidental re-initialization and makes your server configuration easy to manage.

### Monitor Server Health

For production applications, especially with remote servers, consider implementing a health check mechanism. You could create a simple Genkit flow that periodically calls a basic tool (like a `ping` or `status` tool) on each MCP server to ensure it's responsive.
