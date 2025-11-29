package com.cope.meteormcp.systems;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.commands.MCPToolCommand;
import com.cope.meteormcp.gemini.GeminiClientManager;
import com.mojang.brigadier.CommandDispatcher;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.command.CommandSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * System manager for MCP server connections.
 * Follows Meteor's System<T> pattern for persistent data management.
 *
 * @author GhostTypes
 */
public class MCPServers extends System<MCPServers> {
    private final Map<String, MCPServerConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, MCPServerConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, List<MCPToolCommand>> registeredCommands = new ConcurrentHashMap<>();
    private GeminiConfig geminiConfig = new GeminiConfig();

    /**
     * Creates the system container used by Meteor's global registry. The system name
     * matches the persisted NBT filename.
     */
    public MCPServers() {
        super("mcp-servers");
    }

    /**
     * Convenience accessor that mirrors Meteor's conventional static getter pattern.
     *
     * @return singleton instance managed by {@link Systems}
     */
    public static MCPServers get() {
        return Systems.get(MCPServers.class);
    }

    /**
     * Register a new MCP server configuration with the system registry.
     *
     * @param config validated configuration to store
     * @return {@code true} when the config was added, {@code false} when it already existed or was invalid
     */
    public boolean add(MCPServerConfig config) {
        if (config == null || !config.isValid()) {
            MeteorMCPAddon.LOG.warn("Attempted to add invalid MCP server config");
            return false;
        }

        if (configs.containsKey(config.getName())) {
            MeteorMCPAddon.LOG.warn("MCP server with name {} already exists", config.getName());
            return false;
        }

        configs.put(config.getName(), config);
        MeteorMCPAddon.LOG.info("Added MCP server configuration: {}", config.getName());
        return true;
    }

    /**
     * Remove a server configuration and disconnect the associated connection if one is
     * active.
     *
     * @param name name of the server to remove
     * @return {@code true} when a configuration was removed
     */
    public boolean remove(String name) {
        // Disconnect first if connected
        if (connections.containsKey(name)) {
            disconnect(name);
        }

        MCPServerConfig removed = configs.remove(name);
        if (removed != null) {
            MeteorMCPAddon.LOG.info("Removed MCP server configuration: {}", name);
            return true;
        }
        return false;
    }

    /**
     * Replace an existing server configuration with new settings, handling renames as
     * needed.
     *
     * @param oldName   current identifier of the server
     * @param newConfig configuration to store (may change the name)
     * @return {@code true} if the configuration was updated
     */
    public boolean update(String oldName, MCPServerConfig newConfig) {
        if (!configs.containsKey(oldName)) {
            return false;
        }

        // If name changed, handle rename
        if (!oldName.equals(newConfig.getName())) {
            if (configs.containsKey(newConfig.getName())) {
                MeteorMCPAddon.LOG.warn("Cannot rename to {}, name already exists", newConfig.getName());
                return false;
            }

            // Disconnect old connection
            if (connections.containsKey(oldName)) {
                disconnect(oldName);
            }

            configs.remove(oldName);
        } else {
            // Just disconnect if connected (will need to reconnect with new config)
            if (connections.containsKey(oldName)) {
                disconnect(oldName);
            }
        }

        configs.put(newConfig.getName(), newConfig);
        MeteorMCPAddon.LOG.info("Updated MCP server configuration: {}", newConfig.getName());
        return true;
    }

    /**
     * Establish a connection for the specified server if it is not already connected.
     *
     * @param name server identifier
     * @return {@code true} when the connection succeeds or is already active
     */
    public boolean connect(String name) {
        MCPServerConfig config = configs.get(name);
        if (config == null) {
            MeteorMCPAddon.LOG.warn("Cannot connect, server {} not found", name);
            return false;
        }

        // Already connected
        if (connections.containsKey(name) && connections.get(name).isConnected()) {
            return true;
        }

        MCPServerConnection connection = new MCPServerConnection(config);
        if (connection.connect()) {
            connections.put(name, connection);

            Runnable registration = () -> {
                MeteorMCPAddon.registerServerToStarScript(name, connection);
                registerCommandsForServer(name, connection);
            };

            if (mc != null) {
                mc.execute(registration);
            } else {
                registration.run();
            }

            return true;
        }

        return false;
    }

    /**
     * Disconnect from a server and unregister its StarScript hooks.
     *
     * @param name server identifier
     */
    public void disconnect(String name) {
        MCPServerConnection connection = connections.remove(name);
        if (connection != null) {
            connection.disconnect();

            // Clear async results for this server to free memory
            com.cope.meteormcp.starscript.MCPToolExecutor.clearAsyncResultsForServer(name);

            // Unregister from StarScript
            Runnable cleanup = () -> {
                MeteorMCPAddon.unregisterServerFromStarScript(name);
                unregisterCommandsForServer(name);
            };

            if (mc != null) {
                mc.execute(cleanup);
            } else {
                cleanup.run();
            }
        }
    }

    /**
     * Retrieve the live connection for the given server.
     *
     * @param name server identifier
     * @return connection or {@code null} when disconnected
     */
    public MCPServerConnection getConnection(String name) {
        return connections.get(name);
    }

    /**
     * Retrieve a stored configuration by name.
     *
     * @param name server identifier
     * @return configuration or {@code null} when missing
     */
    public MCPServerConfig getConfig(String name) {
        return configs.get(name);
    }

    /**
     * @return copy of all configured server names
     */
    public Set<String> getServerNames() {
        return new HashSet<>(configs.keySet());
    }

    /**
     * @return copy of every stored configuration
     */
    public Collection<MCPServerConfig> getAllConfigs() {
        return new ArrayList<>(configs.values());
    }

    /**
     * @return current Gemini configuration snapshot
     */
    public GeminiConfig getGeminiConfig() {
        return geminiConfig;
    }

    /**
     * Replace the stored Gemini configuration and invalidate any cached client.
     *
     * @param config new configuration instance (falls back to defaults when {@code null})
     */
    public void setGeminiConfig(GeminiConfig config) {
        GeminiConfig next = config != null ? config : new GeminiConfig();
        if (!Objects.equals(this.geminiConfig, next)) {
            this.geminiConfig = next;
            GeminiClientManager.getInstance().invalidateClient();
        } else {
            this.geminiConfig = next;
        }
    }

    /**
     * @return snapshot of all active connections
     */
    public Collection<MCPServerConnection> getAllConnections() {
        return new ArrayList<>(connections.values());
    }

    /**
     * Determine whether the named server currently has an active connection.
     *
     * @param name server identifier
     * @return {@code true} when connected
     */
    public boolean isConnected(String name) {
        MCPServerConnection conn = connections.get(name);
        return conn != null && conn.isConnected();
    }

    /**
     * Apply the provided consumer to all stored configurations.
     *
     * @param consumer callback invoked with each name/config pair
     */
    public void forEach(BiConsumer<String, MCPServerConfig> consumer) {
        configs.forEach(consumer);
    }

    /**
     * Attempt to connect every server that opts into auto-connect on startup.
     */
    public void connectAutoConnect() {
        for (MCPServerConfig config : configs.values()) {
            if (config.isAutoConnect()) {
                connect(config.getName());
            }
        }
    }

    /**
     * Disconnect and unregister every server currently connected.
     */
    public void disconnectAll() {
        new ArrayList<>(connections.keySet()).forEach(this::disconnect);
    }

    private void registerCommandsForServer(String serverName, MCPServerConnection connection) {
        if (connection == null || !connection.isConnected()) {
            return;
        }

        unregisterCommandsForServer(serverName);

        List<Tool> tools = connection.getTools();
        if (tools == null || tools.isEmpty()) {
            return;
        }

        List<MCPToolCommand> commandsForServer = new ArrayList<>();
        for (Tool tool : tools) {
            try {
                MCPToolCommand command = new MCPToolCommand(serverName, tool);
                Commands.add(command);
                commandsForServer.add(command);
            } catch (Exception e) {
                MeteorMCPAddon.LOG.error("Failed to register command for tool {}:{} - {}", serverName, tool.name(), e.getMessage());
            }
        }

        if (!commandsForServer.isEmpty()) {
            registeredCommands.put(serverName, commandsForServer);
            refreshCommandRegistry();
            MeteorMCPAddon.LOG.info("Registered {} MCP commands for server '{}'", commandsForServer.size(), serverName);
        }
    }

    private void unregisterCommandsForServer(String serverName) {
        List<MCPToolCommand> commands = registeredCommands.remove(serverName);
        boolean modified = false;

        if (commands != null && !commands.isEmpty()) {
            modified |= Commands.COMMANDS.removeAll(commands);
        }

        modified |= Commands.COMMANDS.removeIf(command -> command.getName().startsWith(serverName + ":"));

        if (modified) {
            refreshCommandRegistry();
            MeteorMCPAddon.LOG.info("Unregistered MCP commands for server '{}'", serverName);
        }
    }

    private void refreshCommandRegistry() {
        Commands.COMMANDS.sort(Comparator.comparing(Command::getName));

        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        for (Command command : Commands.COMMANDS) {
            command.registerTo(dispatcher);
        }
        Commands.DISPATCHER = dispatcher;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        NbtList serversList = new NbtList();
        for (MCPServerConfig config : configs.values()) {
            serversList.add(config.toTag());
        }
        tag.put("servers", serversList);
        tag.put("gemini", geminiConfig.toTag());

        return tag;
    }

    @Override
    public MCPServers fromTag(NbtCompound tag) {
        if (tag.contains("servers")) {
            NbtElement element = tag.get("servers");
            if (element instanceof NbtList serversList) {
                for (NbtElement serverElement : serversList) {
                    try {
                        if (serverElement instanceof NbtCompound serverTag) {
                            MCPServerConfig config = MCPServerConfig.fromTag(serverTag);
                            configs.put(config.getName(), config);
                        }
                    } catch (Exception e) {
                        MeteorMCPAddon.LOG.error("Failed to load MCP server config: {}", e.getMessage());
                    }
                }
            }

            MeteorMCPAddon.LOG.info("Loaded {} MCP server configurations", configs.size());
        }

        if (tag.contains("gemini")) {
            tag.getCompound("gemini").ifPresent(geminiTag -> {
                try {
                    this.geminiConfig = GeminiConfig.fromTag(geminiTag);
                } catch (Exception e) {
                    MeteorMCPAddon.LOG.error("Failed to load Gemini configuration: {}", e.getMessage());
                    this.geminiConfig = new GeminiConfig();
                }
            });
        } else {
            this.geminiConfig = new GeminiConfig();
        }

        return this;
    }
}
