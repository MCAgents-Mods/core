package io.github.mcagentsmods.core.tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The arguments the model sent with a call, read from its JSON.
 *
 * <p>A tool reads what it needs by name. Asking for an argument that is missing, or that has
 * the wrong type, throws {@link ToolArgumentException}, which the executor turns into an error
 * result the model reads and can correct: a model that sent the wrong arguments is asked again,
 * not left with a failed question.
 */
public final class ToolArguments {

    private static final ToolArguments NONE = new ToolArguments("{}", Map.of());

    private final String json;
    private final Map<String, Object> values;

    private ToolArguments(String json, Map<String, Object> values) {
        this.json = json;
        this.values = values;
    }

    /**
     * Reads the arguments of one call.
     *
     * @param json the arguments as the model sent them; blank means none
     * @throws ToolArgumentException if they are not a JSON object
     */
    public static ToolArguments parse(String json) {
        if (json == null || json.isBlank()) {
            return NONE;
        }
        Object parsed;
        try {
            parsed = Json.parse(json);
        } catch (IllegalArgumentException e) {
            throw new ToolArgumentException("The arguments are not valid JSON. " + e.getMessage());
        }
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new ToolArgumentException("The arguments must be a JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> members = (Map<String, Object>) map;
        return new ToolArguments(json, members);
    }

    /** No arguments at all. */
    public static ToolArguments none() {
        return NONE;
    }

    /** The arguments exactly as the model sent them. */
    public String json() {
        return json;
    }

    /** Every argument, by name. Objects are maps, arrays are lists. */
    public Map<String, Object> asMap() {
        return values;
    }

    /** Whether the model sent {@code name} with a value other than null. */
    public boolean has(String name) {
        return values.get(name) != null;
    }

    public String string(String name) {
        return require(name, String.class, "text");
    }

    public String string(String name, String fallback) {
        return has(name) ? string(name) : fallback;
    }

    public long integer(String name) {
        Object value = values.get(name);
        if (value instanceof Long whole) {
            return whole;
        }
        if (value instanceof Double real && real == Math.rint(real) && !real.isInfinite()) {
            return real.longValue();
        }
        throw wrongType(name, value, "a whole number");
    }

    public long integer(String name, long fallback) {
        return has(name) ? integer(name) : fallback;
    }

    public double number(String name) {
        Object value = values.get(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw wrongType(name, value, "a number");
    }

    public double number(String name, double fallback) {
        return has(name) ? number(name) : fallback;
    }

    public boolean bool(String name) {
        return require(name, Boolean.class, "true or false");
    }

    public boolean bool(String name, boolean fallback) {
        return has(name) ? bool(name) : fallback;
    }

    /** A list of text values; any other item is refused. */
    public List<String> strings(String name) {
        List<?> items = require(name, List.class, "a list");
        for (Object item : items) {
            if (!(item instanceof String)) {
                throw new ToolArgumentException("Every item of '" + name + "' must be text");
            }
        }
        @SuppressWarnings("unchecked")
        List<String> strings = (List<String>) items;
        return strings;
    }

    /** A list of any values. */
    public List<Object> list(String name) {
        @SuppressWarnings("unchecked")
        List<Object> items = require(name, List.class, "a list");
        return items;
    }

    /** A nested object, as a map. */
    public Map<String, Object> object(String name) {
        @SuppressWarnings("unchecked")
        Map<String, Object> members = require(name, Map.class, "an object");
        return members;
    }

    private <T> T require(String name, Class<T> type, String expected) {
        Objects.requireNonNull(name, "name");
        Object value = values.get(name);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw wrongType(name, value, expected);
    }

    private static ToolArgumentException wrongType(String name, Object value, String expected) {
        if (value == null) {
            return new ToolArgumentException("The argument '" + name + "' is required");
        }
        return new ToolArgumentException("The argument '" + name + "' must be " + expected);
    }

    @Override
    public String toString() {
        return json;
    }
}
