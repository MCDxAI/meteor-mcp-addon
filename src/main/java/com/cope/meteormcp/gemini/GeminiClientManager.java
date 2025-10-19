package com.cope.meteormcp.gemini;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.systems.GeminiConfig;
import com.cope.meteormcp.systems.MCPServers;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton responsible for managing the lifecycle of the Gemini API client.
 * Provides thread-safe access, config change invalidation, and connection testing.
 */
public final class GeminiClientManager {
    private static final GeminiClientManager INSTANCE = new GeminiClientManager();

    private final ReentrantLock clientLock = new ReentrantLock();

    private Client client;
    private GeminiConfig currentConfig;

    private GeminiClientManager() {
    }

    public static GeminiClientManager getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        GeminiConfig config = MCPServers.get().getGeminiConfig();
        return config != null && config.isValid();
    }

    public Client getClient() {
        clientLock.lock();
        try {
            GeminiConfig config = MCPServers.get().getGeminiConfig();
            if (config == null || !config.isValid()) {
                throw new IllegalStateException("Gemini configuration is not valid or not enabled.");
            }

            if (client != null && currentConfig != null && Objects.equals(currentConfig, config)) {
                return client;
            }

            closeClientUnsafe();

            client = buildClient(config);
            currentConfig = config.copy();
            MeteorMCPAddon.LOG.info("Gemini client initialized for model {}", currentConfig.getModelId());

            return client;
        } finally {
            clientLock.unlock();
        }
    }

    public void invalidateClient() {
        clientLock.lock();
        try {
            closeClientUnsafe();
        } finally {
            clientLock.unlock();
        }
    }

    public void shutdown() {
        invalidateClient();
    }

    public TestResult testConfiguration(GeminiConfig config) {
        if (config == null || !config.hasCredentials()) {
            return new TestResult(false, "Provide an API key and model before testing.");
        }

        Client temp = null;
        try {
            temp = buildClient(config);
            GenerateContentConfig requestConfig = createBaseRequestConfig(config).build();
            var response = temp.models.generateContent(
                config.getModelId(),
                "Respond with: test successful",
                requestConfig
            );

            String text = "";
            if (response != null) {
                text = response.text();
                if (text == null || text.isBlank()) {
                    var parts = response.parts();
                    if (parts != null && !parts.isEmpty()) {
                        text = parts.stream()
                            .map(part -> part.text().orElse(""))
                            .filter(str -> !str.isBlank())
                            .findFirst()
                            .orElse("");
                    }
                }
            }

            if (text.isEmpty()) {
                text = "Received empty response from Gemini.";
            }

            return new TestResult(true, text);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = ex.getClass().getSimpleName();
            }
            return new TestResult(false, message);
        } finally {
            if (temp != null) {
                try {
                    temp.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    static GenerateContentConfig.Builder createBaseRequestConfig(GeminiConfig config) {
        return GenerateContentConfig.builder()
            .maxOutputTokens(config.getMaxOutputTokens())
            .temperature(config.getTemperature());
    }

    private Client buildClient(GeminiConfig config) {
        return Client.builder()
            .apiKey(config.getApiKey())
            .build();
    }

    private void closeClientUnsafe() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                MeteorMCPAddon.LOG.warn("Error while closing Gemini client: {}", e.getMessage());
            } finally {
                client = null;
                currentConfig = null;
            }
        }
    }

    public static final class TestResult {
        private final boolean success;
        private final String message;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean success() {
            return success;
        }

        public String message() {
            return message;
        }
    }
}
