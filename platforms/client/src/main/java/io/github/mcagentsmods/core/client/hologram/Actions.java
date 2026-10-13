package io.github.mcagentsmods.core.client.hologram;

/** Runs a mod's callback for a panel, so one that throws is logged rather than thrown into the game. */
@FunctionalInterface
interface Actions {

    /**
     * Runs {@code action}.
     *
     * @param what   what the action is, for the log
     * @param action the mod's callback
     * @return whether it finished without throwing
     */
    boolean run(String what, Runnable action);
}
