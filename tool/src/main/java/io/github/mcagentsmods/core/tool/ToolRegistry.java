package io.github.mcagentsmods.core.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every tool the agent may use, held in memory and looked up by name.
 *
 * <p><b>Lookup is one hash.</b> Tools live in a {@link ConcurrentHashMap} keyed by name, so
 * turning a model's call into the code it runs is a single {@code get}, from any thread, while
 * other threads register and remove tools. Nothing compares the name against each tool in turn.
 *
 * <p><b>Names are checked when a tool arrives.</b> {@link #register} refuses a name another tool
 * holds, a name a model provider would not accept, and a tool with no description or no
 * category, by throwing {@link ToolRegistrationException} in the mod that registered it.
 *
 * <p><b>Categories group tools.</b> A tool belongs to one or more. A category a tool names is
 * created on the spot if nobody declared it; {@link #declareCategory} gives it the description
 * the agent shows the model, whichever comes first.
 *
 * <p>Every mod registers into the same registry, {@link #shared()}, which is the one core's agent
 * reads. A separate instance is for tests and for code using this module on its own.
 */
public final class ToolRegistry implements ToolLookup {

    private static final ToolRegistry SHARED = new ToolRegistry();

    private static final Comparator<ToolSpec> BY_NAME = Comparator.comparing(ToolSpec::name);

    private final Map<String, RegisteredTool> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolCategory> categories = new ConcurrentHashMap<>();

    /** Serializes changes, so a tool and its categories arrive together. Reads never wait on it. */
    private final Object writes = new Object();

    /** The registry every mod registers into, and core's agent reads. */
    public static ToolRegistry shared() {
        return SHARED;
    }

    /**
     * Declares a category with what it is for.
     *
     * <p>Safe to call from every mod that uses the category: the first non-blank description
     * stays, so two mods sharing a category such as {@code minecraft} cannot keep overwriting
     * each other.
     *
     * @return the category as it now stands
     */
    public ToolCategory declareCategory(String id, String description) {
        ToolCategory declared = new ToolCategory(id, description);
        synchronized (writes) {
            return categories.merge(id, declared,
                    (existing, offered) -> existing.isDescribed() ? existing : offered);
        }
    }

    /**
     * Registers a tool whose work is done when {@code body} returns.
     *
     * @throws ToolRegistrationException if another tool already has the name
     */
    public ToolRegistration register(ToolSpec spec, Tool body) {
        return add(RegisteredTool.of(spec, body));
    }

    /**
     * Registers a tool whose work finishes later, when the stage {@code body} returns
     * completes.
     *
     * @throws ToolRegistrationException if another tool already has the name
     */
    public ToolRegistration registerAsync(ToolSpec spec, AsyncTool body) {
        return add(RegisteredTool.ofAsync(spec, body));
    }

    private ToolRegistration add(RegisteredTool tool) {
        String name = tool.name();
        synchronized (writes) {
            RegisteredTool existing = tools.putIfAbsent(name, tool);
            if (existing != null) {
                throw new ToolRegistrationException("A tool named '" + name + "' is already "
                        + "registered. Tool names are shared by every mod, so prefix yours, "
                        + "such as mymod_" + name);
            }
            for (String category : tool.spec().categories()) {
                categories.putIfAbsent(category, new ToolCategory(category, ""));
            }
        }
        return new Handle(this, tool);
    }

    /**
     * Removes the tool called {@code name}.
     *
     * @return whether there was one
     */
    public boolean unregister(String name) {
        synchronized (writes) {
            return tools.remove(name) != null;
        }
    }

    private void unregister(RegisteredTool tool) {
        synchronized (writes) {
            tools.remove(tool.name(), tool);
        }
    }

    /** The tool called {@code name}: one hash lookup. */
    @Override
    public Optional<RegisteredTool> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(tools.get(name));
    }

    public boolean contains(String name) {
        return name != null && tools.containsKey(name);
    }

    /** How many tools are registered. */
    public int size() {
        return tools.size();
    }

    public boolean isEmpty() {
        return tools.isEmpty();
    }

    /** The category called {@code id}, whether or not any tool is in it now. */
    public Optional<ToolCategory> category(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(categories.get(id));
    }

    /** Every category with at least one tool in it, by id. */
    public List<ToolCategory> categories() {
        return snapshot().categories();
    }

    /** Every tool, by name. */
    public List<ToolSpec> tools() {
        return snapshot().tools();
    }

    /** The tools in any of {@code categoryIds}, each once, by name. */
    public List<ToolSpec> toolsIn(Collection<String> categoryIds) {
        return snapshot().toolsIn(categoryIds);
    }

    /**
     * The registry as it stands now, unchanged by later registrations.
     *
     * <p>The agent takes one per question and uses it throughout: the tools the model was
     * shown must be the tools its calls run against, or a tool removed mid-question would make
     * the model look as if it had invented it.
     */
    public Snapshot snapshot() {
        synchronized (writes) {
            return new Snapshot(Map.copyOf(tools), Map.copyOf(categories));
        }
    }

    /** A frozen view of the registry. Lookups are still one hash. */
    public static final class Snapshot implements ToolLookup {

        private final Map<String, RegisteredTool> tools;
        private final Map<String, ToolCategory> categories;

        private Snapshot(Map<String, RegisteredTool> tools, Map<String, ToolCategory> categories) {
            this.tools = tools;
            this.categories = categories;
        }

        @Override
        public Optional<RegisteredTool> find(String name) {
            return name == null ? Optional.empty() : Optional.ofNullable(tools.get(name));
        }

        public boolean isEmpty() {
            return tools.isEmpty();
        }

        public int size() {
            return tools.size();
        }

        /** Every category with at least one tool in it, by id. */
        public List<ToolCategory> categories() {
            Map<String, ToolCategory> used = new LinkedHashMap<>();
            for (RegisteredTool tool : tools.values()) {
                for (String id : tool.spec().categories()) {
                    used.computeIfAbsent(id, key -> categories.getOrDefault(key, new ToolCategory(key, "")));
                }
            }
            List<ToolCategory> sorted = new ArrayList<>(used.values());
            sorted.sort(Comparator.comparing(ToolCategory::id));
            return List.copyOf(sorted);
        }

        /** Every tool, by name. */
        public List<ToolSpec> tools() {
            return tools.values().stream().map(RegisteredTool::spec).sorted(BY_NAME).toList();
        }

        /** The tools in any of {@code categoryIds}, each once, by name. */
        public List<ToolSpec> toolsIn(Collection<String> categoryIds) {
            Objects.requireNonNull(categoryIds, "categoryIds");
            return tools.values().stream()
                    .map(RegisteredTool::spec)
                    .filter(spec -> categoryIds.stream().anyMatch(spec::isIn))
                    .sorted(BY_NAME)
                    .toList();
        }

        /** The specs of the tools called {@code names}, in that order, skipping any it does not hold. */
        public List<ToolSpec> specs(Collection<String> names) {
            return names.stream()
                    .map(tools::get)
                    .filter(Objects::nonNull)
                    .map(RegisteredTool::spec)
                    .toList();
        }
    }

    private record Handle(ToolRegistry registry, RegisteredTool tool) implements ToolRegistration {

        @Override
        public String name() {
            return tool.name();
        }

        @Override
        public void close() {
            registry.unregister(tool);
        }
    }
}
