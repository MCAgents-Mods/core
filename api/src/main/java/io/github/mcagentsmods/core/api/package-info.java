/**
 * Root package of the MCAgents core API.
 *
 * <p>Everything here is pure Java: interfaces, records, enums, and abstract
 * types that describe what the core can do, never how it does it. No type in
 * this package or below it may reference a Minecraft server, mod loader, or
 * any other platform class, so the same contracts hold on every platform the
 * project targets.</p>
 *
 * <p>{@link io.github.mcagentsmods.core.api.AgentProvider} is the contract that
 * ties the layer together; {@link io.github.mcagentsmods.core.api.AgentException}
 * is what every failure arrives as. The exchange model lives in
 * {@code io.github.mcagentsmods.core.api.chat} and the vendor transport contracts
 * in {@code io.github.mcagentsmods.core.api.llm}.</p>
 *
 * <p>Implementations live in {@code io.github.mcagentsmods.core.common}.</p>
 */
package io.github.mcagentsmods.core.api;
