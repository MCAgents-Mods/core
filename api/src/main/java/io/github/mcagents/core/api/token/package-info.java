/**
 * Credential storage contracts.
 *
 * <p>Where an API token lives is the consumer's business, not core's: the mod
 * side keeps them in a shared file under the Minecraft directory, and another
 * consumer may keep them somewhere else entirely. Core owns what is done <em>with</em> them —
 * rotation, eviction, and the difference between "never configured" and
 * "every one was rejected" — and reads them through
 * {@link io.github.mcagents.core.api.token.TokenStore}.</p>
 *
 * <p>That split is what lets a consumer hold no token logic at all. It supplies
 * a store and never sees a credential again.</p>
 *
 * <p>Nothing in this package may log, echo, or otherwise reveal a token.</p>
 */
package io.github.mcagents.core.api.token;
