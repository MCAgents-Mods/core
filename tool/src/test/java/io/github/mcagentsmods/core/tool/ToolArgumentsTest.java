package io.github.mcagentsmods.core.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolArgumentsTest {

    @Test
    void readsEveryKindOfValue() {
        ToolArguments arguments = ToolArguments.parse("""
                {"item": "minecraft:oak_log", "count": 3, "ratio": 0.5, "exact": true,
                 "tags": ["a", "b"], "where": {"x": -1}, "nothing": null,
                 "text": "line\\nbreak \\u00e9 \\"quoted\\""}
                """);

        assertEquals("minecraft:oak_log", arguments.string("item"));
        assertEquals(3, arguments.integer("count"));
        assertEquals(0.5, arguments.number("ratio"));
        assertEquals(3.0, arguments.number("count"));
        assertTrue(arguments.bool("exact"));
        assertEquals(List.of("a", "b"), arguments.strings("tags"));
        assertEquals(Map.of("x", -1L), arguments.object("where"));
        assertEquals("line\nbreak é \"quoted\"", arguments.string("text"));
        assertFalse(arguments.has("nothing"));
        assertFalse(arguments.has("absent"));
    }

    @Test
    void fallsBackWhenAnOptionalArgumentIsMissing() {
        ToolArguments arguments = ToolArguments.parse("{}");

        assertEquals("UTC", arguments.string("zone", "UTC"));
        assertEquals(10, arguments.integer("limit", 10));
        assertFalse(arguments.bool("exact", false));
    }

    @Test
    void acceptsAWholeNumberWrittenAsADecimal() {
        assertEquals(4, ToolArguments.parse("{\"n\": 4.0}").integer("n"));
        assertThrows(ToolArgumentException.class, () -> ToolArguments.parse("{\"n\": 4.5}").integer("n"));
    }

    @Test
    void namesAMissingOrMistypedArgument() {
        ToolArguments arguments = ToolArguments.parse("{\"count\": \"three\"}");

        ToolArgumentException missing = assertThrows(ToolArgumentException.class, () -> arguments.string("item"));
        assertEquals("The argument 'item' is required", missing.getMessage());
        ToolArgumentException mistyped = assertThrows(ToolArgumentException.class, () -> arguments.integer("count"));
        assertEquals("The argument 'count' must be a whole number", mistyped.getMessage());
    }

    @Test
    void refusesArgumentsThatAreNotAJsonObject() {
        assertThrows(ToolArgumentException.class, () -> ToolArguments.parse("{\"open\": "));
        assertThrows(ToolArgumentException.class, () -> ToolArguments.parse("[1, 2]"));
        assertThrows(ToolArgumentException.class, () -> ToolArguments.parse("{} trailing"));
        assertThrows(ToolArgumentException.class, () -> ToolArguments.parse("{'single': 1}"));
    }

    @Test
    void blankArgumentsAreNone() {
        assertTrue(ToolArguments.parse("  ").asMap().isEmpty());
        assertTrue(ToolArguments.parse(null).asMap().isEmpty());
    }

    @Test
    void refusesValuesNestedTooDeeply() {
        String deep = "{\"a\":".repeat(100) + "1" + "}".repeat(100);
        assertThrows(ToolArgumentException.class, () -> ToolArguments.parse(deep));
    }
}
