/**
 * Shared mod code — every loader, and both physical sides.
 *
 * <p>Everything the mod modules have in common lives here. Two rules decide
 * what belongs:</p>
 *
 * <ul>
 *   <li><strong>Loader agnostic.</strong> Code needing a NeoForge or Fabric
 *       type belongs in that loader's own module.</li>
 *   <li><strong>Side agnostic.</strong> Code that only makes sense on a client,
 *       or only where a client cannot be trusted, belongs in
 *       {@code io.github.mcagents.core.mods.client} or
 *       {@code io.github.mcagents.core.mods.server}. A branch on the side
 *       inside a shared class is the shape this split exists to avoid.</li>
 * </ul>
 *
 * <p>The machinery that decides which side is running, and starts the matching
 * half without linking the other, is in
 * {@link io.github.mcagents.core.mods.environment}.</p>
 */
package io.github.mcagents.core.mods;
