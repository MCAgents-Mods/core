/**
 * The agents menu and what drives it, on the client only: {@link
 * io.github.mcagentsmods.core.client.agent.AgentsKeys}, the G key;
 * {@link io.github.mcagentsmods.core.client.agent.AgentsMenu}, the hologram it shows, with one
 * button per mod and Settings; {@link io.github.mcagentsmods.core.client.agent.SettingsHologram},
 * the form Settings opens; {@link io.github.mcagentsmods.core.client.agent.AgentsRuntime},
 * which loads the player's settings at start-up; and
 * {@link io.github.mcagentsmods.core.client.agent.ClientThreadTools}, for a tool that must run on
 * the game's thread.
 *
 * <p>Formerly the agents mod's game code, folded into core. Each loader's client entry point
 * registers the key, calls {@code onEndTick} and starts the runtime.
 */
package io.github.mcagentsmods.core.client.agent;
