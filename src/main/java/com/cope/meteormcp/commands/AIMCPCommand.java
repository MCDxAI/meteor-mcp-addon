package com.cope.meteormcp.commands;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.llm.LLMProvider;
import com.cope.meteormcp.llm.LLMProvider.MCPResult;
import com.cope.meteormcp.llm.LLMProvider.ToolCallInfo;
import com.cope.meteormcp.llm.LLMProviderManager;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;

/**
 * AI prompt command that exposes all connected MCP tools.
 * Routes through the active LLM provider (Gemini or Ollama).
 */
public class AIMCPCommand extends Command {
    public AIMCPCommand() {
        super("ai-mcp", "Query AI with access to all connected MCP tools");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("prompt", StringArgumentType.greedyString())
            .executes(context -> executeAIMCP(context.getArgument("prompt", String.class)))
        );

        builder.executes(context -> {
            error("Prompt is required. Usage: .ai-mcp \"prompt\"");
            return 0;
        });
    }

    private int executeAIMCP(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            error("Prompt is required. Usage: .ai-mcp \"prompt\"");
            return 0;
        }

        LLMProviderManager manager = LLMProviderManager.getInstance();
        if (!manager.isConfigured()) {
            error("AI is not configured. Open Meteor GUI → MCP → Configure AI.");
            return 0;
        }

        if (!AICommand.enforceCooldown(this)) {
            return 0;
        }

        Set<String> connectedServers = MCPServers.get().getAllConnections().stream()
            .filter(MCPServerConnection::isConnected)
            .map(connection -> connection.getConfig().getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (connectedServers.isEmpty()) {
            warning("No MCP servers connected. Running simple AI query.");
            AICommand.runSimpleQueryAsync(this, prompt);
            return SINGLE_SUCCESS;
        }

        LLMProvider provider = manager.getActiveProvider();
        info("Querying %s with %d MCP server(s)...", provider.name(), connectedServers.size());
        MeteorExecutor.execute(() -> {
            try {
                MCPResult result = provider.executeWithMCPTools(prompt, connectedServers);
                info(result.response());
                displayToolUsage(result.toolCalls());
            } catch (Exception e) {
                error("AI MCP query failed: %s", safeMessage(e));
                MeteorMCPAddon.LOG.error("AI MCP command failed", e);
            }
        });

        return SINGLE_SUCCESS;
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private void displayToolUsage(List<ToolCallInfo> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder("[Tools Used] ");
        for (int i = 0; i < toolCalls.size(); i++) {
            ToolCallInfo call = toolCalls.get(i);
            if (i > 0) builder.append(", ");

            builder.append(call.serverName()).append(":").append(call.toolName());

            List<String> annotations = new ArrayList<>();
            if (call.durationMs() > 0) {
                annotations.add(call.durationMs() + "ms");
            }
            if (!call.success()) {
                annotations.add("failed");
            }

            if (!annotations.isEmpty()) {
                builder.append(" (").append(String.join(", ", annotations)).append(")");
            }

            if (!call.success() && call.errorMessage() != null && !call.errorMessage().isBlank()) {
                builder.append(" - ").append(call.errorMessage());
            }
        }

        info(builder.toString());
    }
}
