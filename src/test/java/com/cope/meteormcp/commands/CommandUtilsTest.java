package com.cope.meteormcp.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommandUtilsTest {
    @Test
    void parsesPositionalArguments() {
        Tool tool = createWeatherTool();
        Map<String, Object> args = CommandUtils.parseArguments("\"London\" 3", tool);

        assertEquals("London", args.get("location"));
        assertEquals(3L, args.get("days"));
    }

    @Test
    void parsesNamedArguments() {
        Tool tool = createWeatherTool();
        Map<String, Object> args = CommandUtils.parseArguments("location=\"Paris\" days=5", tool);

        assertEquals("Paris", args.get("location"));
        assertEquals(5L, args.get("days"));
    }

    @Test
    void parsesJsonLiteral() {
        Tool tool = createWeatherTool();
        Map<String, Object> args = CommandUtils.parseArguments("{\"location\": \"Berlin\", \"days\": 2}", tool);

        assertEquals("Berlin", args.get("location"));
        assertEquals(2, ((Number) args.get("days")).intValue());
    }

    @Test
    void validatesRequiredParameters() {
        Tool tool = createWeatherTool();
        Map<String, Object> args = Map.of("location", "Rome", "days", 1);

        assertTrue(CommandUtils.validateRequiredParams(args, tool));

        Map<String, Object> missing = Map.of("days", 1);
        assertFalse(CommandUtils.validateRequiredParams(missing, tool));
    }

    @Test
    void generatesUsageString() {
        Tool tool = createWeatherTool();
        assertEquals("<location:string> [days:integer]", CommandUtils.generateUsage(tool));
    }

    private static Tool createWeatherTool() {
        return new Tool(
            "get_forecast",
            "Get Forecast",
            "Fetches weather forecast.",
            createWeatherSchema(),
            null,
            null,
            null
        );
    }

    private static JsonSchema createWeatherSchema() {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("location", Map.of("type", "string"));
        properties.put("days", Map.of("type", "integer"));

        return new JsonSchema(
            "object",
            properties,
            List.of("location"),
            null,
            null,
            null
        );
    }
}
