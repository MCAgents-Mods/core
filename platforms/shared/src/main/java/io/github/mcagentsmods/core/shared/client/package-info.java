/**
 * The client half of the mod side.
 *
 * <p>Physically client-only. Everything in this package runs on a machine with
 * a window, a keyboard, and a player sitting in front of it: the credential
 * file under the player's own Minecraft directory, the client commands, and
 * anything drawn on screen. A dedicated server must never load a class from
 * here, which is why nothing outside this package references one by type — the
 * entry point is resolved by name, on the client only.</p>
 *
 * <p>The server half lives in {@code io.github.mcagentsmods.core.shared.server} and
 * this package never depends on it. What both halves share belongs in
 * {@code io.github.mcagentsmods.core.shared} instead.</p>
 */
package io.github.mcagentsmods.core.shared.client;
