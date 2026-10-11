/**
 * Which physical side is running, and how the right half is started.
 *
 * <p>A mod runs in two physically different processes. A client has a window, a
 * local player, and every Minecraft class. A dedicated server has none of the
 * first two and <em>not all</em> of the third: the client classes are simply
 * absent from its distribution. Code that ignores the difference does not
 * misbehave on a server — it fails to link, and takes the server down with
 * it.</p>
 *
 * <p>This package is how that is avoided, in three parts:</p>
 *
 * <ul>
 *   <li>{@link io.github.mcagentsmods.core.shared.environment.ModEnvironment}
 *       answers which side this is — from what the loader reported, from a
 *       system property, or from a classpath probe, in that order.</li>
 *   <li>{@link io.github.mcagentsmods.core.shared.environment.ModBootstrap} starts
 *       the matching half, resolving it <strong>by name</strong> so the other
 *       half is never loaded, linked, or verified.</li>
 *   <li>{@link io.github.mcagentsmods.core.shared.environment.SideGuard} turns a
 *       wiring mistake into a
 *       {@link io.github.mcagentsmods.core.shared.environment.WrongSideException}
 *       that says what went wrong, instead of a
 *       {@code NoClassDefFoundError} several frames away.</li>
 * </ul>
 *
 * <p>{@link io.github.mcagentsmods.core.shared.environment.ClientOnly} and
 * {@link io.github.mcagentsmods.core.shared.environment.ServerOnly} mark which half
 * a type belongs to. They are the loader-neutral spelling of Fabric's
 * {@code @Environment} and NeoForge's {@code @OnlyIn}.</p>
 */
package io.github.mcagentsmods.core.shared.environment;
