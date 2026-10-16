package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.button.AgentButton;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The buttons on the agents menu, which mods add to.
 *
 * <p>Held by id in a {@link ConcurrentHashMap}, so a mod may add one from any thread at any
 * time; the menu reads the list each time it opens. Two buttons cannot share an id.
 */
public final class AgentButtons {

    private static final Comparator<AgentButton> ORDER =
            Comparator.comparingInt(AgentButton::order).thenComparing(AgentButton::id);

    private final Map<String, AgentButton> buttons = new ConcurrentHashMap<>();

    /**
     * Adds {@code button} to the menu.
     *
     * @return closing it takes the button away again
     * @throws IllegalArgumentException if a button with the same id is already there
     */
    public AutoCloseable register(AgentButton button) {
        AgentButton existing = buttons.putIfAbsent(button.id(), button);
        if (existing != null) {
            throw new IllegalArgumentException("A button with the id '" + button.id()
                    + "' is already on the agents menu");
        }
        return () -> buttons.remove(button.id(), button);
    }

    /** Every button, in the order the menu shows them. */
    public List<AgentButton> list() {
        return buttons.values().stream().sorted(ORDER).toList();
    }

    public boolean isEmpty() {
        return buttons.isEmpty();
    }
}
