/**
 * The agent layer, without Minecraft: {@link io.github.mcagentsmods.core.common.agent.Agents},
 * where a mod finds the shared tool registry, the worker pool, the agents menu's buttons, the
 * player's settings and an agent to ask, and
 * {@link io.github.mcagentsmods.core.common.agent.ToolAgent}, which answers a prompt by picking
 * categories, then tools, then calling them through core's model client.
 *
 * <p>Formerly the agents mod ({@code mcagents_agents}), folded into core so one foundation mod
 * remains. The settings file keeps that mod's path, {@code MCAgents/agents/settings.properties}.
 */
package io.github.mcagentsmods.core.common.agent;
