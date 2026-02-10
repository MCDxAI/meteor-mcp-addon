package com.cope.meteormcp.ollama;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.llm.LLMProvider.MCPResult;
import com.cope.meteormcp.llm.LLMProvider.ToolCallInfo;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import com.cope.meteormcp.systems.OllamaConfig;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.tools.Tools;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Executes Ollama requests, bridging responses to MCP tool invocations when required.
 * Mirrors the structure of {@link com.cope.meteormcp.gemini.GeminiExecutor}.
 */
public final class OllamaExecutor {

    private static final String SYSTEM_PROMPT =
        "You are a concise assistant inside Minecraft. Keep responses short and to the point — "
        + "a few sentences at most. Do not use markdown formatting.";

    private OllamaExecutor() {}

    /**
     * Execute a simple prompt without MCP tool access.
     */
    public static String executeSimplePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "Warning: prompt is required.";
        }

        OllamaClientManager manager = OllamaClientManager.getInstance();
        if (!manager.isConfigured()) {
            return "Warning: Ollama is not configured.";
        }

        try {
            Ollama api = manager.getClient();
            OllamaConfig config = MCPServers.get().getAIConfig().getOllamaConfig();

            MeteorMCPAddon.LOG.info("[Ollama] Simple prompt: model={}, numCtx={}, numPredict={}, temp={}, keepAlive={}",
                config.getModel(), config.getContextLength(), config.getMaxOutputTokens(), config.getTemperature(), config.getKeepAlive());

            Options options = new OptionsBuilder()
                .setTemperature(config.getTemperature())
                .setNumPredict(config.getMaxOutputTokens())
                .setNumCtx(config.getContextLength())
                .build();

            OllamaChatRequest request = OllamaChatRequest.builder()
                .withModel(config.getModel())
                .withMessage(OllamaChatMessageRole.SYSTEM, SYSTEM_PROMPT)
                .withMessage(OllamaChatMessageRole.USER, prompt)
                .withOptions(options)
                .withKeepAlive(config.getKeepAlive())
                .build();

            long start = System.currentTimeMillis();
            // Pass a no-op token handler to enable streaming — ollama4j's callSync()
            // only breaks on the 'done' flag when stream=true. Without streaming,
            // it reads the entire response line-by-line until EOF which is much slower.
            OllamaChatResult result = api.chat(request, token -> {});
            long elapsed = System.currentTimeMillis() - start;

            String text = result.getResponseModel().getMessage().getResponse();
            logResponseMetrics(result.getResponseModel(), elapsed);

            return (text != null && !text.isBlank()) ? text.trim() : "Warning: Ollama returned no text.";
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Ollama prompt failed: {}", e.getMessage());
            return formatError(e);
        }
    }

    /**
     * Execute a prompt with MCP tool access. Uses ollama4j's built-in tool calling
     * by registering MCP tools as Tools.Tool instances with ToolFunction lambdas.
     */
    public static MCPResult executeWithMCPToolsDetailed(String prompt, Set<String> serverNames) {
        if (prompt == null || prompt.isBlank()) {
            return new MCPResult("Warning: prompt is required.", List.of());
        }

        OllamaClientManager manager = OllamaClientManager.getInstance();
        if (!manager.isConfigured()) {
            return new MCPResult("Warning: Ollama is not configured.", List.of());
        }

        OllamaConfig config = MCPServers.get().getAIConfig().getOllamaConfig();
        Map<String, MCPServerConnection> connections = new HashMap<>();
        List<ToolCallInfo> toolHistory = new CopyOnWriteArrayList<>();

        collectConnections(serverNames, connections);

        if (connections.isEmpty()) {
            return new MCPResult(executeSimplePrompt(prompt), toolHistory);
        }

        try {
            Ollama api = manager.getClient();

            // Clear any previously registered tools
            api.deregisterTools();

            // Register each MCP tool as an Ollama tool
            for (Map.Entry<String, MCPServerConnection> entry : connections.entrySet()) {
                String serverName = entry.getKey();
                MCPServerConnection connection = entry.getValue();

                for (Tool tool : connection.getTools()) {
                    try {
                        Tools.Tool ollamaTool = MCPToOllamaBridge.convertMCPToolToOllama(
                            tool, serverName,
                            args -> {
                                long start = System.currentTimeMillis();
                                try {
                                    Map<String, Object> arguments = args != null ? new LinkedHashMap<>(args) : Map.of();
                                    CallToolResult callResult = connection.callTool(tool.name(), arguments);
                                    long duration = System.currentTimeMillis() - start;

                                    String resultText = flattenContent(callResult);
                                    toolHistory.add(new ToolCallInfo(serverName, tool.name(), arguments,
                                        duration, true, null));

                                    return resultText;
                                } catch (Exception ex) {
                                    long duration = System.currentTimeMillis() - start;
                                    toolHistory.add(new ToolCallInfo(serverName, tool.name(),
                                        args != null ? new LinkedHashMap<>(args) : Map.of(),
                                        duration, false, ex.getMessage()));
                                    return "Error: " + ex.getMessage();
                                }
                            }
                        );
                        api.registerTool(ollamaTool);
                    } catch (Exception ex) {
                        MeteorMCPAddon.LOG.warn("Failed to register Ollama tool {} from {}: {}",
                            tool.name(), serverName, ex.getMessage());
                    }
                }
            }

            MeteorMCPAddon.LOG.info("[Ollama] MCP prompt: model={}, numCtx={}, numPredict={}, temp={}, keepAlive={}, tools={}",
                config.getModel(), config.getContextLength(), config.getMaxOutputTokens(), config.getTemperature(),
                config.getKeepAlive(), api.getRegisteredTools().size());

            Options options = new OptionsBuilder()
                .setTemperature(config.getTemperature())
                .setNumPredict(config.getMaxOutputTokens())
                .setNumCtx(config.getContextLength())
                .build();

            OllamaChatRequest request = OllamaChatRequest.builder()
                .withModel(config.getModel())
                .withMessage(OllamaChatMessageRole.SYSTEM, SYSTEM_PROMPT)
                .withMessage(OllamaChatMessageRole.USER, prompt)
                .withOptions(options)
                .withKeepAlive(config.getKeepAlive())
                .withUseTools(true)
                .build();

            long start = System.currentTimeMillis();
            OllamaChatResult result = api.chat(request, token -> {});
            long elapsed = System.currentTimeMillis() - start;

            String text = result.getResponseModel().getMessage().getResponse();
            MeteorMCPAddon.LOG.info("[Ollama] MCP toolCalls={}", toolHistory.size());
            logResponseMetrics(result.getResponseModel(), elapsed);

            if (text == null || text.isBlank()) {
                text = "Warning: Ollama returned no text.";
            }

            return new MCPResult(text.trim(), toolHistory);
        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Ollama MCP execution failed: {}", e.getMessage());
            return new MCPResult(formatError(e), toolHistory);
        }
    }

    private static void collectConnections(
        Collection<String> serverNames,
        Map<String, MCPServerConnection> connections
    ) {
        if (serverNames == null || serverNames.isEmpty()) {
            for (MCPServerConnection conn : MCPServers.get().getAllConnections()) {
                if (conn != null && conn.isConnected()) {
                    connections.put(conn.getConfig().getName(), conn);
                }
            }
            return;
        }

        for (String name : serverNames) {
            if (name == null) continue;
            MCPServerConnection conn = MCPServers.get().getConnection(name);
            if (conn != null && conn.isConnected()) {
                connections.put(name, conn);
            }
        }
    }

    private static String flattenContent(CallToolResult result) {
        if (result == null) return "Tool returned no result.";

        List<McpSchema.Content> contents = result.content();
        if (contents == null || contents.isEmpty()) return "Tool completed without output.";

        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : contents) {
            if (content instanceof McpSchema.TextContent textContent && textContent.text() != null) {
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(textContent.text());
            }
        }
        return sb.isEmpty() ? "Tool completed without text output." : sb.toString().trim();
    }

    /**
     * Log Ollama's own timing metrics from the response model.
     * Durations are reported by Ollama in nanoseconds.
     */
    private static void logResponseMetrics(OllamaChatResponseModel response, long wallClockMs) {
        if (response == null) {
            MeteorMCPAddon.LOG.info("[Ollama] Response in {}ms (no metrics)", wallClockMs);
            return;
        }

        Long loadNs = response.getLoadDuration();
        Long promptNs = response.getPromptEvalDuration();
        Long evalNs = response.getEvalDuration();
        Integer evalCount = response.getEvalCount();

        String text = response.getMessage() != null ? response.getMessage().getResponse() : null;
        int wordCount = text != null && !text.isBlank() ? text.trim().split("\\s+").length : 0;

        MeteorMCPAddon.LOG.info("[Ollama] Response in {}ms (~{} words) | load={}ms prompt={}ms eval={}ms tokens={}",
            wallClockMs, wordCount,
            loadNs != null ? loadNs / 1_000_000 : "?",
            promptNs != null ? promptNs / 1_000_000 : "?",
            evalNs != null ? evalNs / 1_000_000 : "?",
            evalCount != null ? evalCount : "?");

        if (evalCount != null && evalNs != null && evalNs > 0) {
            double tokensPerSec = evalCount / (evalNs / 1_000_000_000.0);
            MeteorMCPAddon.LOG.info("[Ollama] Generation speed: {} tokens/sec", String.format("%.1f", tokensPerSec));
        }
    }

    private static String formatError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return "Warning: Ollama request failed: " + message;
    }
}
