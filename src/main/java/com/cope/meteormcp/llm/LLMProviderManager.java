package com.cope.meteormcp.llm;

import com.cope.meteormcp.systems.AIConfig;
import com.cope.meteormcp.systems.MCPServers;

/**
 * Singleton that holds the active LLM provider.
 * All downstream consumers (commands, StarScript, GUI) use this to access AI capabilities.
 */
public final class LLMProviderManager {
    private static final LLMProviderManager INSTANCE = new LLMProviderManager();

    private final GeminiProvider geminiProvider = new GeminiProvider();
    private final OllamaProvider ollamaProvider = new OllamaProvider();

    private LLMProviderManager() {}

    public static LLMProviderManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the currently active provider based on config, or null if none is configured.
     */
    public LLMProvider getActiveProvider() {
        AIConfig config = MCPServers.get().getAIConfig();
        return switch (config.getActiveProvider()) {
            case GEMINI -> geminiProvider;
            case OLLAMA -> ollamaProvider;
        };
    }

    /**
     * Whether the active provider is configured and ready to use.
     */
    public boolean isConfigured() {
        LLMProvider provider = getActiveProvider();
        return provider != null && provider.isConfigured();
    }

    /**
     * Get the display name of the active provider (e.g. "Gemini", "Ollama").
     */
    public String getActiveProviderName() {
        LLMProvider provider = getActiveProvider();
        return provider != null ? provider.name() : "None";
    }

    /**
     * Called when AI config changes to invalidate any cached state.
     */
    public void invalidate() {
        // Providers are stateless wrappers; invalidation is handled by the
        // underlying client managers (GeminiClientManager, OllamaClientManager).
        com.cope.meteormcp.ollama.OllamaClientManager.getInstance().invalidateClient();
    }
}
