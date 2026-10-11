/**
 * The local web console's server and the registry mods put their pages into.
 *
 * <p>{@link io.github.mcagentsmods.core.common.web.CoreWebRegistry#shared()} is the entry
 * point. The server, its security, and core's own shell module are package-private, so a mod
 * reaches the console only through the {@code api.web} contracts.
 */
package io.github.mcagentsmods.core.common.web;
