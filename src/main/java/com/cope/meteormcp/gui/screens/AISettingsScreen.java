package com.cope.meteormcp.gui.screens;

import com.cope.meteormcp.llm.LLMProvider;
import com.cope.meteormcp.llm.LLMProviderManager;
import com.cope.meteormcp.ollama.OllamaClientManager;
import com.cope.meteormcp.systems.AIConfig;
import com.cope.meteormcp.systems.AIConfig.ProviderType;
import com.cope.meteormcp.systems.GeminiConfig;
import com.cope.meteormcp.systems.GeminiConfig.GeminiModel;
import com.cope.meteormcp.systems.MCPServers;
import com.cope.meteormcp.systems.OllamaConfig;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDoubleEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Unified AI settings screen supporting Gemini and Ollama providers.
 * <p>
 * State that must survive {@link #reload()} (e.g. when switching providers or
 * refreshing the model list) is stored in plain fields rather than read from
 * widgets, because reload destroys and recreates every widget.
 */
public class AISettingsScreen extends WindowScreen {
    private final WidgetScreen parent;

    // ---- State that survives reload() ----
    private ProviderType selectedProvider;
    private GeminiConfig editedGemini;
    private OllamaConfig editedOllama;
    private List<String> ollamaModels;
    private boolean ollamaModelsFetched;

    // ---- Widgets (recreated on every reload) ----
    private WDropdown<ProviderType> providerDropdown;
    private WLabel statusLabel;

    // Gemini
    private WTextBox geminiApiKeyInput;
    private WDropdown<GeminiModel> geminiModelDropdown;
    private WIntEdit geminiMaxTokensInput;
    private WDoubleEdit geminiTemperatureInput;
    private WCheckbox geminiEnabledCheckbox;

    // Ollama
    private WTextBox ollamaHostInput;
    private WDropdown<String> ollamaModelDropdown;
    private WDropdown<String> ollamaKeepAliveDropdown;
    private WIntEdit ollamaContextLengthInput;
    private WIntEdit ollamaMaxTokensInput;
    private WDoubleEdit ollamaTemperatureInput;
    private WCheckbox ollamaEnabledCheckbox;

    public AISettingsScreen(GuiTheme theme, WidgetScreen parent) {
        super(theme, "Configure AI");
        this.parent = parent;

        AIConfig saved = MCPServers.get().getAIConfig();
        this.selectedProvider = saved.getActiveProvider();
        this.editedGemini = saved.getGeminiConfig().copy();
        this.editedOllama = saved.getOllamaConfig().copy();
        this.ollamaModels = new ArrayList<>();
        this.ollamaModelsFetched = false;
    }

    @Override
    public void initWidgets() {
        // Provider selector
        add(theme.label("AI Provider:"));
        providerDropdown = add(theme.dropdown(ProviderType.values(), selectedProvider))
            .expandX().widget();
        providerDropdown.action = () -> {
            snapshotCurrentWidgets();
            selectedProvider = providerDropdown.get();
            reload();
        };

        add(theme.horizontalSeparator()).expandX();

        if (selectedProvider == ProviderType.GEMINI) {
            initGeminiWidgets();
        } else {
            initOllamaWidgets();
        }

        add(theme.horizontalSeparator()).expandX();

        // Status
        statusLabel = add(theme.label("")).expandX().widget();
        updateStatusLabel();

        // Buttons
        WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();

        if (selectedProvider == ProviderType.GEMINI) {
            WButton testButton = buttons.add(theme.button("Test Connection")).expandX().widget();
            testButton.action = this::testGeminiConnection;
        } else {
            WButton testButton = buttons.add(theme.button("Test Connection")).expandX().widget();
            testButton.action = this::testOllamaConnection;

            WButton loadButton = buttons.add(theme.button("Load Model")).expandX().widget();
            loadButton.action = this::loadOllamaModel;
        }

        WButton saveButton = buttons.add(theme.button("Save")).expandX().widget();
        saveButton.action = this::save;

        WButton cancelButton = buttons.add(theme.button("Cancel")).expandX().widget();
        cancelButton.action = () -> mc.setScreen(parent);

        // Auto-fetch Ollama models on first view
        if (selectedProvider == ProviderType.OLLAMA && !ollamaModelsFetched) {
            ollamaModelsFetched = true;
            fetchOllamaModels();
        }
    }

    // ---- Gemini widgets ----

    private void initGeminiWidgets() {
        add(theme.label("Gemini API Key:"));
        geminiApiKeyInput = add(theme.textBox(editedGemini.getApiKey())).expandX().widget();

        add(theme.label("Model:"));
        geminiModelDropdown = add(theme.dropdown(GeminiModel.values(), editedGemini.getModel()))
            .expandX().widget();

        add(theme.label("Max Output Tokens:"));
        geminiMaxTokensInput = add(theme.intEdit(editedGemini.getMaxOutputTokens(), 256, 8192, true))
            .expandX().widget();

        add(theme.label("Temperature:"));
        geminiTemperatureInput = add(theme.doubleEdit(editedGemini.getTemperature(), 0.0, 2.0, 0.0, 2.0))
            .expandX().widget();
        geminiTemperatureInput.decimalPlaces = 2;
        geminiTemperatureInput.small = true;

        add(theme.label("Enable Gemini:"));
        geminiEnabledCheckbox = add(theme.checkbox(editedGemini.isEnabled())).widget();
    }

    // ---- Ollama widgets ----

    private void initOllamaWidgets() {
        add(theme.label("Ollama Host:"));
        ollamaHostInput = add(theme.textBox(editedOllama.getHost())).expandX().widget();

        // Model dropdown
        add(theme.label("Model:"));
        String[] modelChoices = buildModelChoices(editedOllama.getModel());
        String selected = resolveSelectedModel(editedOllama.getModel(), modelChoices);

        WHorizontalList modelRow = add(theme.horizontalList()).expandX().widget();
        ollamaModelDropdown = modelRow.add(theme.dropdown(modelChoices, selected)).expandX().widget();

        WButton refreshBtn = modelRow.add(theme.button("Refresh")).widget();
        refreshBtn.action = this::fetchOllamaModels;

        // Keep Alive
        add(theme.label("Keep Alive:"));
        String currentKeepAlive = resolveKeepAlive(editedOllama.getKeepAlive());
        ollamaKeepAliveDropdown = add(theme.dropdown(OllamaConfig.KEEP_ALIVE_OPTIONS, currentKeepAlive))
            .expandX().widget();

        add(theme.label("Context Length:"));
        ollamaContextLengthInput = add(theme.intEdit(editedOllama.getContextLength(), 2048, 131072, true))
            .expandX().widget();

        add(theme.label("Max Output Tokens:"));
        ollamaMaxTokensInput = add(theme.intEdit(editedOllama.getMaxOutputTokens(), 256, 8192, true))
            .expandX().widget();

        add(theme.label("Temperature:"));
        ollamaTemperatureInput = add(theme.doubleEdit(editedOllama.getTemperature(), 0.0, 2.0, 0.0, 2.0))
            .expandX().widget();
        ollamaTemperatureInput.decimalPlaces = 2;
        ollamaTemperatureInput.small = true;

        add(theme.label("Enable Ollama:"));
        ollamaEnabledCheckbox = add(theme.checkbox(editedOllama.isEnabled())).widget();
    }

    // ---- Model dropdown helpers ----

    private String[] buildModelChoices(String currentModel) {
        Set<String> choices = new LinkedHashSet<>();

        for (String model : ollamaModels) {
            if (model != null && !model.isBlank()) {
                choices.add(model);
            }
        }

        // Always include the current model so it stays selectable
        if (currentModel != null && !currentModel.isBlank()) {
            choices.add(currentModel);
        }

        if (choices.isEmpty()) {
            choices.add("llama3.1");
        }

        return choices.toArray(String[]::new);
    }

    private String resolveSelectedModel(String currentModel, String[] choices) {
        if (currentModel != null && !currentModel.isBlank()) {
            for (String c : choices) {
                if (c.equals(currentModel)) return currentModel;
            }
        }
        return choices[0];
    }

    private String resolveKeepAlive(String value) {
        for (String option : OllamaConfig.KEEP_ALIVE_OPTIONS) {
            if (option.equals(value)) return value;
        }
        return "5m";
    }

    private void fetchOllamaModels() {
        OllamaConfig tempConfig = new OllamaConfig();
        String host = ollamaHostInput != null
            ? ollamaHostInput.get().trim()
            : editedOllama.getHost();
        tempConfig.setHost(host);

        updateStatus("Fetching models from " + host + "...", true);

        MeteorExecutor.execute(() -> {
            List<String> models = OllamaClientManager.getInstance().listModels(tempConfig);
            Runnable update = () -> {
                snapshotCurrentWidgets();
                if (!models.isEmpty()) {
                    ollamaModels = models;
                    updateStatus("Found " + models.size() + " model(s)", true);
                } else {
                    updateStatus("No models found. Is Ollama running at " + host + "?", false);
                }
                reload();
            };
            if (mc != null) mc.execute(update);
            else update.run();
        });
    }

    // ---- Snapshot / collect ----

    /**
     * Read current widget values back into the edited config objects so they
     * survive the next {@link #reload()}.
     */
    private void snapshotCurrentWidgets() {
        if (geminiApiKeyInput != null) {
            editedGemini.setApiKey(geminiApiKeyInput.get().trim());
            editedGemini.setModel(geminiModelDropdown.get());
            editedGemini.setMaxOutputTokens(geminiMaxTokensInput.get());
            editedGemini.setTemperature((float) geminiTemperatureInput.get());
            editedGemini.setEnabled(geminiEnabledCheckbox.checked);
        }
        if (ollamaHostInput != null) {
            editedOllama.setHost(ollamaHostInput.get().trim());
            editedOllama.setModel(ollamaModelDropdown.get());
            editedOllama.setKeepAlive(ollamaKeepAliveDropdown.get());
            editedOllama.setContextLength(ollamaContextLengthInput.get());
            editedOllama.setMaxOutputTokens(ollamaMaxTokensInput.get());
            editedOllama.setTemperature((float) ollamaTemperatureInput.get());
            editedOllama.setEnabled(ollamaEnabledCheckbox.checked);
        }
    }

    private AIConfig collectFormConfig() {
        snapshotCurrentWidgets();

        AIConfig config = new AIConfig();
        config.setActiveProvider(selectedProvider);
        config.setGeminiConfig(editedGemini.copy());
        config.setOllamaConfig(editedOllama.copy());
        return config;
    }

    // ---- Actions ----

    /** Gemini: full test via provider (sends a real API request). */
    private void testGeminiConnection() {
        AIConfig draft = collectFormConfig();
        updateStatus("Testing Gemini connection...", true);

        MeteorExecutor.execute(() -> {
            AIConfig original = MCPServers.get().getAIConfig();
            MCPServers.get().setAIConfig(draft);

            try {
                LLMProvider provider = LLMProviderManager.getInstance().getActiveProvider();
                LLMProvider.TestResult result = provider.testConnection();
                Runnable update = () -> updateStatus(result.message(), result.success());
                if (mc != null) mc.execute(update);
                else update.run();
            } finally {
                MCPServers.get().setAIConfig(original);
            }
        });
    }

    /** Ollama: fast ping + model existence check. Does NOT load the model. */
    private void testOllamaConnection() {
        snapshotCurrentWidgets();
        updateStatus("Testing connection...", true);

        OllamaConfig testConfig = editedOllama.copy();
        MeteorExecutor.execute(() -> {
            OllamaClientManager.TestResult result = OllamaClientManager.getInstance().quickTest(testConfig);
            Runnable update = () -> updateStatus(result.message(), result.success());
            if (mc != null) mc.execute(update);
            else update.run();
        });
    }

    /** Ollama: send a minimal chat request to load the model into memory. */
    private void loadOllamaModel() {
        snapshotCurrentWidgets();
        updateStatus("Loading model (this may take a while on first load)...", true);

        OllamaConfig loadConfig = editedOllama.copy();
        MeteorExecutor.execute(() -> {
            OllamaClientManager.TestResult result = OllamaClientManager.getInstance().loadModel(loadConfig);
            Runnable update = () -> updateStatus(result.message(), result.success());
            if (mc != null) mc.execute(update);
            else update.run();
        });
    }

    private void save() {
        AIConfig config = collectFormConfig();
        MCPServers.get().setAIConfig(config);

        if (parent != null) {
            parent.reload();
        }
        mc.setScreen(parent);
    }

    // ---- Status display ----

    private void updateStatusLabel() {
        boolean valid = selectedProvider == ProviderType.GEMINI
            ? editedGemini.isValid()
            : editedOllama.isValid();
        String text = "Status: " + selectedProvider.name() + " - " + (valid ? "Enabled" : "Not Active");
        updateStatus(text, valid);
    }

    private void updateStatus(String message, boolean success) {
        if (statusLabel == null) return;
        statusLabel.set(message);
        statusLabel.color(success ? theme.textColor() : theme.textSecondaryColor());
    }
}
