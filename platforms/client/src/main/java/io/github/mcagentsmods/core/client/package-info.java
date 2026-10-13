/**
 * What core does inside the game window, on the client only.
 *
 * <p>This module names Minecraft types, which {@code api}, {@code common} and
 * {@code platforms:shared} never do, and no loader's: each loader module bundles it and
 * connects its own events to it. A dedicated server never reaches a class here, because only
 * the loaders' client entry points refer to one.
 */
package io.github.mcagentsmods.core.client;
