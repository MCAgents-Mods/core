package io.github.mcagentsmods.core.tool;

/**
 * Whether a tool may run at the same time as other tools.
 *
 * <p>When the model asks for several tools in one turn, {@link ToolExecutor} runs every
 * {@link #CONCURRENT} call in a row together, and runs each {@link #WAIT} call alone: after
 * everything asked before it has finished, and before anything asked after it starts. The
 * results always come back in the order the model asked for them.
 */
public enum ExecutionMode {

    /**
     * Work that has to wait its turn. For a tool that changes something — the world, a file,
     * the player's inventory — where running beside another call, or in another order, would
     * give a different result. The default, because it is never wrong, only slower.
     */
    WAIT,

    /**
     * Work that can run at the same time as other work. For a tool that only reads, or whose
     * calls cannot affect each other. Runs on the worker pool beside its neighbours, up to the
     * pool's limit.
     */
    CONCURRENT
}
