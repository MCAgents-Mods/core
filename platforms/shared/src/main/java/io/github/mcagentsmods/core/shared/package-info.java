/**
 * Shared mod code — every loader, and both physical sides.
 *
 * <p>Everything the loader modules have in common lives here. Two rules decide
 * what belongs:</p>
 *
 * <ul>
 *   <li><strong>Loader agnostic.</strong> Code needing a Forge, NeoForge or Fabric
 *       type belongs in that loader's own module.</li>
 *   <li><strong>Side agnostic.</strong> Code that only makes sense on a client,
 *       or only where a client cannot be trusted, belongs in
 *       {@code io.github.mcagentsmods.core.shared.client} or
 *       {@code io.github.mcagentsmods.core.shared.server}. A branch on the side
 *       inside a shared class is the shape this split exists to avoid.</li>
 * </ul>
 *
 * <p>The machinery that decides which side is running, and starts the matching
 * half without linking the other, is in
 * {@link io.github.mcagentsmods.core.shared.environment}.</p>
 */
package io.github.mcagentsmods.core.shared;
