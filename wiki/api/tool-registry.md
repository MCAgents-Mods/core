# Tool Registry

`ToolRegistry` holds every tool by name. Register into `ToolRegistry.shared()`, the registry
core's agent reads, and keep the `ToolRegistration` it returns: closing it removes
the tool. How to describe a tool is on [`tool-spec.md`](tool-spec.md), and how to write its
body on [`tool-execution.md`](tool-execution.md).

```java
ToolRegistry tools = ToolRegistry.shared();
tools.declareCategory("time", "Dates, times and time zones");

ToolRegistration registration = tools.register(
        ToolSpec.builder("mymod_current_time")
                .description("The current date and time in UTC")
                .category("time")
                .build(),
        arguments -> ToolResult.ok(Instant.now().toString()));

tools.contains("mymod_current_time");   // true
tools.categories();                     // [ToolCategory[id=time, description=Dates, times and time zones]]

registration.close();
tools.contains("mymod_current_time");   // false
tools.categories();                     // [] — the category is kept, but holds no tool
```

Every type on this page is in `io.github.mcagentsmods.core.tool`, in `core-tool`.
`Agents.tools()` in `core-common` returns the same shared registry.

## ToolRegistry

| Method | Returns | What it does |
|---|---|---|
| `shared()` | `ToolRegistry` | Static. The one registry every mod registers into and the agent reads. |
| `ToolRegistry()` | — | A registry of your own, for tests and for code using `tool` on its own. The agent never reads it. |
| `declareCategory(String id, String description)` | `ToolCategory` | Gives a category the description the model reads. The first non-blank description stays, so every mod sharing a category such as `minecraft` may call it. Returns the category as it now stands. |
| `register(ToolSpec, Tool)` | `ToolRegistration` | Adds a tool whose work is done when its body returns. A category the spec names that nobody declared is created with no description. |
| `registerAsync(ToolSpec, AsyncTool)` | `ToolRegistration` | The same, for a body that returns a `CompletionStage`. |
| `unregister(String name)` | `boolean` | Removes the tool by name, whoever registered it. `false` when there was none. Prefer closing your own `ToolRegistration`. |
| `find(String name)` | `Optional<RegisteredTool>` | One hash lookup. Empty for `null` or an unknown name. |
| `contains(String name)` | `boolean` | Whether a tool has that name. |
| `size()` | `int` | How many tools are registered. |
| `isEmpty()` | `boolean` | Whether none are. |
| `category(String id)` | `Optional<ToolCategory>` | The category, whether or not any tool is in it now. |
| `categories()` | `List<ToolCategory>` | Every category holding at least one tool, sorted by id. |
| `tools()` | `List<ToolSpec>` | Every tool's spec, sorted by name. |
| `toolsIn(Collection<String> categoryIds)` | `List<ToolSpec>` | The tools in any of those categories, each once, sorted by name. |
| `snapshot()` | `ToolRegistry.Snapshot` | The registry as it stands now, unchanged by later registrations. |

Tools sit in a `ConcurrentHashMap` keyed by name. Registering and removing are serialized, so
a tool and its categories arrive together; reads never wait, and every method is safe from
any thread. Removing a tool never removes a category.

## ToolRegistry.Snapshot

A frozen view, taken with `snapshot()`. The agent takes one per question and uses it for every
step, so the tools the model was shown are the tools its calls run against, even if a mod
removes one meanwhile.

| Method | Returns | What it does |
|---|---|---|
| `find(String name)` | `Optional<RegisteredTool>` | One hash lookup, as on the registry. |
| `size()`, `isEmpty()` | `int`, `boolean` | As on the registry, at the moment it was taken. |
| `categories()` | `List<ToolCategory>` | Every category holding at least one tool, sorted by id. |
| `tools()` | `List<ToolSpec>` | Every tool's spec, sorted by name. |
| `toolsIn(Collection<String> categoryIds)` | `List<ToolSpec>` | The tools in any of those categories, each once, sorted by name. |
| `specs(Collection<String> names)` | `List<ToolSpec>` | The named tools' specs in the order named, skipping names it does not hold. |

## ToolRegistration

What `register` and `registerAsync` return. It extends `AutoCloseable`, and its `close()`
throws nothing, so it needs no `catch`.

| Method | Returns | What it does |
|---|---|---|
| `name()` | `String` | The tool's name. |
| `close()` | `void` | Removes the tool, if this registration still holds its name. Closing twice, or after the name was removed and registered again by someone else, does nothing. |

Close it when what backs the tool goes away, such as a server that disconnects.

## ToolCategory

`record ToolCategory(String id, String description)`: a group of tools the agent picks as a
whole. The registry makes them; a mod names them in a spec and describes them with
`declareCategory`.

| Member | Meaning |
|---|---|
| `id` | What the model picks it by. 1 to 64 letters, digits, dashes or underscores. |
| `description` | What its tools are for, as the model reads it. Stripped; blank for a category a tool named before anyone described it. |
| `isDescribed()` | Whether it carries a description yet. |

## ToolNames

| Method | Returns | What it does |
|---|---|---|
| `ToolNames.isValid(String name)` | `boolean` | Whether `name` could be a tool name or a category id: 1 to 64 letters, digits, dashes or underscores, the rule OpenAI-compatible providers apply to a function name. |

The rule is checked when a tool is registered rather than when it is sent, because a provider
refuses the whole request over one bad name.

## ToolRegistrationException

An unchecked `IllegalArgumentException`, thrown in the mod at fault before any player question
depends on the tool. Its message names the tool.

| Thrown by | When |
|---|---|
| `register`, `registerAsync` | Another tool already has the name. The message suggests a prefix, such as `mymod_`, because names are shared by every mod. |
| `ToolSpec.Builder.build()` | A name or category id `ToolNames` refuses, a blank description, no category, or a timeout of zero or less. See [`tool-spec.md`](tool-spec.md). |
| `ToolSchema.of`, `ToolSchema.Builder` | A schema that is not a JSON object, an argument declared twice or with a blank name, or a choice with no values. |
| `declareCategory` | An id `ToolNames` refuses. |

A second tool called `count_items` is refused with:

```
A tool named 'count_items' is already registered. Tool names are shared by every mod, so prefix yours, such as mymod_count_items
```
