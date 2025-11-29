package com.cope.meteormcp.commands;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.gemini.GeminiClientManager;
import com.cope.meteormcp.gemini.GeminiExecutor;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.cope.meteormcp.gemini.GeminiExecutor.GeminiMCPResult;
import com.cope.meteormcp.gemini.GeminiExecutor.ToolCallInfo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;

/**
 * Gemini prompt command that exposes all connected MCP tools.
 */
public class GeminiMCPCommand extends Command {
    public GeminiMCPCommand() {
        super("gemini-mcp", "Query Gemini AI with access to all connected MCP tools");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("prompt", StringArgumentType.greedyString())
            .executes(context -> executeGeminiMCP(context.getArgument("prompt", String.class)))
        );

        builder.executes(context -> {
            error("Prompt is required. Usage: .gemini-mcp \"prompt\"");
            return 0;
        });
    }

    private int executeGeminiMCP(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            error("Prompt is required. Usage: .gemini-mcp \"prompt\"");
            return 0;
        }

        if (!GeminiClientManager.getInstance().isConfigured()) {
            error("Gemini is not configured. Open Meteor GUI → MCP → Configure Gemini API.");
            return 0;
        }

        if (!GeminiCommand.enforceCooldown(this)) {
            return 0;
        }

        Set<String> connectedServers = MCPServers.get().getAllConnections().stream()
            .filter(MCPServerConnection::isConnected)
            .map(connection -> connection.getConfig().getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (connectedServers.isEmpty()) {
            warning("No MCP servers connected. Running simple Gemini query.");
            GeminiCommand.runSimpleQueryAsync(this, prompt);
            return SINGLE_SUCCESS;
        }

        info("Querying Gemini with %d MCP server(s)...", connectedServers.size());
        MeteorExecutor.execute(() -> {
            try {
                GeminiMCPResult result = GeminiExecutor.executeWithMCPToolsDetailed(prompt, connectedServers);
                info(result.response());
                displayToolUsage(result.toolCalls());
            } catch (Exception e) {
                error("Gemini MCP query failed: {}", safeMessage(e));
                MeteorMCPAddon.LOG.error("Gemini MCP command failed", e);
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
