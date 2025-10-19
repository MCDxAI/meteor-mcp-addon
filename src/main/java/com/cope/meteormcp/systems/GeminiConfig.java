package com.cope.meteormcp.systems;

import net.minecraft.nbt.NbtCompound;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * Persistent configuration container for Gemini API integration.
 * Mirrors the persistence pattern used by other Meteor systems.
 */
public class GeminiConfig {
    private static final byte[] KEY_SALT = "meteor-mcp-gemini".getBytes(StandardCharsets.UTF_8);

    private String apiKey;
    private GeminiModel model;
    private int maxOutputTokens;
    private float temperature;
    private boolean enabled;

    public GeminiConfig() {
        this.apiKey = "";
        this.model = GeminiModel.GEMINI_2_5_FLASH;
        this.maxOutputTokens = 2048;
        this.temperature = 0.7f;
        this.enabled = false;
    }

    public GeminiConfig copy() {
        GeminiConfig copy = new GeminiConfig();
        copy.apiKey = this.apiKey;
        copy.model = this.model;
        copy.maxOutputTokens = this.maxOutputTokens;
        copy.temperature = this.temperature;
        copy.enabled = this.enabled;
        return copy;
    }

    public boolean hasCredentials() {
        return apiKey != null && !apiKey.isBlank() && model != null;
    }

    public boolean isValid() {
        return enabled && hasCredentials();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public GeminiModel getModel() {
        return model;
    }

    public void setModel(GeminiModel model) {
        this.model = model != null ? model : GeminiModel.GEMINI_2_5_FLASH;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = Math.max(1, Math.min(8192, maxOutputTokens));
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        if (Float.isNaN(temperature) || Float.isInfinite(temperature)) {
            this.temperature = 0.7f;
        } else {
            this.temperature = Math.max(0.0f, Math.min(2.0f, temperature));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelId() {
        return model != null ? model.getId() : GeminiModel.GEMINI_2_5_FLASH.getId();
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        if (apiKey != null && !apiKey.isEmpty()) {
            tag.putString("api_key", encode(apiKey));
        }

        tag.putString("model", model != null ? model.name() : GeminiModel.GEMINI_2_5_FLASH.name());
        tag.putInt("max_tokens", maxOutputTokens);
        tag.putFloat("temperature", temperature);
        tag.putBoolean("enabled", enabled);

        return tag;
    }

    public static GeminiConfig fromTag(NbtCompound tag) {
        GeminiConfig config = new GeminiConfig();

        if (tag == null) {
            return config;
        }

        tag.getString("api_key").ifPresent(encoded -> config.apiKey = decode(encoded));
        tag.getString("model").ifPresent(value -> config.model = GeminiModel.fromName(value));

        tag.getInt("max_tokens").ifPresent(value -> config.maxOutputTokens = value);
        tag.getFloat("temperature").ifPresent(value -> config.temperature = value);
        tag.getBoolean("enabled").ifPresent(value -> config.enabled = value);

        // Clamp fields after loading to guard against invalid data
        config.setMaxOutputTokens(config.maxOutputTokens);
        config.setTemperature(config.temperature);
        config.setModel(config.model);

        return config;
    }

    private static String encode(String raw) {
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= KEY_SALT[i % KEY_SALT.length];
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String decode(String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] ^= KEY_SALT[i % KEY_SALT.length];
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeminiConfig that)) return false;
        return maxOutputTokens == that.maxOutputTokens
            && Float.compare(that.temperature, temperature) == 0
            && enabled == that.enabled
            && Objects.equals(apiKey, that.apiKey)
            && model == that.model;
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, model, maxOutputTokens, temperature, enabled);
    }

    @Override
    public String toString() {
        return "GeminiConfig{" +
            "model=" + model +
            ", maxOutputTokens=" + maxOutputTokens +
            ", temperature=" + temperature +
            ", enabled=" + enabled +
            '}';
    }

    public enum GeminiModel {
        GEMINI_2_5_PRO("gemini-2.5-pro"),
        GEMINI_2_5_FLASH("gemini-2.5-flash"),
        GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite"),
        GEMINI_FLASH_LATEST("gemini-flash-latest"),
        GEMINI_FLASH_LITE_LATEST("gemini-flash-lite-latest");

        private final String id;

        GeminiModel(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static GeminiModel fromName(String name) {
            if (name == null) return GEMINI_2_5_FLASH;
            try {
                return GeminiModel.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return GEMINI_2_5_FLASH;
            }
        }
    }
}
