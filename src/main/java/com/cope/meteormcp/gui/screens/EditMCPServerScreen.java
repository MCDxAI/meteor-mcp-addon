package com.cope.meteormcp.gui.screens;

import com.cope.meteormcp.systems.MCPServerConfig;
import com.cope.meteormcp.systems.MCPServers;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;

import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Form to edit an existing MCP server configuration.
 */
public class EditMCPServerScreen extends WindowScreen {
    private final WidgetScreen parent;
    private final MCPServerConfig originalConfig;
    private final String originalName;

    private WTextBox nameInput;
    private WDropdown<MCPServerConfig.TransportType> transportInput;
    private WTextBox commandInput;
    private WTextBox argsInput;
    private WTextBox workingDirInput;
    private WTextBox urlInput;
    private WTextBox envInput;
    private WCheckbox autoConnectInput;
    private WIntEdit timeoutInput;

    public EditMCPServerScreen(GuiTheme theme, WidgetScreen parent, MCPServerConfig config) {
        super(theme, "Edit MCP Server");
        this.parent = parent;
        this.originalConfig = config;
        this.originalName = config.getName();
    }

    @Override
    public void initWidgets() {
        // Server name
        add(theme.label("Server Name:"));
        nameInput = add(theme.textBox(originalConfig.getName())).expandX().widget();

        // Transport type
        add(theme.label("Transport Type:"));
        transportInput = add(theme.dropdown(originalConfig.getTransport())).expandX().widget();

        // STDIO fields
        add(theme.label("Command:"));
        commandInput = add(theme.textBox(originalConfig.getCommand() != null ? originalConfig.getCommand() : ""))
            .expandX().widget();

        add(theme.label("Arguments (comma separated):"));
        String argsStr = originalConfig.getArgs() != null ?
            String.join(",", originalConfig.getArgs()) : "";
        argsInput = add(theme.textBox(argsStr)).expandX().widget();

        add(theme.label("Working Directory (optional):"));
        String workingDir = originalConfig.getWorkingDirectory() != null ? originalConfig.getWorkingDirectory() : "";
        workingDirInput = add(theme.textBox(workingDir)).expandX().widget();

        // SSE/HTTP fields
        add(theme.label("URL:"));
        urlInput = add(theme.textBox(originalConfig.getUrl() != null ? originalConfig.getUrl() : ""))
            .expandX().widget();

        // Environment variables
        add(theme.label("Environment Variables (KEY=value, comma separated):"));
        String envStr = originalConfig.getEnv() != null ?
            originalConfig.getEnv().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")) : "";
        envInput = add(theme.textBox(envStr)).expandX().widget();

        // Auto-connect
        add(theme.label("Auto-connect on startup:"));
        autoConnectInput = add(theme.checkbox(originalConfig.isAutoConnect())).widget();

        // Timeout
        add(theme.label("Timeout (ms):"));
        timeoutInput = add(theme.intEdit(originalConfig.getTimeout(), 1000, 30000, false)).widget();

        // Buttons
        WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();

        WButton saveBtn = buttons.add(theme.button("Save")).expandX().widget();
        saveBtn.action = this::save;

        WButton cancelBtn = buttons.add(theme.button("Cancel")).expandX().widget();
        cancelBtn.action = () -> mc.setScreen(parent);
    }

    private void save() {
        String name = nameInput.get().trim();

        // Validation
        if (name.isEmpty()) {
            return;
        }

        // Check if name changed and new name already exists
        if (!name.equals(originalName)) {
            if (MCPServers.get().getConfig(name) != null) {
                return;
            }
        }

        MCPServerConfig.TransportType transport = transportInput.get();
        MCPServerConfig config = new MCPServerConfig(name, transport);

        // Set fields based on transport
        if (transport == MCPServerConfig.TransportType.STDIO) {
            String command = commandInput.get().trim();
            if (command.isEmpty()) {
                return;
            }
            config.setCommand(command);

            // Parse arguments
            String argsStr = argsInput.get().trim();
            if (!argsStr.isEmpty()) {
                String[] args = argsStr.split(",");
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
                config.setArgs(java.util.Arrays.asList(args));
            }

            // Set working directory if provided
            String workingDir = workingDirInput.get().trim();
            if (!workingDir.isEmpty()) {
                config.setWorkingDirectory(workingDir);
            }
        } else {
            String url = urlInput.get().trim();
            if (url.isEmpty()) {
                return;
            }
            config.setUrl(url);
        }

        // Parse environment variables
        String envStr = envInput.get().trim();
        if (!envStr.isEmpty()) {
            String[] pairs = envStr.split(",");
            java.util.Map<String, String> env = new java.util.HashMap<>();
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    env.put(kv[0].trim(), kv[1].trim());
                }
            }
            config.setEnv(env);
        }

        config.setAutoConnect(autoConnectInput.checked);
        config.setTimeout(timeoutInput.get());

        // Update in system
        if (MCPServers.get().update(originalName, config)) {
            mc.setScreen(parent);
            if (parent != null) {
                parent.reload();
            }
        }
    }
}
