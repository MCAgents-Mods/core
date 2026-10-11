/**
 * One HTTP exchange, described without naming an HTTP library.
 *
 * <p>{@link io.github.mcagentsmods.core.api.http.HttpTransport} is the seam the routed model
 * client sends through. Tests drive it with a scripted stub, and a mod may reuse it for its
 * own HTTP calls so they share one timeout policy.
 */
package io.github.mcagentsmods.core.api.http;
