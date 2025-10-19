package com.cope.meteormcp.gemini;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.systems.GeminiConfig;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import com.google.genai.Client;
import com.google.genai.types.AutomaticFunctionCallingConfig;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes Gemini API requests, bridging responses to MCP tool invocations when required.
 */
public final class GeminiExecutor {
    private static final int MAX_TOOL_CALL_ITERATIONS = 6;

    private GeminiExecutor() {
    }

    public static String executeSimplePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "Warning: prompt is required.";
        }

        GeminiClientManager manager = GeminiClientManager.getInstance();
        if (!manager.isConfigured()) {
            return "Warning: Gemini is not configured.";
        }

        try {
            Client client = manager.getClient();
            GeminiConfig config = MCPServers.get().getGeminiConfig();
            GenerateContentConfig request = GeminiClientManager.createBaseRequestConfig(config).build();
            GenerateContentResponse response = client.models.generateContent(config.getModelId(), prompt, request);
            return extractResponseText(response);
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Gemini prompt failed: {}", e.getMessage());
            return formatError(e);
        }
    }

    public static String executeWithMCPTools(String prompt, Set<String> serverNames) {
        if (prompt == null || prompt.isBlank()) {
            return "Warning: prompt is required.";
        }

        GeminiClientManager manager = GeminiClientManager.getInstance();
        if (!manager.isConfigured()) {
            return "Warning: Gemini is not configured.";
        }

        GeminiConfig config = MCPServers.get().getGeminiConfig();
        Map<String, MCPServerConnection> connections = new HashMap<>();
        List<FunctionDeclaration> functionDeclarations = new ArrayList<>();

        collectTools(serverNames, connections, functionDeclarations);

        if (functionDeclarations.isEmpty()) {
            return executeSimplePrompt(prompt);
        }

        try {
            Client client = manager.getClient();
            GenerateContentConfig.Builder requestBuilder = GeminiClientManager
                .createBaseRequestConfig(config)
                .tools(List.of(com.google.genai.types.Tool.builder()
                    .functionDeclarations(functionDeclarations)
                    .build()))
                .automaticFunctionCalling(AutomaticFunctionCallingConfig.builder()
                    .disable(true)
                    .build());

            GenerateContentConfig request = requestBuilder.build();
            List<Content> history = new ArrayList<>();
            history.add(Content.fromParts(Part.fromText(prompt)));

            for (int iteration = 0; iteration < MAX_TOOL_CALL_ITERATIONS; iteration++) {
                GenerateContentResponse response = client.models.generateContent(
                    config.getModelId(),
                    history,
                    request
                );

                if (response == null) {
                    return "Warning: Gemini returned no response.";
                }

                List<FunctionCall> functionCalls = response.functionCalls();
                if (functionCalls == null || functionCalls.isEmpty()) {
                    return extractResponseText(response);
                }

                response.candidates().ifPresent(candidates -> {
                    if (!candidates.isEmpty()) {
                        candidates.get(0).content().ifPresent(history::add);
                    }
                });

                boolean executed = false;
                for (FunctionCall call : functionCalls) {
                    String callName = call.name().orElse("");
                    if (callName.isEmpty()) continue;

                    MCPToGeminiBridge.ToolCallRoute route;
                    try {
                        route = MCPToGeminiBridge.resolveRoute(callName);
                    } catch (IllegalArgumentException ex) {
                        MeteorMCPAddon.LOG.warn("Gemini requested unknown function '{}'", callName);
                        history.add(buildFunctionErrorResponse(callName, "Unknown function requested: " + callName));
                        continue;
                    }

                    MCPServerConnection connection = connections.get(route.serverName());
                    if (connection == null || !connection.isConnected()) {
                        history.add(buildFunctionErrorResponse(callName,
                            "Server '" + route.serverName() + "' is not connected."));
                        continue;
                    }

                    Map<String, Object> arguments = safeArguments(call.args().orElse(Collections.emptyMap()));
                    Map<String, Object> toolResult = executeMCPTool(connection, route.toolName(), arguments);
                    history.add(Content.builder()
                        .parts(List.of(Part.fromFunctionResponse(callName, toolResult)))
                        .build());
                    executed = true;
                }

                if (!executed) {
                    return "Warning: Gemini could not execute any MCP tools.";
                }
            }

            return "Warning: Gemini did not finish after multiple tool calls.";
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Gemini MCP execution failed: {}", e.getMessage());
            return formatError(e);
        }
    }

    private static void collectTools(
        Collection<String> serverNames,
        Map<String, MCPServerConnection> connections,
        List<FunctionDeclaration> declarations
    ) {
        if (serverNames == null || serverNames.isEmpty()) {
            for (MCPServerConnection connection : MCPServers.get().getAllConnections()) {
                addToolsForConnection(connection, connections, declarations);
            }
            return;
        }

        for (String serverName : serverNames) {
            if (serverName == null) continue;
            MCPServerConnection connection = MCPServers.get().getConnection(serverName);
            addToolsForConnection(connection, connections, declarations);
        }
    }

    private static void addToolsForConnection(
        MCPServerConnection connection,
        Map<String, MCPServerConnection> connections,
        List<FunctionDeclaration> declarations
    ) {
        if (connection == null || !connection.isConnected()) {
            return;
        }

        connections.put(connection.getConfig().getName(), connection);
        for (Tool tool : connection.getTools()) {
            try {
                declarations.add(MCPToGeminiBridge.convertMCPToolToGemini(
                    tool,
                    connection.getConfig().getName()
                ));
            } catch (Exception ex) {
                MeteorMCPAddon.LOG.warn("Failed to convert MCP tool {} from {}: {}",
                    tool.name(), connection.getConfig().getName(), ex.getMessage());
            }
        }
    }

    private static Map<String, Object> executeMCPTool(
        MCPServerConnection connection,
        String toolName,
        Map<String, Object> arguments
    ) {
        try {
            CallToolResult result = connection.callTool(toolName, arguments);
            return formatToolResult(result);
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Error executing tool {} on {}: {}",
                toolName, connection.getConfig().getName(), e.getMessage());
            return Map.of(
                "error", true,
                "message", e.getMessage() != null ? e.getMessage() : "Tool execution failed."
            );
        }
    }

    private static Map<String, Object> formatToolResult(CallToolResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (result == null) {
            payload.put("message", "Tool returned no result.");
            return payload;
        }

        if (Boolean.TRUE.equals(result.isError())) {
            payload.put("error", true);
        }

        if (result.structuredContent() != null) {
            payload.put("structuredContent", result.structuredContent());
        }

        String contentText = flattenContent(result.content());
        if (!contentText.isBlank()) {
            payload.put("content", contentText);
        }

        if (result.meta() != null && !result.meta().isEmpty()) {
            payload.put("meta", result.meta());
        }

        if (payload.isEmpty()) {
            payload.put("message", "Tool completed without returning data.");
        }

        return payload;
    }

    private static String flattenContent(List<McpSchema.Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : contents) {
            if (content instanceof McpSchema.TextContent textContent && textContent.text() != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(textContent.text());
            } else {
                if (sb.length() > 0) sb.append('\n');
                sb.append(content.type());
            }
        }
        return sb.toString().trim();
    }

    private static Map<String, Object> safeArguments(Map<String, Object> args) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (args == null) return sanitized;
        args.forEach((key, value) -> {
            if (key == null) return;
            sanitized.put(key, value);
        });
        return sanitized;
    }

    private static Content buildFunctionErrorResponse(String functionName, String message) {
        Map<String, Object> errorPayload = Map.of(
            "error", true,
            "message", message
        );
        return Content.builder()
            .parts(List.of(Part.fromFunctionResponse(functionName, errorPayload)))
            .build();
    }

    private static String extractResponseText(GenerateContentResponse response) {
        if (response == null) {
            return "Warning: Gemini returned no response.";
        }

        String text = response.text();
        if (text != null && !text.isBlank()) {
            return text.trim();
        }

        var parts = response.parts();
        if (parts != null && !parts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Part part : parts) {
                part.text().ifPresent(value -> {
                    if (value.isBlank()) return;
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(value.trim());
                });
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }

        return "Warning: Gemini returned no text.";
    }

    private static String formatError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return "Warning: Gemini request failed: " + message;
    }
}
