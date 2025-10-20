package com.cope.meteormcp.commands;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Dynamic command that proxies an MCP tool through the Meteor command system.
 */
public class MCPToolCommand extends Command {
    private final String serverName;
    private final String toolName;
    private final Tool toolSchema;

    public MCPToolCommand(String serverName, Tool toolSchema) {
        super(
            serverName + ":" + toolSchema.name(),
            Objects.requireNonNullElse(
                toolSchema.description(), "MCP Tool: " + toolSchema.name()
            )
        );
        this.serverName = serverName;
        this.toolName = toolSchema.name();
        this.toolSchema = toolSchema;
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("help").executes(context -> {
            showHelp();
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("args", StringArgumentType.greedyString())
            .suggests(this::suggestArguments)
            .executes(context -> executeTool(context.getArgument("args", String.class)))
        );

        builder.executes(context -> executeTool(""));
    }

    private int executeTool(String argsString) {
        MCPServerConnection connection = MCPServers.get().getConnection(serverName);
        if (connection == null || !connection.isConnected()) {
            error("Server '{}' is not connected.", serverName);
            return 0;
        }

        Map<String, Object> arguments;
        try {
            arguments = CommandUtils.parseArguments(argsString, toolSchema);
        } catch (IllegalArgumentException ex) {
            error("Argument parsing failed: {}", ex.getMessage());
            return 0;
        } catch (Exception ex) {
            error("Argument parsing failed.");
            MeteorMCPAddon.LOG.error("Failed to parse arguments for {}/{}: {}", serverName, toolName, ex.getMessage());
            return 0;
        }

        if (!CommandUtils.validateRequiredParams(arguments, toolSchema)) {
            error("Missing required parameters. Usage: /{} {}", getName(), CommandUtils.generateUsage(toolSchema));
            return 0;
        }

        Map<String, Object> callArgs = new LinkedHashMap<>(arguments);
        info("Executing %s:%s...", serverName, toolName);

        MeteorExecutor.execute(() -> {
            try {
                var result = connection.callTool(toolName, callArgs);
                Runnable deliver = () -> CommandUtils.displayToolResult(MCPToolCommand.this, result);
                if (mc != null) mc.execute(deliver); else deliver.run();
            } catch (Exception e) {
                MeteorMCPAddon.LOG.error("MCP tool command {}:{} failed", serverName, toolName, e);
                Runnable fail = () -> error("Tool execution failed: {}", safeMessage(e));
                if (mc != null) mc.execute(fail); else fail.run();
            }
        });

        return SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestArguments(SuggestionsBuilder builder) {
        JsonSchema schema = toolSchema.inputSchema();
        Map<String, Object> properties = schema != null ? schema.properties() : null;
        if (properties == null || properties.isEmpty()) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String param : properties.keySet()) {
            String suggestion = param + "=";
            if (remaining.isEmpty() || suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestArguments(com.mojang.brigadier.context.CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        return suggestArguments(builder);
    }

    private void showHelp() {
        String description = toolSchema.description();
        if (description == null || description.isBlank()) {
            description = "No description provided.";
        }

        info("{} - {}", getName(), description);
        info("Usage: /{} {}", getName(), CommandUtils.generateUsage(toolSchema));

        JsonSchema schema = toolSchema.inputSchema();
        Map<String, Object> properties = schema != null ? schema.properties() : null;
        if (properties != null && !properties.isEmpty()) {
            info("Parameters:");
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String name = entry.getKey();
                String type = extractType(entry.getValue());
                boolean required = schema.required() != null && schema.required().contains(name);
                String flag = required ? "*" : "-";
                info("  {} {} ({}) {}", flag, name, type, extractDescription(entry.getValue()));
            }
            if (schema.required() != null && !schema.required().isEmpty()) {
                info("* indicates required parameter.");
            }
        }
    }

    private String extractDescription(Object schema) {
        if (schema instanceof Map<?, ?> map) {
            Object description = map.get("description");
            if (description != null) {
                return description.toString();
            }
        }
        return "No description available.";
    }

    private String extractType(Object schema) {
        if (schema instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if (type != null) {
                return type.toString();
            }
        }
        return "any";
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
