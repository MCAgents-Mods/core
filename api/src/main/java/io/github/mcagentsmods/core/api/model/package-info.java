/**
 * The routed model client's contracts.
 *
 * <p>{@link io.github.mcagentsmods.core.api.model.ModelClient} sends a
 * {@link io.github.mcagentsmods.core.api.model.ChatRequest} — which may offer tools and mark
 * cache breakpoints — and embeds text. Where a request goes is decided per request by
 * {@link io.github.mcagentsmods.core.api.model.ProviderRoutes}, from a stored
 * {@link io.github.mcagentsmods.core.api.model.ModelName}, and which key it uses by an
 * {@link io.github.mcagentsmods.core.api.model.ApiKeySource} the consuming mod supplies.
 *
 * <p>Every type here is an immutable record, an enum-like constant holder, or an interface,
 * so an instance can be handed to another thread without copying or locking. The
 * implementation lives in {@code io.github.mcagentsmods.core.common.model}.
 */
package io.github.mcagentsmods.core.api.model;
