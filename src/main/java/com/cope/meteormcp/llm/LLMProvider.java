package com.cope.meteormcp.llm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstraction over AI providers (Gemini, Ollama, etc.).
 * All downstream consumers (commands, StarScript, GUI) interact through this interface.
 */
public interface LLMProvider {

    /** Human-readable provider name (e.g. "Gemini", "Ollama"). */
    String name();

    /** Whether the provider has valid configuration and is ready to use. */
    boolean isConfigured();

    /** Execute a simple text prompt without MCP tool access. */
    String executeSimplePrompt(String prompt);

    /** Execute a prompt with access to connected MCP server tools. */
    MCPResult executeWithMCPTools(String prompt, Set<String> serverNames);

    /** Test the provider connection/credentials. */
    TestResult testConnection();

    // ---- Shared result types ----

    record MCPResult(String response, List<ToolCallInfo> toolCalls) {
        public MCPResult(String response, List<ToolCallInfo> toolCalls) {
            this.response = response;
            this.toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        }
    }

    record TestResult(boolean success, String message) {}

    record ToolCallInfo(
        String serverName,
        String toolName,
        Map<String, Object> arguments,
        long durationMs,
        boolean success,
        String errorMessage
    ) {
        public ToolCallInfo {
            arguments = arguments == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        }
    }
}
