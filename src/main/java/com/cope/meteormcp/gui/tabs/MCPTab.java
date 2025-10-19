package com.cope.meteormcp.gui.tabs;

import com.cope.meteormcp.gui.screens.MCPServersScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screen.Screen;

/**
 * MCP tab registration for Meteor Client GUI.
 * Creates the "MCP" tab that displays the server management screen.
 */
public class MCPTab extends Tab {

    public MCPTab() {
        super("MCP");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new MCPServersScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof MCPServersScreen;
    }
}
