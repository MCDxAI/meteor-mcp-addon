package com.cope.meteormcp.gui.screens;

import com.cope.meteormcp.starscript.MCPToolExecutor;
import com.cope.meteormcp.systems.MCPServerConnection;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * Browser for available MCP tools from a connected server.
 * Displays tool information and example StarScript syntax.
 */
public class MCPToolsScreen extends WindowScreen {
    private final Screen parent;
    private final MCPServerConnection connection;

    public MCPToolsScreen(GuiTheme theme, Screen parent, MCPServerConnection connection) {
        super(theme, "MCP Tools - " + connection.getConfig().getName());
        this.parent = parent;
        this.connection = connection;
    }

    @Override
    public void initWidgets() {
        // Server info
        add(theme.label("Server: " + connection.getConfig().getName()));

        String status = connection.isConnected() ? "Connected" : "Disconnected";
        add(theme.label("Status: " + status));

        add(theme.horizontalSeparator()).expandX();

        // Back button
        WButton backBtn = add(theme.button("Back")).expandX().widget();
        backBtn.action = () -> client.setScreen(parent);

        add(theme.horizontalSeparator()).expandX();

        // Tools list
        List<Tool> tools = connection.getTools();

        if (tools.isEmpty()) {
            add(theme.label("No tools available."));
            if (!connection.isConnected()) {
                add(theme.label("Server is not connected."));
            }
        } else {
            add(theme.label("Tools (" + tools.size() + "):"));

            for (Tool tool : tools) {
                WSection section = add(theme.section(tool.name(), true)).expandX().widget();
                section.add(theme.label("Name: " + tool.name()));

                // Description
                if (tool.description() != null && !tool.description().isEmpty()) {
                    section.add(theme.label("Description:"));
                    section.add(theme.label("  " + tool.description()));
                }

                // Parameters
                List<String> paramNames = MCPToolExecutor.getParameterNames(tool);
                if (!paramNames.isEmpty()) {
                    section.add(theme.label("Parameters:"));

                    WTable paramsTable = section.add(theme.table()).expandX().widget();
                    List<String> required = MCPToolExecutor.getRequiredParameters(tool);

                    for (String paramName : paramNames) {
                        String type = MCPToolExecutor.getParameterType(tool, paramName);
                        boolean isRequired = required.contains(paramName);
                        String reqStr = isRequired ? " (required)" : " (optional)";

                        paramsTable.add(theme.label("  - " + paramName));
                        paramsTable.add(theme.label(type));
                        paramsTable.add(theme.label(reqStr));
                        paramsTable.row();
                    }
                }

                // StarScript example
                String example = MCPToolExecutor.generateExampleSyntax(
                    connection.getConfig().getName(),
                    tool
                );
                section.add(theme.label("StarScript Usage:"));
                section.add(theme.label("  " + example));

                // Copy button
                WButton copyBtn = section.add(theme.button("Copy StarScript Syntax")).expandX().widget();
                final String syntaxToCopy = example;
                copyBtn.action = () -> {
                    client.keyboard.setClipboard(syntaxToCopy);
                };
            }
        }
    }
}
