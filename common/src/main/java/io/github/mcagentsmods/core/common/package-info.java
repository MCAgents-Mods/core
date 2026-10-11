/**
 * Root package of the MCAgents core implementation.
 *
 * <p>Pure Java implementations of the contracts declared in
 * {@code io.github.mcagentsmods.core.api}. Like the API layer, nothing here may
 * reference a Minecraft server, mod loader, or any other platform class.</p>
 *
 * <p>{@link io.github.mcagentsmods.core.common.MCAgentsProvider} is the only public
 * class in the package, and the only one a consumer reads. Everything it
 * delegates to — the client registry, the factory, the HTTP transport, and the
 * two vendor dialects — is package-private, so this facade cannot be routed
 * around. The routed model client is a separate entry point, in
 * {@code io.github.mcagentsmods.core.common.model}.</p>
 *
 * <p>The whole package is deliberately one flat namespace for that reason:
 * splitting the internals into subpackages would force them public and undo the
 * encapsulation.</p>
 */
package io.github.mcagentsmods.core.common;
