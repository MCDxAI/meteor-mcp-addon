package com.cope.meteormcp.gemini;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility that transforms MCP tool metadata into Gemini FunctionDeclarations.
 */
public final class MCPToGeminiBridge {
    private static final int MAX_FUNCTION_NAME_LENGTH = 64;
    private static final ConcurrentHashMap<String, ToolCallRoute> ROUTING = new ConcurrentHashMap<>();

    private MCPToGeminiBridge() {
    }

    public static FunctionDeclaration convertMCPToolToGemini(McpSchema.Tool mcpTool, String serverName) {
        if (mcpTool == null || serverName == null) {
            throw new IllegalArgumentException("Tool and server name must be provided.");
        }

        String functionName = buildFunctionName(serverName, mcpTool.name());

        FunctionDeclaration.Builder builder = FunctionDeclaration.builder()
            .name(functionName);

        String description = composeDescription(mcpTool, serverName);
        if (!description.isBlank()) {
            builder.description(description);
        }

        Schema parameters = convertJsonSchema(mcpTool.inputSchema());
        if (parameters != null) {
            builder.parameters(parameters);
        }

        ROUTING.put(functionName, new ToolCallRoute(serverName, mcpTool.name()));

        return builder.build();
    }

    public static ToolCallRoute resolveRoute(String functionName) {
        ToolCallRoute route = ROUTING.get(functionName);
        if (route != null) {
            return route;
        }

        // Fallback: split on first underscore if the mapping was not recorded (should be rare).
        String fallback = functionName != null ? functionName : "";
        int idx = fallback.indexOf('_');
        if (idx <= 0 || idx == fallback.length() - 1) {
            throw new IllegalArgumentException("Cannot resolve Gemini function route: " + functionName);
        }
        String server = fallback.substring(0, idx);
        String tool = fallback.substring(idx + 1);
        return new ToolCallRoute(server, tool);
    }

    public static final class ToolCallRoute {
        private final String serverName;
        private final String toolName;

        public ToolCallRoute(String serverName, String toolName) {
            this.serverName = serverName;
            this.toolName = toolName;
        }

        public String serverName() {
            return serverName;
        }

        public String toolName() {
            return toolName;
        }
    }

    private static String buildFunctionName(String serverName, String toolName) {
        String normalizedServer = normalizeSegment(serverName, "server");
        String normalizedTool = normalizeSegment(toolName, "tool");

        String base = normalizedServer + "_" + normalizedTool;
        if (base.length() > MAX_FUNCTION_NAME_LENGTH) {
            base = base.substring(0, MAX_FUNCTION_NAME_LENGTH);
        }

        ToolCallRoute existing = ROUTING.get(base);
        if (existing != null
            && existing.serverName().equals(serverName)
            && existing.toolName().equals(toolName)) {
            return base;
        }

        String candidate = base;
        int suffix = 1;
        while (ROUTING.containsKey(candidate)) {
            ToolCallRoute route = ROUTING.get(candidate);
            if (route != null
                && route.serverName().equals(serverName)
                && route.toolName().equals(toolName)) {
                return candidate;
            }

            String suffixStr = "_" + suffix++;
            int cut = Math.min(candidate.length(), MAX_FUNCTION_NAME_LENGTH - suffixStr.length());
            candidate = candidate.substring(0, cut) + suffixStr;
        }

        return candidate;
    }

    private static String normalizeSegment(String value, String fallback) {
        if (value == null || value.isBlank()) {
            value = fallback;
        }

        String sanitized = value.trim()
            .replaceAll("[^A-Za-z0-9_\\-\\.]", "_")
            .replaceAll("_+", "_");

        if (sanitized.isEmpty()) {
            sanitized = fallback;
        }

        if (!Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_') {
            sanitized = "_" + sanitized;
        }

        if (sanitized.length() > MAX_FUNCTION_NAME_LENGTH / 2) {
            sanitized = sanitized.substring(0, MAX_FUNCTION_NAME_LENGTH / 2);
        }

        return sanitized;
    }

    private static Schema convertJsonSchema(JsonSchema schema) {
        if (schema == null) {
            return Schema.builder()
                .type(Type.Known.OBJECT)
                .build();
        }

        Map<String, Object> root = new LinkedHashMap<>();
        if (schema.type() != null) root.put("type", schema.type());
        if (schema.properties() != null) root.put("properties", schema.properties());
        if (schema.required() != null) root.put("required", schema.required());
        if (schema.additionalProperties() != null) root.put("additionalProperties", schema.additionalProperties());
        if (schema.defs() != null) root.put("$defs", schema.defs());
        if (schema.definitions() != null) root.put("definitions", schema.definitions());

        return convertSchemaObject(root, true);
    }

    @SuppressWarnings("unchecked")
    private static Schema convertSchemaObject(Object raw, boolean forceObject) {
        Schema.Builder builder = Schema.builder();
        boolean typeSet = false;

        if (raw instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if (type instanceof String typeStr && !typeStr.isBlank()) {
                builder.type(typeStr);
                typeSet = true;
            }

            Object description = map.get("description");
            if (description instanceof String desc && !desc.isBlank()) {
                builder.description(desc);
            }

            Object title = map.get("title");
            if (title instanceof String titleStr && !titleStr.isBlank()) {
                builder.title(titleStr);
            }

            Object format = map.get("format");
            if (format instanceof String fmt && !fmt.isBlank()) {
                builder.format(fmt);
            }

            Object defaultValue = map.get("default");
            if (defaultValue != null) {
                builder.default_(defaultValue);
            }

            Object example = map.get("example");
            if (example != null) {
                builder.example(example);
            }

            Object enumValues = map.get("enum");
            if (enumValues instanceof List<?> list && !list.isEmpty()) {
                builder.enum_(list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList());
            }

            Object properties = map.get("properties");
            if (properties instanceof Map<?, ?> props && !props.isEmpty()) {
                Map<String, Schema> childProps = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : props.entrySet()) {
                    if (entry.getKey() == null) continue;
                    Schema child = convertSchemaObject(entry.getValue(), false);
                    childProps.put(entry.getKey().toString(), child);
                }
                builder.properties(childProps);
                if (!typeSet) {
                    builder.type(Type.Known.OBJECT);
                    typeSet = true;
                }
            }

            Object required = map.get("required");
            if (required instanceof List<?> requiredList && !requiredList.isEmpty()) {
                List<String> names = requiredList.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
                builder.required(names);
            }

            Object anyOf = map.get("anyOf");
            if (anyOf instanceof List<?> anyOfList && !anyOfList.isEmpty()) {
                builder.anyOf(convertSchemaList(anyOfList));
            }

            Object items = map.get("items");
            if (items != null) {
                builder.items(convertSchemaObject(items, false));
                if (!typeSet) {
                    builder.type(Type.Known.ARRAY);
                    typeSet = true;
                }
            }

            Double minimum = asDouble(map.get("minimum"));
            if (minimum != null) builder.minimum(minimum);
            Double maximum = asDouble(map.get("maximum"));
            if (maximum != null) builder.maximum(maximum);
            Long minItems = asLong(map.get("minItems"));
            if (minItems != null) builder.minItems(minItems);
            Long maxItems = asLong(map.get("maxItems"));
            if (maxItems != null) builder.maxItems(maxItems);
            Long minLength = asLong(map.get("minLength"));
            if (minLength != null) builder.minLength(minLength);
            Long maxLength = asLong(map.get("maxLength"));
            if (maxLength != null) builder.maxLength(maxLength);
            Long minProperties = asLong(map.get("minProperties"));
            if (minProperties != null) builder.minProperties(minProperties);
            Long maxProperties = asLong(map.get("maxProperties"));
            if (maxProperties != null) builder.maxProperties(maxProperties);

            Object nullable = map.get("nullable");
            if (nullable instanceof Boolean bool) {
                builder.nullable(bool);
            }

            Object propertyOrdering = map.get("propertyOrdering");
            if (propertyOrdering instanceof List<?> ordering && !ordering.isEmpty()) {
                List<String> order = ordering.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
                builder.propertyOrdering(order);
            }
        } else if (raw instanceof JsonSchema nested) {
            return convertJsonSchema(nested);
        }

        if (!typeSet && forceObject) {
            builder.type(Type.Known.OBJECT);
        } else if (!typeSet) {
            builder.type(Type.Known.STRING);
        }

        return builder.build();
    }

    private static List<Schema> convertSchemaList(List<?> list) {
        List<Schema> schemas = new ArrayList<>();
        for (Object entry : list) {
            schemas.add(convertSchemaObject(entry, false));
        }
        return schemas;
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static String composeDescription(McpSchema.Tool tool, String serverName) {
        StringBuilder sb = new StringBuilder();
        if (tool.description() != null && !tool.description().isBlank()) {
            sb.append(tool.description().trim());
        } else if (tool.title() != null && !tool.title().isBlank()) {
            sb.append(tool.title().trim());
        }

        if (sb.length() == 0) {
            sb.append("Tool ").append(tool.name());
        }

        sb.append(" (server: ").append(serverName).append(")");

        return sb.toString();
    }
}
