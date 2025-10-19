package com.cope.meteormcp.gui.screens;

import com.cope.meteormcp.systems.GeminiConfig;
import com.cope.meteormcp.systems.MCPServerConfig;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Main MCP servers management screen.
 * Displays list of configured servers with management controls.
 */
public class MCPServersScreen extends WindowTabScreen {

    public MCPServersScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);
        window.padding = 6;
        window.spacing = 4;
        window.id = "mcp-servers";
    }

    @Override
    public void initWidgets() {
        // Add server button
        WButton addButton = add(theme.button("Add Server")).expandX().widget();
        addButton.action = () -> mc.setScreen(new AddMCPServerScreen(theme, this));

        add(theme.horizontalSeparator()).expandX();

        // Gemini Settings Section
        add(theme.label("Gemini API Settings")).expandX();

        WHorizontalList geminiRow = add(theme.horizontalList()).expandX().widget();

        WButton geminiSettingsBtn = geminiRow.add(theme.button("Configure Gemini API")).expandX().widget();
        geminiSettingsBtn.action = () -> mc.setScreen(new GeminiSettingsScreen(theme, this));

        GeminiConfig geminiConfig = MCPServers.get().getGeminiConfig();
        boolean hasGeminiCredentials = geminiConfig != null && geminiConfig.hasCredentials();
        boolean geminiEnabled = geminiConfig != null && geminiConfig.isEnabled();
        String geminiStatus;
        if (!hasGeminiCredentials) {
            geminiStatus = "Not Configured";
        } else if (geminiEnabled) {
            geminiStatus = "Enabled";
        } else {
            geminiStatus = "Configured (Disabled)";
        }
        geminiRow.add(theme.label(geminiStatus));

        add(theme.horizontalSeparator()).expandX();

        // Server list table
        WTable table = add(theme.table()).expandX().widget();

        MCPServers mcpServers = MCPServers.get();

        for (MCPServerConfig config : mcpServers.getAllConfigs()) {
            // Server name
            table.add(theme.label(config.getName()));

            // Status
            boolean connected = mcpServers.isConnected(config.getName());
            String status = connected ? "Connected" : "Disconnected";
            table.add(theme.label(status));

            // Action buttons
            WHorizontalList actions = table.add(theme.horizontalList()).expandCellX().right().widget();

            // Connect/Disconnect button
            if (connected) {
                WButton disconnectBtn = actions.add(theme.button("Disconnect")).widget();
                disconnectBtn.action = () -> {
                    mcpServers.disconnect(config.getName());
                    reload();
                };
            } else {
                WButton connectBtn = actions.add(theme.button("Connect")).widget();
                connectBtn.action = () -> {
                    mcpServers.connect(config.getName());
                    reload();
                };
            }

            // Tools button
            WButton toolsBtn = actions.add(theme.button("Tools")).widget();
            toolsBtn.action = () -> {
                MCPServerConnection connection = mcpServers.getConnection(config.getName());
                if (connection != null) {
                    mc.setScreen(new MCPToolsScreen(theme, this, connection));
                }
            };

            // Edit button
            WButton editBtn = actions.add(theme.button("Edit")).widget();
            editBtn.action = () -> mc.setScreen(new EditMCPServerScreen(theme, this, config));

            // Remove button
            WButton removeBtn = actions.add(theme.button("Remove")).widget();
            removeBtn.action = () -> {
                mcpServers.remove(config.getName());
                reload();
            };

            table.row();
        }

        // Empty state
        if (mcpServers.getAllConfigs().isEmpty()) {
            add(theme.label("No MCP servers configured."));
            add(theme.label("Click 'Add Server' to get started."));
        }
    }
}
