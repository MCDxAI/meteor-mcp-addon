package com.cope.meteormcp.commands;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.llm.LLMProvider;
import com.cope.meteormcp.llm.LLMProviderManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

/**
 * Simple AI prompt command without MCP tool access.
 * Routes through the active LLM provider (Gemini or Ollama).
 */
public class AICommand extends Command {
    private static final long COOLDOWN_MS = 1000;
    private static final Map<UUID, Long> LAST_CALL_TIME = new ConcurrentHashMap<>();

    public AICommand() {
        super("ai", "Query AI without MCP tools");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("prompt", StringArgumentType.greedyString())
            .executes(context -> executeAI(context.getArgument("prompt", String.class)))
        );

        builder.executes(context -> {
            error("Prompt is required. Usage: .ai \"prompt\"");
            return 0;
        });
    }

    int executeAI(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            error("Prompt is required. Usage: .ai \"prompt\"");
            return 0;
        }

        LLMProviderManager manager = LLMProviderManager.getInstance();
        if (!manager.isConfigured()) {
            error("AI is not configured. Open Meteor GUI → MCP → Configure AI.");
            return 0;
        }

        if (!enforceCooldown(this)) {
            return 0;
        }

        runSimpleQueryAsync(this, prompt);
        return SINGLE_SUCCESS;
    }

    static boolean enforceCooldown(Command command) {
        if (mc.player == null) {
            return true;
        }

        UUID playerId = mc.player.getUUID();
        long now = System.currentTimeMillis();
        Long last = LAST_CALL_TIME.get(playerId);

        if (last != null && (now - last) < COOLDOWN_MS) {
            double waitSeconds = (COOLDOWN_MS - (now - last)) / 1000.0;
            command.warning("Please wait %.1f seconds before using this command again", waitSeconds);
            return false;
        }

        LAST_CALL_TIME.put(playerId, now);
        return true;
    }

    static void runSimpleQueryAsync(Command command, String prompt) {
        LLMProvider provider = LLMProviderManager.getInstance().getActiveProvider();
        command.info("Querying %s...", provider.name());
        MeteorExecutor.execute(() -> {
            try {
                String response = provider.executeSimplePrompt(prompt);
                command.info(response);
            } catch (Exception e) {
                command.error("AI query failed: %s", safeMessage(e));
                MeteorMCPAddon.LOG.error("AI command failed", e);
            }
        });
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
