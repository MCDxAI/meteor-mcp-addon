package com.cope.meteormcp.systems;

import com.cope.meteormcp.MeteorMCPAddon;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.time.Duration;
import java.util.*;

import io.modelcontextprotocol.json.McpJsonMapper;

/**
 * Wrapper around MCP Java SDK Client.
 * Manages connection lifecycle and provides tool execution interface.
 *
 * @author GhostTypes
 */
public class MCPServerConnection {
    private final MCPServerConfig config;
    private McpSyncClient client;
    private McpClientTransport transport;
    private boolean connected;
    private List<Tool> tools;
    private long lastConnectAttempt;
    private static final long RECONNECT_COOLDOWN = 5000; // 5 seconds

    /**
     * Creates a new connection wrapper for the supplied configuration. The connection is
     * not opened until {@link #connect()} is invoked, giving callers control over when
     * processes are spawned and resources allocated.
     *
     * @param config user-provided configuration describing how to reach the MCP server
     */
    public MCPServerConnection(MCPServerConfig config) {
        this.config = config;
        this.connected = false;
        this.tools = new ArrayList<>();
        this.lastConnectAttempt = 0;
    }

    /**
     * Attempt to establish a live connection to the configured MCP server.
     *
     * @return {@code true} when the connection and tool discovery succeed, otherwise {@code false}
     */
    public boolean connect() {
        if (connected) {
            return true;
        }

        // Rate limiting
        long now = System.currentTimeMillis();
        if (now - lastConnectAttempt < RECONNECT_COOLDOWN) {
            return false;
        }
        lastConnectAttempt = now;

        try {
            switch (config.getTransport()) {
                case STDIO:
                    return connectStdio();
                case SSE:
                case HTTP:
                    MeteorMCPAddon.LOG.warn("SSE/HTTP transport not yet implemented for {}", config.getName());
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Failed to connect to MCP server {}: {}", config.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Performs the STDIO transport handshake, spawning the target process and loading
     * the available tool metadata.
     *
     * @return {@code true} when the server responds successfully
     */
    private boolean connectStdio() {
        try {
            // Build ServerParameters
            ServerParameters.Builder paramsBuilder = ServerParameters.builder(config.getCommand());

            if (config.getArgs() != null && !config.getArgs().isEmpty()) {
                paramsBuilder.args(config.getArgs());
            }

            if (config.getEnv() != null && !config.getEnv().isEmpty()) {
                paramsBuilder.env(config.getEnv());
            }

            ServerParameters params = paramsBuilder.build();

            // Create STDIO transport
            transport = new StdioClientTransport(params, McpJsonMapper.getDefault());

            // Build sync client
            client = McpClient.sync(transport)
                .requestTimeout(Duration.ofMillis(config.getTimeout()))
                .initializationTimeout(Duration.ofMillis(config.getTimeout()))
                .build();

            // Initialize connection
            client.initialize();

            // List available tools (copy to mutable list)
            ListToolsResult result = client.listTools();
            tools = new ArrayList<>(result.tools());

            connected = true;
            MeteorMCPAddon.LOG.info("Connected to MCP server: {} ({} tools available)",
                config.getName(), tools.size());
            return true;

        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Error connecting to MCP server {}: {}", config.getName(), e.getMessage());
            disconnect();
            return false;
        }
    }

    /**
     * Disconnect from the MCP server and release any processes or network resources. Safe
     * to call multiple times.
     */
    public void disconnect() {
        connected = false;
        tools = new ArrayList<>();

        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                MeteorMCPAddon.LOG.warn("Error closing MCP client: {}", e.getMessage());
            }
            client = null;
        }

        transport = null;

        MeteorMCPAddon.LOG.info("Disconnected from MCP server: {}", config.getName());
    }

    /**
     * Execute a tool on the connected MCP server.
     *
     * @param toolName  identifier returned from {@link #getTools()}
     * @param arguments JSON-serializable argument payload
     * @return structured call result reported by the server
     */
    public CallToolResult callTool(String toolName, Map<String, Object> arguments) {
        if (!connected || client == null) {
            throw new IllegalStateException("Not connected to MCP server");
        }

        try {
            CallToolRequest request = new CallToolRequest(toolName, arguments);
            return client.callTool(request);
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Error calling tool {} on server {}: {}",
                toolName, config.getName(), e.getMessage());
            throw new RuntimeException("Tool call failed", e);
        }
    }

    /**
     * Convenience helper that tears down the current connection and immediately attempts
     * to reconnect. Rate limits defined in {@link #connect()} still apply.
     *
     * @return {@code true} when the reconnection handshake succeeds
     */
    public boolean reconnect() {
        disconnect();
        return connect();
    }

    // Getters
    public MCPServerConfig getConfig() { return config; }
    public boolean isConnected() { return connected; }
    public List<Tool> getTools() { return new ArrayList<>(tools); }

    /**
     * Look up a tool that was advertised by the remote server.
     *
     * @param name identifier returned by {@link #getTools()}
     * @return tool metadata or {@code null} when the tool is unknown
     */
    public Tool getTool(String name) {
        return tools.stream()
            .filter(tool -> tool.name().equals(name))
            .findFirst()
            .orElse(null);
    }
}
