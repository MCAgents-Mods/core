/**
 * The credential file every MCAgents mod shares.
 *
 * <p>{@link io.github.mcagentsmods.core.shared.store.MinecraftDirectory} finds the
 * Minecraft directory — which is not at a fixed path, differs by operating
 * system, and is relocated freely by launchers and modpacks — and
 * {@link io.github.mcagentsmods.core.shared.store.SharedTokenStore} reads and writes
 * {@code mcagents.json} inside it.</p>
 *
 * <p>The file belongs to core, not to any one mod. A player configures a token
 * once and every MCAgents mod finds it, because none of them keeps credentials
 * of its own. Several mods may therefore hold the file at once, which is why
 * writes go through a temporary sibling and are moved into place atomically.</p>
 *
 * <p>Nothing here may log, echo, or otherwise reveal a token.</p>
 */
package io.github.mcagentsmods.core.shared.store;
