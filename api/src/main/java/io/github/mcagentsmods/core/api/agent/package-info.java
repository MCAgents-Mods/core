/**
 * Asking core's agent: a mod hands an
 * {@link io.github.mcagentsmods.core.api.agent.AgentRequest} to an
 * {@link io.github.mcagentsmods.core.api.agent.Agent}, which answers it with the registered
 * tools the prompt needs and completes with an
 * {@link io.github.mcagentsmods.core.api.agent.AgentAnswer}.
 *
 * <p>Contracts only. {@code io.github.mcagentsmods.core.common.agent.Agents.agent} builds the
 * agent over the shared tool registry, in {@code core-common}.
 */
package io.github.mcagentsmods.core.api.agent;
