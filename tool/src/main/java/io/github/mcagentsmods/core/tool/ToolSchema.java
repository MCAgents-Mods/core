package io.github.mcagentsmods.core.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The JSON Schema of a tool's arguments: what the model must send when it calls the tool.
 *
 * <p>Build one with {@link #builder()}, which covers the shapes a tool usually takes, or hand
 * over a schema written elsewhere — an MCP server's, say — with {@link #of(String)}. Either way
 * it is checked to be a JSON object when it is made, not when a model first sees it.
 *
 * <pre>{@code
 * ToolSchema schema = ToolSchema.builder()
 *         .string("item", "The item id, such as minecraft:oak_log", true)
 *         .integer("count", "How many to look for", false)
 *         .build();
 * }</pre>
 *
 * @param json the schema, a JSON object
 */
public record ToolSchema(String json) {

    private static final ToolSchema EMPTY = new ToolSchema("{\"type\":\"object\",\"properties\":{}}");

    public ToolSchema {
        Objects.requireNonNull(json, "json");
        Object parsed;
        try {
            parsed = Json.parse(json);
        } catch (IllegalArgumentException e) {
            throw new ToolRegistrationException("A tool's parameter schema is not valid JSON: "
                    + e.getMessage());
        }
        if (!(parsed instanceof Map<?, ?>)) {
            throw new ToolRegistrationException("A tool's parameter schema must be a JSON object");
        }
    }

    /** A schema for a tool that takes no arguments. */
    public static ToolSchema empty() {
        return EMPTY;
    }

    /** A schema written elsewhere, as JSON text. */
    public static ToolSchema of(String json) {
        return json == null || json.isBlank() ? EMPTY : new ToolSchema(json.strip());
    }

    /** Starts a schema for an object with named arguments. */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return json;
    }

    /** Builds an object schema one argument at a time, in the order they are added. */
    public static final class Builder {

        private final Map<String, String> properties = new LinkedHashMap<>();
        private final List<String> required = new ArrayList<>();

        private Builder() {
        }

        /** A text argument. */
        public Builder string(String name, String description, boolean required) {
            return add(name, "\"type\":\"string\"", description, required);
        }

        /** A text argument that must be one of {@code allowed}. */
        public Builder choice(String name, String description, boolean required, List<String> allowed) {
            return add(name, enumOf(name, allowed), description, required);
        }

        /** A list of text values, each one of {@code allowed}. */
        public Builder choices(String name, String description, boolean required, List<String> allowed) {
            String items = "\"type\":\"array\",\"items\":{" + enumOf(name, allowed) + "}";
            return add(name, items, description, required);
        }

        /** A whole-number argument. */
        public Builder integer(String name, String description, boolean required) {
            return add(name, "\"type\":\"integer\"", description, required);
        }

        /** A numeric argument. */
        public Builder number(String name, String description, boolean required) {
            return add(name, "\"type\":\"number\"", description, required);
        }

        /** A true or false argument. */
        public Builder bool(String name, String description, boolean required) {
            return add(name, "\"type\":\"boolean\"", description, required);
        }

        /** A list of text values. */
        public Builder strings(String name, String description, boolean required) {
            return add(name, "\"type\":\"array\",\"items\":{\"type\":\"string\"}", description, required);
        }

        /**
         * An argument described by a schema of its own, for a shape the methods above do not
         * cover.
         *
         * @param schema the argument's schema, a JSON object
         */
        public Builder property(String name, ToolSchema schema, boolean required) {
            Objects.requireNonNull(schema, "schema");
            String body = schema.json().strip();
            // The object's members without their braces, so they sit beside nothing else.
            return add(name, body.substring(1, body.length() - 1).strip(), null, required);
        }

        /** The schema, as it will be offered to the model. */
        public ToolSchema build() {
            StringBuilder out = new StringBuilder("{\"type\":\"object\",\"properties\":{");
            boolean first = true;
            for (Map.Entry<String, String> property : properties.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(Json.quote(property.getKey()))
                        .append(":{").append(property.getValue()).append('}');
            }
            out.append('}');
            if (!required.isEmpty()) {
                out.append(",\"required\":[");
                for (int i = 0; i < required.size(); i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    out.append(Json.quote(required.get(i)));
                }
                out.append(']');
            }
            return new ToolSchema(out.append('}').toString());
        }

        private static String enumOf(String name, List<String> allowed) {
            Objects.requireNonNull(allowed, "allowed");
            if (allowed.isEmpty()) {
                throw new ToolRegistrationException("The argument '" + name + "' offers no choices");
            }
            StringBuilder values = new StringBuilder("\"type\":\"string\",\"enum\":[");
            for (int i = 0; i < allowed.size(); i++) {
                if (i > 0) {
                    values.append(',');
                }
                values.append(Json.quote(Objects.requireNonNull(allowed.get(i), "allowed value")));
            }
            return values.append(']').toString();
        }

        private Builder add(String name, String type, String description, boolean isRequired) {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new ToolRegistrationException("An argument needs a name");
            }
            if (properties.containsKey(name)) {
                throw new ToolRegistrationException("The argument '" + name + "' is declared twice");
            }
            String body = type;
            if (description != null && !description.isBlank()) {
                body = body + (body.isEmpty() ? "" : ",")
                        + "\"description\":" + Json.quote(description.strip());
            }
            properties.put(name, body);
            if (isRequired) {
                required.add(name);
            }
            return this;
        }
    }
}
