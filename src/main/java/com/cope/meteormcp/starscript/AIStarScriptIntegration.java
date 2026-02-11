package com.cope.meteormcp.starscript;

import com.cope.meteormcp.llm.LLMProvider;
import com.cope.meteormcp.llm.LLMProviderManager;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.value.Value;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers AI helper functions into the global StarScript namespace.
 * Routes through the active LLM provider (Gemini or Ollama).
 */
public final class AIStarScriptIntegration {
    private AIStarScriptIntegration() {}

    public static void register() {
        MeteorStarscript.ss.set("ai", createAIFunction());
        MeteorStarscript.ss.set("ai_mcp", createAIMcpFunction());
    }

    private static Value createAIFunction() {
        return Value.function((ss, argCount) -> {
            String prompt = extractPrompt(ss, argCount);
            LLMProvider provider = LLMProviderManager.getInstance().getActiveProvider();
            if (provider == null || !provider.isConfigured()) {
                return Value.string("AI not configured");
            }
            String response = provider.executeSimplePrompt(prompt);
            return Value.string(response);
        });
    }

    private static Value createAIMcpFunction() {
        return Value.function((ss, argCount) -> {
            String prompt = extractPrompt(ss, argCount);
            LLMProvider provider = LLMProviderManager.getInstance().getActiveProvider();
            if (provider == null || !provider.isConfigured()) {
                return Value.string("AI not configured");
            }
            Set<String> servers = collectConnectedServers();
            LLMProvider.MCPResult result = provider.executeWithMCPTools(prompt, servers);
            return Value.string(result.response());
        });
    }

    private static String extractPrompt(org.meteordev.starscript.Starscript ss, int argCount) {
        String prompt = "";
        for (int i = 0; i < argCount; i++) {
            Value value = ss.pop();
            if (i == 0 && value != null) {
                prompt = value.isString() ? value.getString() : value.toString();
            }
        }
        return prompt;
    }

    private static Set<String> collectConnectedServers() {
        Set<String> servers = new LinkedHashSet<>();
        for (MCPServerConnection connection : MCPServers.get().getAllConnections()) {
            if (connection != null && connection.isConnected()) {
                servers.add(connection.getConfig().getName());
            }
        }
        return servers;
    }
}
