/**
 * Tools, in pure Java: what a tool is, the registry every mod adds tools to, and the worker
 * pool that runs them.
 *
 * <p>Nothing here knows about Minecraft, a mod loader or the rest of MCAgents core, so any code
 * can register a tool — a mod that reads the game, or plain Java that never touches it.
 *
 * <pre>{@code
 * ToolRegistry tools = ToolRegistry.shared();
 * tools.declareCategory("time", "Dates, times and time zones");
 * tools.register(
 *         ToolSpec.builder("current_time")
 *                 .description("The current date and time in a time zone")
 *                 .category("time")
 *                 .parameters(ToolSchema.builder()
 *                         .string("zone", "An IANA zone such as Europe/Paris", false)
 *                         .build())
 *                 .mode(ExecutionMode.CONCURRENT)
 *                 .build(),
 *         arguments -> ToolResult.ok(ZonedDateTime
 *                 .now(ZoneId.of(arguments.string("zone", "UTC"))).toString()));
 * }</pre>
 *
 * <ul>
 *   <li>{@link io.github.mcagentsmods.core.tool.ToolSpec} — a tool's name, description,
 *       arguments, categories and {@link io.github.mcagentsmods.core.tool.ExecutionMode}.</li>
 *   <li>{@link io.github.mcagentsmods.core.tool.ToolRegistry} — tools by name in a hash map;
 *       duplicate and malformed names are refused at registration.</li>
 *   <li>{@link io.github.mcagentsmods.core.tool.WorkerPool} — at most N pieces of work at
 *       once, N set while running.</li>
 *   <li>{@link io.github.mcagentsmods.core.tool.ToolExecutor} — runs one turn's calls:
 *       concurrent ones together, waiting ones alone, results in the order asked.</li>
 * </ul>
 */
package io.github.mcagentsmods.core.tool;
