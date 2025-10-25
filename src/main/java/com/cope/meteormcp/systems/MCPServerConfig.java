package com.cope.meteormcp.systems;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.*;

/**
 * Configuration model for an MCP server connection.
 * Stores all necessary information to connect to and manage an MCP server.
 *
 * @author GhostTypes
 */
public class MCPServerConfig {
    private String name;
    private TransportType transport;
    private String command;
    private List<String> args;
    private String workingDirectory;
    private String url;
    private Map<String, String> env;
    private boolean autoConnect;
    private int timeout;

    /**
     * Supported transport layers for connecting to an MCP server. Only STDIO is wired up
     * today, but the enum allows the GUI to present future options.
     */
    public enum TransportType {
        STDIO,
        SSE,
        HTTP
    }

    /**
     * Create a new configuration shell with sensible defaults. Callers can populate the
     * remaining fields via setters before persisting or attempting a connection.
     *
     * @param name      unique identifier used throughout the addon and StarScript
     * @param transport transport type describing how to reach the MCP server
     */
    public MCPServerConfig(String name, TransportType transport) {
        this.name = name;
        this.transport = transport;
        this.args = new ArrayList<>();
        this.env = new HashMap<>();
        this.autoConnect = false;
        this.timeout = 5000; // 5 seconds default
    }

    // Getters
    public String getName() { return name; }
    public TransportType getTransport() { return transport; }
    public String getCommand() { return command; }
    public List<String> getArgs() { return args; }
    public String getWorkingDirectory() { return workingDirectory; }
    public String getUrl() { return url; }
    public Map<String, String> getEnv() { return env; }
    public boolean isAutoConnect() { return autoConnect; }
    public int getTimeout() { return timeout; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setTransport(TransportType transport) { this.transport = transport; }
    public void setCommand(String command) { this.command = command; }
    public void setArgs(List<String> args) { this.args = args != null ? args : new ArrayList<>(); }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
    public void setUrl(String url) { this.url = url; }
    public void setEnv(Map<String, String> env) { this.env = env != null ? env : new HashMap<>(); }
    public void setAutoConnect(boolean autoConnect) { this.autoConnect = autoConnect; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    /**
     * Serialize configuration to NBT for persistence.
     *
     * @return NBT tag containing the serialized configuration
     */
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putString("transport", transport.name());
        tag.putBoolean("autoConnect", autoConnect);
        tag.putInt("timeout", timeout);

        if (command != null) {
            tag.putString("command", command);
        }

        if (workingDirectory != null) {
            tag.putString("workingDirectory", workingDirectory);
        }

        if (url != null) {
            tag.putString("url", url);
        }

        // Serialize args list
        if (args != null && !args.isEmpty()) {
            NbtList argsList = new NbtList();
            for (String arg : args) {
                argsList.add(NbtString.of(arg));
            }
            tag.put("args", argsList);
        }

        // Serialize env map
        if (env != null && !env.isEmpty()) {
            NbtCompound envTag = new NbtCompound();
            for (Map.Entry<String, String> entry : env.entrySet()) {
                envTag.putString(entry.getKey(), entry.getValue());
            }
            tag.put("env", envTag);
        }

        return tag;
    }

    /**
     * Deserialize configuration from NBT.
     *
     * @param tag serialized configuration tag
     * @return deserialized configuration instance
     */
    public static MCPServerConfig fromTag(NbtCompound tag) {
        if (tag.getString("name").isEmpty() || tag.getString("transport").isEmpty()) {
            throw new RuntimeException("Invalid MCPServerConfig NBT: missing required fields");
        }

        String name = tag.getString("name").orElseThrow();
        TransportType transport = TransportType.valueOf(tag.getString("transport").orElseThrow());

        MCPServerConfig config = new MCPServerConfig(name, transport);
        config.setAutoConnect(tag.getBoolean("autoConnect").orElse(false));
        config.setTimeout(tag.getInt("timeout").orElse(5000));

        tag.getString("command").ifPresent(config::setCommand);
        tag.getString("workingDirectory").ifPresent(config::setWorkingDirectory);
        tag.getString("url").ifPresent(config::setUrl);

        // Deserialize args list
        if (tag.contains("args")) {
            NbtElement element = tag.get("args");
            if (element instanceof NbtList argsList) {
                List<String> args = new ArrayList<>();
                for (NbtElement argElement : argsList) {
                    argElement.asString().ifPresent(args::add);
                }
                config.setArgs(args);
            }
        }

        // Deserialize env map (using old approach)
        if (tag.contains("env")) {
            NbtCompound envTag = tag.getCompound("env").orElse(null);
            if (envTag != null) {
                Map<String, String> env = new HashMap<>();
                for (String key : envTag.getKeys()) {
                    envTag.getString(key).ifPresent(value -> env.put(key, value));
                }
                config.setEnv(env);
            }
        }

        return config;
    }

    /**
     * Validate configuration against the requirements for the selected transport.
     *
     * @return {@code true} when the configuration has the minimum required data
     */
    public boolean isValid() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        switch (transport) {
            case STDIO:
                return command != null && !command.trim().isEmpty();
            case SSE:
            case HTTP:
                return url != null && !url.trim().isEmpty();
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return name + " (" + transport + ")";
    }
}
