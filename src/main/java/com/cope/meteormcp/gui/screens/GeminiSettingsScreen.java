package com.cope.meteormcp.gui.screens;

import com.cope.meteormcp.gemini.GeminiClientManager;
import com.cope.meteormcp.systems.GeminiConfig;
import com.cope.meteormcp.systems.GeminiConfig.GeminiModel;
import com.cope.meteormcp.systems.MCPServers;
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

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Configuration screen for Gemini API integration.
 */
public class GeminiSettingsScreen extends WindowScreen {
    private final WidgetScreen parent;
    private final GeminiConfig initialConfig;

    private WTextBox apiKeyInput;
    private WDropdown<GeminiModel> modelDropdown;
    private WIntEdit maxTokensInput;
    private WDoubleEdit temperatureInput;
    private WCheckbox enabledCheckbox;
    private WLabel statusLabel;

    public GeminiSettingsScreen(GuiTheme theme, WidgetScreen parent) {
        super(theme, "Configure Gemini API");
        this.parent = parent;
        this.initialConfig = MCPServers.get().getGeminiConfig().copy();
    }

    @Override
    public void initWidgets() {
        add(theme.label("Gemini API Key:"));
        apiKeyInput = add(theme.textBox(initialConfig.getApiKey()))
            .expandX()
            .widget();

        add(theme.label("Model:"));
        modelDropdown = add(theme.dropdown(GeminiModel.values(), initialConfig.getModel()))
            .expandX().widget();

        add(theme.label("Max Output Tokens:"));
        maxTokensInput = add(theme.intEdit(initialConfig.getMaxOutputTokens(), 256, 8192, true))
            .expandX().widget();

        add(theme.label("Temperature:"));
        temperatureInput = add(theme.doubleEdit(initialConfig.getTemperature(), 0.0, 2.0, 0.0, 2.0))
            .expandX().widget();
        temperatureInput.decimalPlaces = 2;
        temperatureInput.small = true;

        add(theme.label("Enable Gemini Integration:"));
        enabledCheckbox = add(theme.checkbox(initialConfig.isEnabled())).widget();

        add(theme.horizontalSeparator()).expandX();

        statusLabel = add(theme.label(""))
            .expandX()
            .widget();
        updateStatusLabel(initialConfig);

        WHorizontalList buttons = add(theme.horizontalList())
            .expandX()
            .widget();

        WButton testButton = buttons.add(theme.button("Test Connection"))
            .expandX()
            .widget();
        testButton.action = this::testConnection;

        WButton saveButton = buttons.add(theme.button("Save"))
            .expandX()
            .widget();
        saveButton.action = this::save;

        WButton cancelButton = buttons.add(theme.button("Cancel"))
            .expandX()
            .widget();
        cancelButton.action = () -> mc.setScreen(parent);
    }

    private void testConnection() {
        GeminiConfig draft = collectFormConfig();
        // Ensure we attempt the test even when the user hasn't enabled it yet.
        draft.setEnabled(true);

        if (!draft.hasCredentials()) {
            updateStatus("Warning: enter an API key and pick a model before testing.", false);
            return;
        }

        GeminiClientManager.TestResult result = GeminiClientManager.getInstance().testConfiguration(draft);
        updateStatus(result.message(), result.success());
    }

    private void save() {
        GeminiConfig config = collectFormConfig();

        if (!config.hasCredentials()) {
            config.setEnabled(false);
            updateStatus("Warning: Gemini disabled because the API key or model is missing.", false);
        }

        MCPServers.get().setGeminiConfig(config);
        updateStatusLabel(config);

        if (parent != null) {
            parent.reload();
        }
        mc.setScreen(parent);
    }

    private GeminiConfig collectFormConfig() {
        GeminiConfig config = new GeminiConfig();
        config.setApiKey(apiKeyInput.get().trim());
        config.setModel(modelDropdown.get());
        config.setMaxOutputTokens(maxTokensInput.get());
        config.setTemperature((float) temperatureInput.get());
        config.setEnabled(enabledCheckbox.checked && config.hasCredentials());
        return config;
    }

    private void updateStatusLabel(GeminiConfig config) {
        boolean hasCredentials = config != null && config.hasCredentials();
        boolean enabled = config != null && config.isEnabled();

        String text;
        if (!hasCredentials) {
            text = "Status: Not Configured";
        } else if (enabled) {
            text = "Status: Enabled";
        } else {
            text = "Status: Configured (Disabled)";
        }

        updateStatus(text, hasCredentials);
    }

    private void updateStatus(String message, boolean success) {
        if (statusLabel == null) return;
        statusLabel.set(message);
        statusLabel.color(success ? theme.textColor() : theme.textSecondaryColor());
    }
}
