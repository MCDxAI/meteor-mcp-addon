package com.cope.meteormcp.ollama;

import io.github.ollama4j.tools.ToolFunction;
import io.github.ollama4j.tools.Tools;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts MCP tool schemas to Ollama's Tools.Tool format and maintains routing metadata.
 */
public final class MCPToOllamaBridge {
    private static final ConcurrentHashMap<String, ToolCallRoute> ROUTING = new ConcurrentHashMap<>();

    private MCPToOllamaBridge() {}

    /**
     * Convert an MCP tool to an Ollama Tools.Tool with routing metadata and a tool function.
     */
    public static Tools.Tool convertMCPToolToOllama(McpSchema.Tool mcpTool, String serverName, ToolFunction function) {
        if (mcpTool == null || serverName == null) {
            throw new IllegalArgumentException("Tool and server name must be provided.");
        }

        String functionName = buildFunctionName(serverName, mcpTool.name());
        String description = composeDescription(mcpTool, serverName);

        // Build parameters
        Tools.Parameters parameters = convertParameters(mcpTool.inputSchema());

        // Build tool spec
        Tools.ToolSpec spec = Tools.ToolSpec.builder()
            .name(functionName)
            .description(description)
            .parameters(parameters)
            .build();

        ROUTING.put(functionName, new ToolCallRoute(serverName, mcpTool.name()));

        return Tools.Tool.builder()
            .toolSpec(spec)
            .toolFunction(function)
            .build();
    }

    /**
     * Look up routing info for a function name returned by Ollama.
     */
    public static ToolCallRoute resolveRoute(String functionName) {
        ToolCallRoute route = ROUTING.get(functionName);
        if (route != null) return route;

        String fallback = functionName != null ? functionName : "";
        int idx = fallback.indexOf('_');
        if (idx <= 0 || idx == fallback.length() - 1) {
            throw new IllegalArgumentException("Cannot resolve Ollama function route: " + functionName);
        }
        return new ToolCallRoute(fallback.substring(0, idx), fallback.substring(idx + 1));
    }

    public record ToolCallRoute(String serverName, String toolName) {}

    private static Tools.Parameters convertParameters(JsonSchema schema) {
        if (schema == null || schema.properties() == null || schema.properties().isEmpty()) {
            return new Tools.Parameters();
        }

        Set<String> required = schema.required() != null
            ? new HashSet<>(schema.required())
            : Collections.emptySet();

        Map<String, Tools.Property> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schema.properties().entrySet()) {
            String propName = entry.getKey();
            String propType = extractType(entry.getValue());
            String propDesc = extractDescription(entry.getValue());
            boolean isRequired = required.contains(propName);

            Tools.Property property = Tools.Property.builder()
                .type(propType.isEmpty() ? "string" : propType)
                .description(propDesc.isEmpty() ? propName : propDesc)
                .required(isRequired)
                .build();

            properties.put(propName, property);
        }

        List<String> requiredList = new ArrayList<>(required);
        return new Tools.Parameters(properties, requiredList);
    }

    private static String buildFunctionName(String serverName, String toolName) {
        String normalized = normalizeSegment(serverName) + "_" + normalizeSegment(toolName);
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        return normalized;
    }

    private static String normalizeSegment(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.trim()
            .replaceAll("[^A-Za-z0-9_]", "_")
            .replaceAll("_+", "_");
    }

    @SuppressWarnings("unchecked")
    private static String extractType(Object schema) {
        if (schema instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if (type != null) return type.toString();
        }
        return "string";
    }

    @SuppressWarnings("unchecked")
    private static String extractDescription(Object schema) {
        if (schema instanceof Map<?, ?> map) {
            Object desc = map.get("description");
            if (desc != null) return desc.toString();
        }
        return "";
    }

    private static String composeDescription(McpSchema.Tool tool, String serverName) {
        StringBuilder sb = new StringBuilder();
        if (tool.description() != null && !tool.description().isBlank()) {
            sb.append(tool.description().trim());
        } else if (tool.title() != null && !tool.title().isBlank()) {
            sb.append(tool.title().trim());
        } else {
            sb.append("Tool ").append(tool.name());
        }
        sb.append(" (server: ").append(serverName).append(")");
        return sb.toString();
    }
}
