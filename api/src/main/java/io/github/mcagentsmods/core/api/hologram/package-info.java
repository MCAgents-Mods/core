/**
 * Holograms: panels that stand in the world in front of the player, who aims at them and
 * clicks instead of opening a screen.
 *
 * <p>A mod describes what to show with a {@link io.github.mcagentsmods.core.api.hologram.Hologram}
 * made of {@link io.github.mcagentsmods.core.api.hologram.HologramPanel}s, and opens it with
 * {@code io.github.mcagentsmods.core.client.hologram.Holograms}, in {@code mcagents-core-client}.
 * Core draws it, places it, moves it while the player holds the move key, and removes it once
 * the player is more than two chunks away. Everything here is plain data; nothing in this
 * package names a Minecraft type.
 */
package io.github.mcagentsmods.core.api.hologram;
