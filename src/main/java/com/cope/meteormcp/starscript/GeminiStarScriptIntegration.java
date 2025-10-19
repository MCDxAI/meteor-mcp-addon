package com.cope.meteormcp.starscript;

import com.cope.meteormcp.gemini.GeminiExecutor;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.value.Value;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers Gemini helper functions into the global StarScript namespace.
 */
public final class GeminiStarScriptIntegration {
    private GeminiStarScriptIntegration() {
    }

    public static void register() {
        MeteorStarscript.ss.set("gemini", createGeminiFunction());
        MeteorStarscript.ss.set("gemini_mcp", createGeminiMcpFunction());
    }

    private static Value createGeminiFunction() {
        return Value.function((ss, argCount) -> {
            String prompt = extractPrompt(ss, argCount);
            String response = GeminiExecutor.executeSimplePrompt(prompt);
            return Value.string(response);
        });
    }

    private static Value createGeminiMcpFunction() {
        return Value.function((ss, argCount) -> {
            String prompt = extractPrompt(ss, argCount);
            Set<String> servers = collectConnectedServers();
            String response = GeminiExecutor.executeWithMCPTools(prompt, servers);
            return Value.string(response);
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
