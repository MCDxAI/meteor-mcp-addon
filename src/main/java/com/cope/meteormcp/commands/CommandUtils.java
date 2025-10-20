package com.cope.meteormcp.commands;

import com.cope.meteormcp.MeteorMCPAddon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import meteordevelopment.meteorclient.commands.Command;

/**
 * Utility helpers shared between MCP command implementations.
 */
public final class CommandUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    static {
        MAPPER.findAndRegisterModules();
    }

    private static final Set<String> BOOLEAN_TRUE = Set.of("true", "1", "yes", "on");
    private static final Set<String> BOOLEAN_FALSE = Set.of("false", "0", "no", "off");

    private CommandUtils() {
    }

    public static Map<String, Object> parseArguments(String argsString, Tool tool) {
        JsonSchema schema = tool != null ? tool.inputSchema() : null;
        return parseArguments(argsString, schema);
    }

    public static Map<String, Object> parseArguments(String argsString, JsonSchema schema) {
        String raw = argsString == null ? "" : argsString.trim();
        if (raw.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> schemaProperties = schema != null ? schema.properties() : Collections.emptyMap();

        // JSON literal shortcut
        if (looksLikeJson(raw)) {
            try {
                Object parsed = MAPPER.readValue(raw, Object.class);
                if (parsed instanceof Map<?, ?> map) {
                    return normalizeJsonMap(map);
                }
                // Wrap raw JSON array or scalar so we still send something meaningful
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("value", parsed);
                return payload;
            } catch (JsonProcessingException e) {
                MeteorMCPAddon.LOG.debug("Argument JSON parsing failed, falling back to token parsing: {}", e.getMessage());
            }
        }

        List<String> tokens = tokenizeArguments(raw);
        if (tokens.isEmpty()) {
            return new LinkedHashMap<>();
        }

        boolean hasNamed = tokens.stream().anyMatch(token -> findAssignment(token) >= 0);
        return hasNamed
            ? parseNamedArguments(tokens, schemaProperties)
            : parsePositionalArguments(tokens, schemaProperties);
    }

    public static boolean validateRequiredParams(Map<String, Object> arguments, Tool toolSchema) {
        if (toolSchema == null) {
            return true;
        }

        JsonSchema schema = toolSchema.inputSchema();
        List<String> required = schema != null ? schema.required() : null;
        if (required == null || required.isEmpty()) {
            return true;
        }

        for (String name : required) {
            if (!arguments.containsKey(name)) {
                return false;
            }
        }
        return true;
    }

    public static String generateUsage(Tool toolSchema) {
        if (toolSchema == null) {
            return "<no arguments>";
        }

        JsonSchema schema = toolSchema.inputSchema();
        Map<String, Object> properties = schema != null ? schema.properties() : null;
        if (properties == null || properties.isEmpty()) {
            return "<no arguments>";
        }

        Set<String> required = new LinkedHashSet<>(schema != null && schema.required() != null
            ? schema.required()
            : Collections.emptyList());

        StringBuilder usage = new StringBuilder();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String name = entry.getKey();
            String type = extractType(entry.getValue());
            boolean requiredParam = required.contains(name);
            usage.append(requiredParam ? "<" : "[")
                .append(name);
            if (!type.isBlank()) {
                usage.append(":").append(type);
            }
            usage.append(requiredParam ? "> " : "] ");
        }
        return usage.toString().trim();
    }

    public static void displayToolResult(Command command, CallToolResult result) {
        if (command == null) {
            return;
        }

        if (result == null) {
            command.error("Tool returned no result.");
            return;
        }

        if (Boolean.TRUE.equals(result.isError())) {
            command.error("Tool Error: {}", extractErrorMessage(result));
            displayContentList(command, result.content());
            displayStructuredContent(command, result.structuredContent());
            return;
        }

        boolean hasContent = displayContentList(command, result.content());
        boolean hasStructured = displayStructuredContent(command, result.structuredContent());
        boolean hasMeta = displayMeta(command, result.meta());

        if (!hasContent && !hasStructured && !hasMeta) {
            command.info("Tool executed successfully (no output).");
        }
    }

    private static boolean displayContentList(Command command, List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return false;
        }

        for (Content content : contents) {
            displayContent(command, content);
        }
        return true;
    }

    private static void displayContent(Command command, Content content) {
        if (content == null) {
            return;
        }

        if (content instanceof McpSchema.TextContent textContent) {
            String text = Objects.toString(textContent.text(), "").trim();
            if (text.isEmpty()) {
                return;
            }

            for (String line : text.split("\\R")) {
                if (!line.isBlank()) {
                    command.info(line.trim());
                }
            }
            return;
        }

        if (content instanceof McpSchema.ImageContent image) {
            String mime = image.mimeType() != null ? image.mimeType() : "image";
            int length = image.data() != null ? image.data().length() : 0;
            command.info("[Image] {} ({} chars)", mime, length);
            return;
        }

        if (content instanceof McpSchema.AudioContent audio) {
            String mime = audio.mimeType() != null ? audio.mimeType() : "audio";
            command.info("[Audio] {}", mime);
            if (audio.data() != null) {
                command.info("Data length: {} chars", audio.data().length());
            }
            return;
        }

        command.info(content.toString());
    }

    private static boolean displayStructuredContent(Command command, Object structured) {
        if (structured == null) {
            return false;
        }

        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(structured);
            for (String line : json.split("\\R")) {
                command.info(line);
            }
            return true;
        } catch (JsonProcessingException e) {
            MeteorMCPAddon.LOG.warn("Failed to render structured content: {}", e.getMessage());
            command.info(structured.toString());
            return true;
        }
    }

    private static boolean displayMeta(Command command, Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return false;
        }

        command.info("Meta:");
        for (Map.Entry<String, Object> entry : meta.entrySet()) {
            command.info("  {}: {}", entry.getKey(), entry.getValue());
        }
        return true;
    }

    private static String extractErrorMessage(CallToolResult result) {
        List<Content> contents = result.content();
        if (contents != null) {
            for (Content content : contents) {
                if (content instanceof McpSchema.TextContent text && text.text() != null) {
                    return text.text();
                }
            }
        }
        Object structured = result.structuredContent();
        if (structured != null) {
            return structured.toString();
        }
        return "No error message provided.";
    }

    private static List<String> tokenizeArguments(String raw) {
        List<String> tokens = new ArrayList<>();
        if (raw.isEmpty()) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        boolean escape = false;
        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }

            if (inQuotes) {
                if (c == '\\') {
                    escape = true;
                }
                current.append(c);
                if (c == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }

            switch (c) {
                case '"', '\'' -> {
                    inQuotes = true;
                    quoteChar = c;
                    current.append(c);
                    continue;
                }
                case '\\' -> {
                    escape = true;
                    current.append(c);
                    continue;
                }
                case '{' -> braceDepth++;
                case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                case '(' -> parenDepth++;
                case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                default -> {
                }
            }

            if (Character.isWhitespace(c) && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static Map<String, Object> parseNamedArguments(List<String> tokens, Map<String, Object> properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String token : tokens) {
            int assignment = findAssignment(token);
            if (assignment < 0) {
                throw new IllegalArgumentException("Invalid named argument: " + token);
            }

            String key = token.substring(0, assignment).trim();
            String valuePart = token.substring(assignment + 1).trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Argument name is required near: " + token);
            }

            Object schema = properties != null ? properties.get(key) : null;
            Object value = coerceValue(valuePart, schema);
            result.put(key, value);
        }
        return result;
    }

    private static Map<String, Object> parsePositionalArguments(List<String> tokens, Map<String, Object> properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (properties == null || properties.isEmpty()) {
            for (int i = 0; i < tokens.size(); i++) {
                result.put("arg" + i, stripQuotes(tokens.get(i)));
            }
            return result;
        }

        int index = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (index >= tokens.size()) {
                break;
            }

            String token = tokens.get(index);
            Object schema = entry.getValue();
            Object value = coerceValue(token, schema);
            result.put(entry.getKey(), value);
            index++;
        }

        if (tokens.size() > properties.size()) {
            throw new IllegalArgumentException("Too many positional arguments (expected " + properties.size() + ").");
        }

        return result;
    }

    private static Object coerceValue(String valuePart, Object schema) {
        String raw = valuePart == null ? "" : valuePart.trim();
        if (raw.isEmpty()) {
            return "";
        }

        String type = extractType(schema);
        if (type.isBlank()) {
            return stripQuotes(raw);
        }

        try {
            return switch (type) {
                case "integer" -> parseInteger(raw);
                case "number" -> parseDecimal(raw);
                case "boolean" -> parseBoolean(raw);
                case "array" -> MAPPER.readValue(normalizeJsonValue(raw), List.class);
                case "object" -> MAPPER.readValue(normalizeJsonValue(raw), Map.class);
                default -> stripQuotes(raw);
            };
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON for " + type + " value: " + raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + type + " value: " + raw);
        }
    }

    private static Object parseInteger(String raw) {
        if (raw.startsWith("0x") || raw.startsWith("0X")) {
            return Long.parseLong(raw.substring(2), 16);
        }
        return Long.parseLong(stripQuotes(raw));
    }

    private static Object parseDecimal(String raw) {
        return Double.parseDouble(stripQuotes(raw));
    }

    private static Object parseBoolean(String raw) {
        String normalized = stripQuotes(raw).toLowerCase(Locale.ROOT);
        if (BOOLEAN_TRUE.contains(normalized)) {
            return Boolean.TRUE;
        }
        if (BOOLEAN_FALSE.contains(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean value: " + raw);
    }

    private static String normalizeJsonValue(String raw) {
        String trimmed = raw.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
            || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }

        // Treat quoted JSON literal
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
            || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = stripQuotes(trimmed);
        }
        return trimmed;
    }

    private static String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if (first == '"' && last == '"') {
            try {
                return MAPPER.readValue(value, String.class);
            } catch (JsonProcessingException e) {
                return value.substring(1, value.length() - 1);
            }
        }
        if (first == '\'' && last == '\'') {
            return value.substring(1, value.length() - 1).replace("\\'", "'");
        }
        return value;
    }

    private static boolean looksLikeJson(String raw) {
        if (raw == null) return false;
        String trimmed = raw.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
            || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private static Map<String, Object> normalizeJsonMap(Map<?, ?> map) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            if (key == null) {
                continue;
            }
            normalized.put(key.toString(), entry.getValue());
        }
        return normalized;
    }

    private static String extractType(Object schema) {
        if (schema instanceof Map<?, ?> schemaMap) {
            Object type = schemaMap.get("type");
            if (type != null) {
                return type.toString();
            }
        }
        return "";
    }

    private static int findAssignment(String token) {
        boolean inQuotes = false;
        char quoteChar = 0;
        int brace = 0;
        int bracket = 0;
        int paren = 0;
        boolean escape = false;

        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (inQuotes) {
                if (c == '\\') {
                    escape = true;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }

            switch (c) {
                case '"', '\'' -> {
                    inQuotes = true;
                    quoteChar = c;
                }
                case '{' -> brace++;
                case '}' -> brace = Math.max(0, brace - 1);
                case '[' -> bracket++;
                case ']' -> bracket = Math.max(0, bracket - 1);
                case '(' -> paren++;
                case ')' -> paren = Math.max(0, paren - 1);
                case '=' -> {
                    if (brace == 0 && bracket == 0 && paren == 0) {
                        return i;
                    }
                }
                default -> {
                }
            }
        }
        return -1;
    }
}
