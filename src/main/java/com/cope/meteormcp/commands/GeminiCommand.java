package com.cope.meteormcp.commands;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.gemini.GeminiClientManager;
import com.cope.meteormcp.gemini.GeminiExecutor;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;

/**
 * Simple Gemini prompt command without MCP tool access.
 */
public class GeminiCommand extends Command {
    private static final long COOLDOWN_MS = 1000;
    private static final Map<UUID, Long> LAST_CALL_TIME = new ConcurrentHashMap<>();

    public GeminiCommand() {
        super("gemini", "Query Gemini AI without MCP tools");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("prompt", StringArgumentType.greedyString())
            .executes(context -> executeGemini(context.getArgument("prompt", String.class)))
        );

        builder.executes(context -> {
            error("Prompt is required. Usage: /gemini \"prompt\"");
            return 0;
        });
    }

    int executeGemini(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            error("Prompt is required. Usage: /gemini \"prompt\"");
            return 0;
        }

        if (!GeminiClientManager.getInstance().isConfigured()) {
            error("Gemini is not configured. Open Meteor GUI → MCP → Configure Gemini API.");
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

        UUID playerId = mc.player.getUuid();
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
        command.info("Querying Gemini...");
        MeteorExecutor.execute(() -> {
            try {
                String response = GeminiExecutor.executeSimplePrompt(prompt);
                command.info(response);
            } catch (Exception e) {
                command.error("Gemini query failed: {}", safeMessage(e));
                MeteorMCPAddon.LOG.error("Gemini command failed", e);
            }
        });
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
