package com.cope.meteormcp.ollama;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.systems.MCPServers;
import com.cope.meteormcp.systems.OllamaConfig;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Singleton managing the lifecycle of the Ollama API client.
 * Mirrors the pattern of {@link com.cope.meteormcp.gemini.GeminiClientManager}.
 */
public final class OllamaClientManager {
    private static final OllamaClientManager INSTANCE = new OllamaClientManager();
    private static final AtomicBoolean mapperConfigured = new AtomicBoolean(false);

    private final ReentrantLock clientLock = new ReentrantLock();

    private Ollama client;
    private OllamaConfig currentConfig;

    private OllamaClientManager() {}

    public static OllamaClientManager getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        OllamaConfig config = MCPServers.get().getAIConfig().getOllamaConfig();
        return config != null && config.isValid();
    }

    public Ollama getClient() {
        clientLock.lock();
        try {
            OllamaConfig config = MCPServers.get().getAIConfig().getOllamaConfig();
            if (config == null || !config.isValid()) {
                throw new IllegalStateException("Ollama configuration is not valid or not enabled.");
            }

            if (client != null && currentConfig != null && Objects.equals(currentConfig, config)) {
                return client;
            }

            client = buildClient(config);
            currentConfig = config.copy();
            MeteorMCPAddon.LOG.info("Ollama client initialized for model {} at {}", config.getModel(), config.getHost());

            return client;
        } finally {
            clientLock.unlock();
        }
    }

    public void invalidateClient() {
        clientLock.lock();
        try {
            client = null;
            currentConfig = null;
        } finally {
            clientLock.unlock();
        }
    }

    public void shutdown() {
        invalidateClient();
    }

    static void ensureMapperCompatibility() {
        if (mapperConfigured.compareAndSet(false, true)) {
            Utils.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
    }

    /**
     * Fast connection test: pings the server and verifies the configured model
     * exists in the local model list. Does NOT load the model into memory.
     */
    public TestResult quickTest(OllamaConfig config) {
        if (config == null || !config.isConfigured()) {
            return new TestResult(false, "Provide a host and model before testing.");
        }

        try {
            Ollama tempApi = buildClient(config);

            if (!tempApi.ping()) {
                return new TestResult(false, "Server not reachable at " + config.getHost());
            }

            List<String> models = tempApi.listModels().stream()
                .map(m -> m.getName())
                .collect(Collectors.toList());

            if (models.isEmpty()) {
                return new TestResult(false, "Server reachable but no models installed.");
            }

            boolean modelFound = models.stream()
                .anyMatch(name -> name.equals(config.getModel())
                    || name.startsWith(config.getModel() + ":"));

            if (!modelFound) {
                return new TestResult(false, "Server reachable but model '"
                    + config.getModel() + "' not found. Available: "
                    + String.join(", ", models));
            }

            return new TestResult(true, "Connected! Model '" + config.getModel() + "' is available.");
        } catch (Exception ex) {
            return new TestResult(false, safeMessage(ex));
        }
    }

    /**
     * Load model into memory by sending a minimal chat request with keep-alive.
     * This is slow on first call (model loads into VRAM/RAM) but keeps it warm.
     */
    public TestResult loadModel(OllamaConfig config) {
        if (config == null || !config.isConfigured()) {
            return new TestResult(false, "Provide a host and model before loading.");
        }

        try {
            Ollama tempApi = buildClient(config);

            String keepAlive = config.getKeepAlive() != null ? config.getKeepAlive() : "5m";

            OllamaChatRequest request = OllamaChatRequest.builder()
                .withModel(config.getModel())
                .withMessage(OllamaChatMessageRole.USER, "Respond with only: ok")
                .withKeepAlive(keepAlive)
                .build();

            long start = System.currentTimeMillis();
            OllamaChatResult result = tempApi.chat(request, token -> {});
            long elapsed = System.currentTimeMillis() - start;

            String text = result.getResponseModel().getMessage().getResponse();
            if (text == null || text.isBlank()) {
                text = "(empty response)";
            }

            return new TestResult(true, "Model loaded in " + elapsed + "ms (keep-alive: "
                + keepAlive + "). Response: " + text.trim());
        } catch (Exception ex) {
            return new TestResult(false, "Load failed: " + safeMessage(ex));
        }
    }

    /**
     * List models available on the Ollama server.
     */
    public List<String> listModels(OllamaConfig config) {
        if (config == null || config.getHost() == null || config.getHost().isBlank()) {
            return Collections.emptyList();
        }

        try {
            Ollama tempApi = buildClient(config);
            return tempApi.listModels().stream()
                .map(m -> m.getName())
                .collect(Collectors.toList());
        } catch (Exception e) {
            MeteorMCPAddon.LOG.debug("Failed to list Ollama models: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Ollama buildClient(OllamaConfig config) {
        ensureMapperCompatibility();

        Ollama api = new Ollama(config.getHost());
        api.setRequestTimeoutSeconds(60);
        return api;
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    public record TestResult(boolean success, String message) {}
}
