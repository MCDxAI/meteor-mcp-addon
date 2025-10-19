package com.cope.meteormcp;

import com.cope.meteormcp.gui.tabs.MCPTab;
import com.cope.meteormcp.starscript.GeminiStarScriptIntegration;
import com.cope.meteormcp.starscript.MCPToolExecutor;
import com.cope.meteormcp.systems.MCPServerConnection;
import com.cope.meteormcp.systems.MCPServers;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meteor MCP Addon - Entry Point
 *
 * Integrates MCP (Model Context Protocol) servers with Meteor Client via StarScript.
 * Enables programmatic execution of MCP tools in any StarScript-enabled module.
 *
 * Core Pattern: {server_name.tool_name(args)} syntax available globally
 *
 * @author GhostTypes
 */
public class MeteorMCPAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteor MCP");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor MCP Addon");

        // Initialize MCPServers system (loads saved configurations)
        Systems.add(new MCPServers());
        LOG.info("MCPServers system initialized");

        // Register MCP tab in Meteor GUI
        Tabs.add(new MCPTab());
        LOG.info("MCP tab registered");

        // Connect to auto-connect servers
        MCPServers.get().connectAutoConnect();

        // Register Gemini helpers in StarScript
        GeminiStarScriptIntegration.register();
        LOG.info("Gemini StarScript functions registered");

        LOG.info("Meteor MCP Addon initialized successfully");
    }

    @Override
    public void onRegisterCategories() {
        // No custom module categories needed for this addon
    }

    @Override
    public String getPackage() {
        return "com.cope.meteormcp";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("cope", "meteor-mcp");
    }

    /**
     * Register MCP server tools to global StarScript instance.
     * Makes tools available to ALL Meteor modules that use StarScript.
     *
     * @param serverName Name of the MCP server
     * @param connection Active MCP server connection
     */
    public static void registerServerToStarScript(String serverName, MCPServerConnection connection) {
        try {
            ValueMap serverMap = new ValueMap();

            // Register each tool as a StarScript function
            for (Tool tool : connection.getTools()) {
                serverMap.set(
                    tool.name(),
                    MCPToolExecutor.createToolFunction(connection, tool)
                );
            }

            // Register globally - NOW ALL MODULES CAN USE IT!
            MeteorStarscript.ss.set(serverName, serverMap);

            LOG.info("Registered {} tools from MCP server '{}' to StarScript",
                connection.getTools().size(), serverName);

        } catch (Exception e) {
            LOG.error("Failed to register MCP server '{}' to StarScript: {}",
                serverName, e.getMessage());
        }
    }

    /**
     * Unregister MCP server from global StarScript instance.
     * Removes all tool functions when server disconnects.
     *
     * @param serverName Name of the MCP server to unregister
     */
    public static void unregisterServerFromStarScript(String serverName) {
        try {
            MeteorStarscript.ss.set(serverName, Value.null_());
            LOG.info("Unregistered MCP server '{}' from StarScript", serverName);
        } catch (Exception e) {
            LOG.error("Failed to unregister MCP server '{}' from StarScript: {}",
                serverName, e.getMessage());
        }
    }
}
