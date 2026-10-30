/**
 * What core adds to a dedicated server: the {@code /agents} command over the server's
 * credential file.
 *
 * <p>This module names Minecraft types, which {@code api}, {@code common} and
 * {@code platforms:shared} never do, and no loader's: each loader module bundles it and
 * registers {@link io.github.mcagentsmods.core.server.AgentsCommand} from its server entry
 * point. A client never reaches a class here, because only the loaders' dedicated server
 * paths refer to one.
 */
package io.github.mcagentsmods.core.server;
