package com.cope.meteormcp.systems;

import net.minecraft.nbt.NbtCompound;

import java.util.Objects;
import java.util.Set;

/**
 * Persistent configuration for the Ollama AI provider.
 */
public class OllamaConfig {

    /** Valid keep-alive presets shown in the settings dropdown. */
    public static final String[] KEEP_ALIVE_OPTIONS = {"5m", "10m", "30m", "1h", "-1"};

    private static final Set<String> VALID_KEEP_ALIVE = Set.of(
        "0m", "1m", "5m", "10m", "30m", "1h", "2h", "-1"
    );

    private String host;
    private String model;
    private int contextLength;
    private int maxOutputTokens;
    private float temperature;
    private boolean enabled;
    private String keepAlive;

    public OllamaConfig() {
        this.host = "http://localhost:11434";
        this.model = "llama3.1";
        this.contextLength = 8192;
        this.maxOutputTokens = 2048;
        this.temperature = 0.7f;
        this.enabled = false;
        this.keepAlive = "5m";
    }

    public OllamaConfig copy() {
        OllamaConfig copy = new OllamaConfig();
        copy.host = this.host;
        copy.model = this.model;
        copy.contextLength = this.contextLength;
        copy.maxOutputTokens = this.maxOutputTokens;
        copy.temperature = this.temperature;
        copy.enabled = this.enabled;
        copy.keepAlive = this.keepAlive;
        return copy;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && model != null && !model.isBlank();
    }

    public boolean isValid() {
        return enabled && isConfigured();
    }

    // ---- Getters / Setters ----

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host != null ? host.trim() : "http://localhost:11434";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : "llama3.1";
    }

    public int getContextLength() {
        return contextLength;
    }

    public void setContextLength(int contextLength) {
        this.contextLength = Math.max(2048, Math.min(131072, contextLength));
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

    public String getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(String keepAlive) {
        if (keepAlive != null && VALID_KEEP_ALIVE.contains(keepAlive.trim())) {
            this.keepAlive = keepAlive.trim();
        } else {
            this.keepAlive = "5m";
        }
    }

    // ---- NBT persistence ----

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("host", host != null ? host : "");
        tag.putString("model", model != null ? model : "");
        tag.putInt("context_length", contextLength);
        tag.putInt("max_tokens", maxOutputTokens);
        tag.putFloat("temperature", temperature);
        tag.putBoolean("enabled", enabled);
        tag.putString("keep_alive", keepAlive != null ? keepAlive : "5m");
        return tag;
    }

    public static OllamaConfig fromTag(NbtCompound tag) {
        OllamaConfig config = new OllamaConfig();
        if (tag == null) return config;

        tag.getString("host").ifPresent(config::setHost);
        tag.getString("model").ifPresent(config::setModel);
        tag.getInt("context_length").ifPresent(v -> config.contextLength = v);
        tag.getInt("max_tokens").ifPresent(v -> config.maxOutputTokens = v);
        tag.getFloat("temperature").ifPresent(v -> config.temperature = v);
        tag.getBoolean("enabled").ifPresent(v -> config.enabled = v);
        tag.getString("keep_alive").ifPresent(config::setKeepAlive);

        config.setContextLength(config.contextLength);
        config.setMaxOutputTokens(config.maxOutputTokens);
        config.setTemperature(config.temperature);

        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OllamaConfig that)) return false;
        return contextLength == that.contextLength
            && maxOutputTokens == that.maxOutputTokens
            && Float.compare(that.temperature, temperature) == 0
            && enabled == that.enabled
            && Objects.equals(host, that.host)
            && Objects.equals(model, that.model)
            && Objects.equals(keepAlive, that.keepAlive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, model, contextLength, maxOutputTokens, temperature, enabled, keepAlive);
    }

    @Override
    public String toString() {
        return "OllamaConfig{" +
            "host='" + host + '\'' +
            ", model='" + model + '\'' +
            ", contextLength=" + contextLength +
            ", maxOutputTokens=" + maxOutputTokens +
            ", temperature=" + temperature +
            ", enabled=" + enabled +
            ", keepAlive='" + keepAlive + '\'' +
            '}';
    }
}
