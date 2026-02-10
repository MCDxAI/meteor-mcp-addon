package com.cope.meteormcp.llm;

import com.cope.meteormcp.gemini.GeminiClientManager;
import com.cope.meteormcp.gemini.GeminiExecutor;

import java.util.List;
import java.util.Set;

/**
 * LLMProvider implementation that delegates to the existing Gemini integration.
 */
public final class GeminiProvider implements LLMProvider {

    @Override
    public String name() {
        return "Gemini";
    }

    @Override
    public boolean isConfigured() {
        return GeminiClientManager.getInstance().isConfigured();
    }

    @Override
    public String executeSimplePrompt(String prompt) {
        return GeminiExecutor.executeSimplePrompt(prompt);
    }

    @Override
    public MCPResult executeWithMCPTools(String prompt, Set<String> serverNames) {
        GeminiExecutor.GeminiMCPResult geminiResult = GeminiExecutor.executeWithMCPToolsDetailed(prompt, serverNames);

        // Convert Gemini-specific ToolCallInfo to shared ToolCallInfo
        List<ToolCallInfo> toolCalls = geminiResult.toolCalls().stream()
            .map(tc -> new ToolCallInfo(
                tc.serverName(), tc.toolName(), tc.arguments(),
                tc.durationMs(), tc.success(), tc.errorMessage()
            ))
            .toList();

        return new MCPResult(geminiResult.response(), toolCalls);
    }

    @Override
    public TestResult testConnection() {
        GeminiClientManager.TestResult result = GeminiClientManager.getInstance()
            .testConfiguration(com.cope.meteormcp.systems.MCPServers.get().getGeminiConfig());
        return new TestResult(result.success(), result.message());
    }
}
