package com.cope.meteormcp.systems;

import com.cope.meteormcp.MeteorMCPAddon;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * System manager for MCP server connections.
 * Follows Meteor's System<T> pattern for persistent data management.
 *
 * @author GhostTypes
 */
public class MCPServers extends System<MCPServers> {
    private final Map<String, MCPServerConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, MCPServerConfig> configs = new ConcurrentHashMap<>();

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

            // Register to StarScript
            MeteorMCPAddon.registerServerToStarScript(name, connection);

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

            // Unregister from StarScript
            MeteorMCPAddon.unregisterServerFromStarScript(name);
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

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        NbtList serversList = new NbtList();
        for (MCPServerConfig config : configs.values()) {
            serversList.add(config.toTag());
        }
        tag.put("servers", serversList);

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

        return this;
    }
}
