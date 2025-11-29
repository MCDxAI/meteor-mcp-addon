package com.cope.meteormcp.systems;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a single MCP tool execution request submitted to the request queue.
 * Contains the tool name, arguments, and a future for receiving the result asynchronously.
 *
 * @author GhostTypes
 */
record ToolRequest(
    String toolName,
    Map<String, Object> arguments,
    CompletableFuture<CallToolResult> resultFuture
) {
    /**
     * Creates a new tool request with the given parameters.
     *
     * @param toolName name of the tool to execute
     * @param arguments tool arguments (JSON-serializable map)
     * @param resultFuture future that will receive the result when execution completes
     */
    ToolRequest {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }
        if (resultFuture == null) {
            throw new IllegalArgumentException("Result future cannot be null");
        }
        // Arguments can be null or empty
    }
}
