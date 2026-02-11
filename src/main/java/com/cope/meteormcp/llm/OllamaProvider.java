package com.cope.meteormcp.llm;

import com.cope.meteormcp.ollama.OllamaClientManager;
import com.cope.meteormcp.ollama.OllamaExecutor;
import com.cope.meteormcp.systems.MCPServers;

import java.util.Set;

/**
 * LLMProvider implementation backed by a local Ollama server.
 */
public final class OllamaProvider implements LLMProvider {

    @Override
    public String name() {
        return "Ollama";
    }

    @Override
    public boolean isConfigured() {
        return OllamaClientManager.getInstance().isConfigured();
    }

    @Override
    public String executeSimplePrompt(String prompt) {
        return OllamaExecutor.executeSimplePrompt(prompt);
    }

    @Override
    public MCPResult executeWithMCPTools(String prompt, Set<String> serverNames) {
        return OllamaExecutor.executeWithMCPToolsDetailed(prompt, serverNames);
    }

    @Override
    public TestResult testConnection() {
        OllamaClientManager.TestResult result = OllamaClientManager.getInstance()
            .quickTest(MCPServers.get().getAIConfig().getOllamaConfig());
        return new TestResult(result.success(), result.message());
    }
}
