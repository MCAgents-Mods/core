package io.github.mcagentsmods.core.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolSchemaTest {

    @Test
    void buildsAnObjectSchemaInTheOrderGiven() {
        ToolSchema schema = ToolSchema.builder()
                .string("item", "The item id", true)
                .integer("count", null, false)
                .choice("side", "Which hand", true, List.of("main", "off"))
                .strings("tags", "Tags \"quoted\"", false)
                .build();

        assertEquals("{\"type\":\"object\",\"properties\":{"
                + "\"item\":{\"type\":\"string\",\"description\":\"The item id\"},"
                + "\"count\":{\"type\":\"integer\"},"
                + "\"side\":{\"type\":\"string\",\"enum\":[\"main\",\"off\"],\"description\":\"Which hand\"},"
                + "\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"Tags \\\"quoted\\\"\"}},"
                + "\"required\":[\"item\",\"side\"]}", schema.json());
    }

    @Test
    void offersAListOfChoices() {
        ToolSchema schema = ToolSchema.builder()
                .choices("picked", "What to use", true, List.of("a", "b"))
                .build();

        assertEquals("{\"type\":\"object\",\"properties\":{"
                + "\"picked\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"enum\":[\"a\",\"b\"]},"
                + "\"description\":\"What to use\"}},\"required\":[\"picked\"]}", schema.json());
        assertThrows(ToolRegistrationException.class,
                () -> ToolSchema.builder().choices("none", null, true, List.of()));
    }

    @Test
    void nestsASchemaOfItsOwn() {
        ToolSchema position = ToolSchema.builder().integer("x", null, true).build();
        ToolSchema schema = ToolSchema.builder().property("at", position, true).build();

        Object parsed = Json.parse(schema.json());
        Map<?, ?> at = (Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) parsed).get("properties")).get("at");
        assertEquals("object", at.get("type"));
        assertEquals(List.of("x"), at.get("required"));
    }

    @Test
    void refusesASchemaThatIsNotAnObject() {
        assertThrows(ToolRegistrationException.class, () -> ToolSchema.of("[1]"));
        assertThrows(ToolRegistrationException.class, () -> ToolSchema.of("{\"type\":"));
        assertThrows(ToolRegistrationException.class,
                () -> ToolSchema.builder().string("a", null, true).string("a", null, true));
    }

    @Test
    void aBlankSchemaTakesNoArguments() {
        assertEquals(ToolSchema.empty(), ToolSchema.of(" "));
        assertEquals(ToolSchema.empty(), ToolSpec.builder("t").description("d").category("c").build().parameters());
    }
}
