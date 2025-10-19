package com.cope.meteormcp.starscript;

import com.cope.meteormcp.MeteorMCPAddon;
import com.cope.meteormcp.systems.MCPServerConnection;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.meteordev.starscript.Starscript;
import org.meteordev.starscript.value.Value;

import java.util.*;

/**
 * Executes MCP tools from StarScript function calls.
 * Bridges between StarScript syntax and MCP tool execution.
 *
 * @author GhostTypes
 */
public class MCPToolExecutor {

    /**
     * Execute an MCP tool from StarScript
     *
     * @param ss StarScript instance
     * @param argCount Number of arguments on stack
     * @param connection MCP server connection
     * @param tool Tool to execute
     * @return Result as StarScript Value
     */
    public static Value execute(
        Starscript ss,
        int argCount,
        MCPServerConnection connection,
        Tool tool
    ) {
        try {
            // Validate connection
            if (!connection.isConnected()) {
                MeteorMCPAddon.LOG.warn("Attempted to call tool {} on disconnected server {}",
                    tool.name(), connection.getConfig().getName());
                return Value.null_();
            }

            // Extract arguments from StarScript stack
            Map<String, Object> args = extractArguments(ss, argCount, tool);

            // Execute MCP tool
            CallToolResult result = connection.callTool(tool.name(), args);

            // Convert result to StarScript Value
            return MCPValueConverter.toValue(result);

        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Error executing MCP tool {} on server {}: {}",
                tool.name(), connection.getConfig().getName(), e.getMessage());
            return Value.null_();
        }
    }

    /**
     * Extract arguments from StarScript stack and map to tool parameters.
     *
     * @param ss StarScript runtime
     * @param argCount number of incoming arguments
     * @param tool tool metadata that provides schema information
     * @return map of argument names to JSON-compatible values
     */
    private static Map<String, Object> extractArguments(
        Starscript ss,
        int argCount,
        Tool tool
    ) {
        // Get parameter names from tool schema
        List<String> paramNames = getParameterNames(tool);

        // Extract arguments using value converter
        return MCPValueConverter.extractArgumentsAsMap(ss, argCount, paramNames);
    }

    /**
     * Get parameter names from a tool's input schema.
     *
     * @param tool tool metadata
     * @return ordered list of parameter names
     */
    public static List<String> getParameterNames(Tool tool) {
        List<String> names = new ArrayList<>();

        try {
            // Get input schema from tool
            JsonSchema schema = tool.inputSchema();
            Map<String, Object> properties = schema != null ? schema.properties() : null;
            if (properties != null) {
                names.addAll(properties.keySet());
            }
        } catch (Exception e) {
            MeteorMCPAddon.LOG.warn("Could not extract parameter names from tool {}: {}",
                tool.name(), e.getMessage());
        }

        return names;
    }

    /**
     * Create a StarScript function for an MCP tool. The returned value is registered in
     * {@link org.meteordev.starscript.value.ValueMap} instances.
     *
     * @param connection active connection for executing the tool
     * @param tool tool metadata
     * @return callable StarScript function
     */
    public static Value createToolFunction(
        MCPServerConnection connection,
        Tool tool
    ) {
        return Value.function((ss, argCount) -> execute(ss, argCount, connection, tool));
    }

    /**
     * Generate example StarScript syntax for a tool.
     *
     * @param serverName StarScript namespace assigned to the server
     * @param tool tool metadata
     * @return formatted StarScript snippet
     */
    public static String generateExampleSyntax(String serverName, Tool tool) {
        StringBuilder syntax = new StringBuilder();
        syntax.append("{").append(serverName).append(".").append(tool.name()).append("(");

        List<String> paramNames = getParameterNames(tool);
        for (int i = 0; i < paramNames.size(); i++) {
            if (i > 0) syntax.append(", ");
            syntax.append(paramNames.get(i));
        }

        syntax.append(")}");
        return syntax.toString();
    }

    /**
     * Resolve the set of required parameter names from a tool schema.
     *
     * @param tool tool metadata
     * @return ordered list of required parameter names
     */
    public static List<String> getRequiredParameters(Tool tool) {
        List<String> required = new ArrayList<>();

        try {
            JsonSchema schema = tool.inputSchema();
            List<String> requiredList = schema != null ? schema.required() : null;
            if (requiredList != null) {
                required.addAll(requiredList);
            }
        } catch (Exception e) {
            MeteorMCPAddon.LOG.warn("Could not extract required parameters from tool {}: {}",
                tool.name(), e.getMessage());
        }

        return required;
    }

    /**
     * Determine the declared type for a parameter. Defaults to {@code any} when no schema
     * information is available.
     *
     * @param tool tool metadata
     * @param paramName parameter name to inspect
     * @return schema-provided type or {@code any}
     */
    public static String getParameterType(Tool tool, String paramName) {
        try {
            JsonSchema schema = tool.inputSchema();
            Map<String, Object> properties = schema != null ? schema.properties() : null;
            if (properties != null) {
                Object paramSchema = properties.get(paramName);
                if (paramSchema instanceof Map<?, ?> paramSchemaMap) {
                    Object type = paramSchemaMap.get("type");
                    if (type != null) return type.toString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return "any";
    }
}
