/**
 * The local web console that every MCAgents mod registers its pages into.
 *
 * <p>A mod implements {@link io.github.mcagentsmods.core.api.web.WebModule} and registers it
 * with a {@link io.github.mcagentsmods.core.api.web.WebRegistry}; core runs one loopback
 * server for all of them, owns its security, and serves the shell their pages share. The
 * implementation is {@code io.github.mcagentsmods.core.common.web.CoreWebRegistry}.
 */
package io.github.mcagentsmods.core.api.web;
