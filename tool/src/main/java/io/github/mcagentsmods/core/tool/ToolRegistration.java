package io.github.mcagentsmods.core.tool;

/**
 * A tool's place in the registry, kept by whoever registered it.
 *
 * <p>Closing it removes the tool — the way to take tools away when what backs them goes, such
 * as a server that disconnects. Closing it twice, or after the tool was replaced by a new
 * registration under the same name, does nothing.
 */
public interface ToolRegistration extends AutoCloseable {

    /** The tool's name. */
    String name();

    /** Removes the tool, if this registration still holds its name. */
    @Override
    void close();
}
