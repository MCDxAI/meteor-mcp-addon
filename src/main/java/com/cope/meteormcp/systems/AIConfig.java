package com.cope.meteormcp.systems;

import net.minecraft.nbt.NbtCompound;

import java.util.Locale;
import java.util.Objects;

/**
 * Unified AI configuration that wraps provider-specific configs and a provider selector.
 * Stored in MCPServers and persisted via NBT.
 */
public class AIConfig {

    public enum ProviderType {
        GEMINI,
        OLLAMA;

        public static ProviderType fromName(String name) {
            if (name == null) return GEMINI;
            try {
                return ProviderType.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return GEMINI;
            }
        }
    }

    private ProviderType activeProvider;
    private GeminiConfig geminiConfig;
    private OllamaConfig ollamaConfig;

    public AIConfig() {
        this.activeProvider = ProviderType.GEMINI;
        this.geminiConfig = new GeminiConfig();
        this.ollamaConfig = new OllamaConfig();
    }

    public AIConfig copy() {
        AIConfig copy = new AIConfig();
        copy.activeProvider = this.activeProvider;
        copy.geminiConfig = this.geminiConfig.copy();
        copy.ollamaConfig = this.ollamaConfig.copy();
        return copy;
    }

    public ProviderType getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(ProviderType activeProvider) {
        this.activeProvider = activeProvider != null ? activeProvider : ProviderType.GEMINI;
    }

    public GeminiConfig getGeminiConfig() {
        return geminiConfig;
    }

    public void setGeminiConfig(GeminiConfig geminiConfig) {
        this.geminiConfig = geminiConfig != null ? geminiConfig : new GeminiConfig();
    }

    public OllamaConfig getOllamaConfig() {
        return ollamaConfig;
    }

    public void setOllamaConfig(OllamaConfig ollamaConfig) {
        this.ollamaConfig = ollamaConfig != null ? ollamaConfig : new OllamaConfig();
    }

    /** Whether the currently active provider is fully configured and enabled. */
    public boolean isActiveProviderValid() {
        return switch (activeProvider) {
            case GEMINI -> geminiConfig.isValid();
            case OLLAMA -> ollamaConfig.isValid();
        };
    }

    // ---- NBT persistence ----

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("provider", activeProvider.name());
        tag.put("gemini", geminiConfig.toTag());
        tag.put("ollama", ollamaConfig.toTag());
        return tag;
    }

    public static AIConfig fromTag(NbtCompound tag) {
        AIConfig config = new AIConfig();
        if (tag == null) return config;

        tag.getString("provider").ifPresent(v -> config.activeProvider = ProviderType.fromName(v));
        tag.getCompound("gemini").ifPresent(geminiTag -> {
            config.geminiConfig = GeminiConfig.fromTag(geminiTag);
        });
        tag.getCompound("ollama").ifPresent(ollamaTag -> {
            config.ollamaConfig = OllamaConfig.fromTag(ollamaTag);
        });

        return config;
    }

    /**
     * Migrate from old saves that only had a top-level "gemini" tag.
     * Creates an AIConfig with the old Gemini config and defaults for everything else.
     */
    public static AIConfig migrateFromGeminiTag(NbtCompound geminiTag) {
        AIConfig config = new AIConfig();
        config.activeProvider = ProviderType.GEMINI;
        config.geminiConfig = GeminiConfig.fromTag(geminiTag);
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AIConfig that)) return false;
        return activeProvider == that.activeProvider
            && Objects.equals(geminiConfig, that.geminiConfig)
            && Objects.equals(ollamaConfig, that.ollamaConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeProvider, geminiConfig, ollamaConfig);
    }

    @Override
    public String toString() {
        return "AIConfig{provider=" + activeProvider + "}";
    }
}
