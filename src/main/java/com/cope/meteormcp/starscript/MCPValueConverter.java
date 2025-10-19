package com.cope.meteormcp.starscript;

import com.cope.meteormcp.MeteorMCPAddon;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;

import java.util.*;

/**
 * Converts between StarScript Values and MCP types (JSON).
 * Handles bidirectional conversion for tool arguments and results.
 *
 * @author GhostTypes
 */
public class MCPValueConverter {

    /**
     * Convert a StarScript runtime value into a JSON-compatible structure suitable for
     * MCP tool invocation.
     *
     * @param value StarScript value retrieved from the stack
     * @return representation that can be serialized by the MCP client
     */
    public static Object toJson(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isBool()) {
            return value.getBool();
        }

        if (value.isNumber()) {
            double num = value.getNumber();
            // Return integer if whole number
            if (num == Math.floor(num)) {
                return (long) num;
            }
            return num;
        }

        if (value.isString()) {
            return value.getString();
        }

        if (value.isMap()) {
            Map<String, Object> map = new HashMap<>();
            ValueMap valueMap = value.getMap();
            for (String key : valueMap.keys()) {
                var supplier = valueMap.get(key);
                if (supplier != null) {
                    Value val = supplier.get();
                    if (val != null) {
                        map.put(key, toJson(val));
                    }
                }
            }
            return map;
        }

        // Fallback to string representation
        return value.toString();
    }

    /**
     * Convert an MCP tool result into a StarScript value.
     *
     * @param result structured MCP response
     * @return StarScript value for display or further computation
     */
    public static Value toValue(CallToolResult result) {
        if (result == null) {
            return Value.null_();
        }

        try {
            List<Content> contentList = result.content();
            if (contentList == null || contentList.isEmpty()) {
                return Value.null_();
            }

            // If single content item, return it directly
            if (contentList.size() == 1) {
                return contentToValue(contentList.get(0));
            }

            // Multiple content items - concatenate text or return first
            StringBuilder combined = new StringBuilder();
            for (Content content : contentList) {
                Value val = contentToValue(content);
                if (val.isString()) {
                    if (combined.length() > 0) {
                        combined.append("\n");
                    }
                    combined.append(val.getString());
                }
            }

            if (combined.length() > 0) {
                return Value.string(combined.toString());
            }

            // Fallback to first content
            return contentToValue(contentList.get(0));

        } catch (Exception e) {
            MeteorMCPAddon.LOG.error("Error converting MCP result to StarScript value: {}", e.getMessage());
            return Value.null_();
        }
    }

    /**
     * Convert a single content item within a tool result into a StarScript value.
     *
     * @param content tool content payload
     * @return StarScript-friendly wrapper
     */
    private static Value contentToValue(Content content) {
        if (content instanceof TextContent) {
            TextContent text = (TextContent) content;
            return Value.string(text.text());
        }

        if (content instanceof ImageContent) {
            ImageContent image = (ImageContent) content;
            // Return the data (URL or base64)
            return Value.string(image.data());
        }

        // Unknown content type - try toString
        return Value.string(content.toString());
    }

    /**
     * Convert a JSON-like map into a lazily-evaluated StarScript {@link ValueMap}.
     *
     * @param map structured content returned by an MCP tool
     * @return value map that can be injected back into StarScript
     */
    public static ValueMap jsonToValueMap(Map<String, Object> map) {
        ValueMap valueMap = new ValueMap();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            valueMap.set(entry.getKey(), objectToValue(entry.getValue()));
        }

        return valueMap;
    }

    /**
     * Convert a generic Java object into a StarScript value.
     *
     * @param obj source object (String, Number, Map, etc.)
     * @return StarScript representation
     */
    private static Value objectToValue(Object obj) {
        if (obj == null) {
            return Value.null_();
        }

        if (obj instanceof Boolean) {
            return Value.bool((Boolean) obj);
        }

        if (obj instanceof Number) {
            return Value.number(((Number) obj).doubleValue());
        }

        if (obj instanceof String) {
            return Value.string((String) obj);
        }

        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return Value.map(jsonToValueMap(map));
        }

        if (obj instanceof List) {
            // StarScript doesn't have native arrays, convert to comma-separated string
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(list.get(i).toString());
            }
            return Value.string(sb.toString());
        }

        // Fallback to string
        return Value.string(obj.toString());
    }

    /**
     * Extract positional arguments from the StarScript stack. Values are popped in LIFO
     * order and converted to JSON-friendly structures.
     *
     * @param ss       StarScript runtime
     * @param argCount number of arguments present on the stack
     * @return ordered list of arguments
     */
    public static List<Object> extractArgumentsAsList(org.meteordev.starscript.Starscript ss, int argCount) {
        List<Object> args = new ArrayList<>();

        // Pop in reverse order
        for (int i = argCount - 1; i >= 0; i--) {
            Value val = ss.pop();
            args.add(0, toJson(val)); // Add at beginning to preserve order
        }

        return args;
    }

    /**
     * Extract named arguments from the StarScript stack, pairing values with the provided
     * parameter names.
     *
     * @param ss         StarScript runtime
     * @param argCount   number of arguments present on the stack
     * @param paramNames ordered parameter names derived from the MCP schema
     * @return map of argument names to JSON-friendly values
     */
    public static Map<String, Object> extractArgumentsAsMap(
        org.meteordev.starscript.Starscript ss,
        int argCount,
        List<String> paramNames
    ) {
        Map<String, Object> args = new HashMap<>();

        // Pop in reverse order
        for (int i = argCount - 1; i >= 0; i--) {
            Value val = ss.pop();
            String paramName = i < paramNames.size() ? paramNames.get(i) : "arg" + i;
            args.put(paramName, toJson(val));
        }

        return args;
    }
}
