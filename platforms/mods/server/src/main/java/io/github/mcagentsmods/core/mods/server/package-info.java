/**
 * The dedicated server half of the mod side.
 *
 * <p>Physically server-only. Everything in this package runs where there is no
 * window and no local player: the credential file under the server's own run
 * directory, the checks deciding who is allowed to do what, and the decisions
 * that must be made where a client cannot tamper with them. A client may load
 * these classes when it hosts a single player world, but they never assume a
 * screen.</p>
 *
 * <p>The client half lives in {@code io.github.mcagentsmods.core.mods.client} and
 * this package never depends on it. What both halves share belongs in
 * {@code io.github.mcagentsmods.core.mods} instead.</p>
 */
package io.github.mcagentsmods.core.mods.server;
