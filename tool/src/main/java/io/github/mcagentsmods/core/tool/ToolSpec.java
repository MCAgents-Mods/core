package io.github.mcagentsmods.core.tool;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Everything about a tool except what it does: its name, what it is for, its arguments, the
 * categories it belongs to, and how it runs.
 *
 * <pre>{@code
 * ToolSpec spec = ToolSpec.builder("count_items")
 *         .description("Counts an item in the player's inventory")
 *         .category("minecraft")
 *         .category("inventory")
 *         .parameters(ToolSchema.builder()
 *                 .string("item", "The item id, such as minecraft:oak_log", true)
 *                 .build())
 *         .mode(ExecutionMode.CONCURRENT)
 *         .build();
 * }</pre>
 *
 * @param name        unique across every registered tool; 1 to 64 letters, digits, dashes or
 *                    underscores, because that is what a model provider accepts
 * @param description what it does, which is all the model has to decide whether to call it
 * @param parameters  the schema of its arguments
 * @param categories  one or more category ids, in the order given
 * @param mode        whether it may run beside other tools
 * @param timeout     how long one call may take before it is answered with an error and its
 *                    worker slot is freed
 */
public record ToolSpec(
        String name,
        String description,
        ToolSchema parameters,
        Set<String> categories,
        ExecutionMode mode,
        Duration timeout) {

    /** How long a call may take when a tool does not say. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    public ToolSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(categories, "categories");
        ToolNames.requireValid(name, "tool");
        description = description == null ? "" : description.strip();
        if (description.isEmpty()) {
            throw new ToolRegistrationException("The tool '" + name + "' needs a description: "
                    + "it is all the model reads when it picks tools");
        }
        parameters = parameters == null ? ToolSchema.empty() : parameters;
        if (categories.isEmpty()) {
            throw new ToolRegistrationException("The tool '" + name + "' needs at least one category");
        }
        for (String category : categories) {
            ToolNames.requireValid(category, "category");
        }
        categories = Collections.unmodifiableSet(new LinkedHashSet<>(categories));
        mode = mode == null ? ExecutionMode.WAIT : mode;
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ToolRegistrationException("The tool '" + name + "' needs a timeout above zero");
        }
    }

    /** Whether the tool belongs to {@code category}. */
    public boolean isIn(String category) {
        return categories.contains(category);
    }

    /** Starts a spec for the tool called {@code name}. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Builds a {@link ToolSpec}. Only the name, a description and one category are required. */
    public static final class Builder {

        private final String name;
        private final Set<String> categories = new LinkedHashSet<>();
        private String description;
        private ToolSchema parameters = ToolSchema.empty();
        private ExecutionMode mode = ExecutionMode.WAIT;
        private Duration timeout = DEFAULT_TIMEOUT;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** Adds a category. A tool may belong to as many as it fits. */
        public Builder category(String category) {
            categories.add(Objects.requireNonNull(category, "category"));
            return this;
        }

        /** Adds every category in {@code ids}. */
        public Builder categories(Collection<String> ids) {
            ids.forEach(this::category);
            return this;
        }

        public Builder parameters(ToolSchema parameters) {
            this.parameters = Objects.requireNonNull(parameters, "parameters");
            return this;
        }

        /** {@link ExecutionMode#WAIT} unless set. */
        public Builder mode(ExecutionMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /** {@link #DEFAULT_TIMEOUT} unless set. */
        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        public ToolSpec build() {
            return new ToolSpec(name, description, parameters, categories, mode, timeout);
        }
    }
}
