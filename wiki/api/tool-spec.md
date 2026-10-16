# Tool Spec

A `ToolSpec` is everything about a tool except its body: its name, its description, its
categories, the schema of its arguments, whether it may run beside other tools, and how long
one call may take. Build it with `ToolSpec.builder(name)` and the arguments' schema with
`ToolSchema.builder()`; every rule is checked when `build()` runs, and a broken one throws
`ToolRegistrationException` there, in your mod.

```java
ToolSpec spec = ToolSpec.builder("mymod_count_items")
        .description("Counts one item in the player's inventory")
        .category("inventory")
        .category("minecraft")
        .parameters(ToolSchema.builder()
                .string("item", "The item id, such as minecraft:oak_log", true)
                .integer("slot", "Only this slot", false)
                .build())
        .mode(ExecutionMode.CONCURRENT)
        .timeout(Duration.ofSeconds(5))
        .build();

spec.parameters().json();
```

```json
{"type":"object","properties":{"item":{"type":"string","description":"The item id, such as minecraft:oak_log"},"slot":{"type":"integer","description":"Only this slot"}},"required":["item"]}
```

Every type on this page is in `io.github.mcagentsmods.core.tool`, in `core-tool`.
Registering the spec is on [`tool-registry.md`](tool-registry.md).

## ToolSpec

`record ToolSpec(String name, String description, ToolSchema parameters, Set<String> categories, ExecutionMode mode, Duration timeout)`

| Component | Rule | When not set |
|---|---|---|
| `name` | 1 to 64 letters, digits, dashes or underscores. Unique across every mod, which `register` checks; prefix it with your mod's id. | Required. |
| `description` | Not blank; stripped. It is all the model reads when it decides whether to call the tool. | Required. |
| `parameters` | A `ToolSchema`, below. | `ToolSchema.empty()`: no arguments. |
| `categories` | One or more category ids, each under the same rule as a name. Kept in the order given, each once. | Required. |
| `mode` | An `ExecutionMode`, below. | `WAIT`. |
| `timeout` | Above zero. How long one call may run once it has a worker slot; see [`tool-execution.md`](tool-execution.md). | `DEFAULT_TIMEOUT`, 60 seconds. |

| Member | What it is |
|---|---|
| `DEFAULT_TIMEOUT` | `Duration.ofSeconds(60)`. |
| `isIn(String category)` | Whether the tool belongs to that category. |
| `builder(String name)` | Static. Starts a spec. |

Calling the record's constructor directly applies the same rules, and the same defaults for
`null` parameters, mode or timeout; a `null` description is refused like a blank one.

## ToolSpec.Builder

| Method | What it does |
|---|---|
| `description(String)` | Sets the description. |
| `category(String)` | Adds one category. Call it again for each further one. |
| `categories(Collection<String>)` | Adds each category in turn. |
| `parameters(ToolSchema)` | Sets the arguments' schema. |
| `mode(ExecutionMode)` | Sets how it runs. |
| `timeout(Duration)` | Sets how long one call may take. |
| `build()` | Checks every rule and returns the spec, or throws `ToolRegistrationException`. |

A `null` passed to any setter throws `NullPointerException` at once. The messages `build()`
throws name the tool:

```
The tool 'mymod_bad' needs a description: it is all the model reads when it picks tools
The tool name 'my mod' is not one a model provider accepts: use 1 to 64 letters, digits, dashes or underscores
The tool 'mymod_x' needs at least one category
```

## ToolSchema

`record ToolSchema(String json)`: the JSON Schema of a tool's arguments, which the model is
sent with the tool. It is checked to be a JSON object when it is made, not when a model first
sees it.

| Method | Returns | What it does |
|---|---|---|
| `ToolSchema.empty()` | `ToolSchema` | `{"type":"object","properties":{}}`, for a tool with no arguments. |
| `ToolSchema.of(String json)` | `ToolSchema` | A schema written elsewhere, such as an MCP server's. Stripped; `null` or blank gives `empty()`. Anything but a JSON object throws `ToolRegistrationException`. |
| `ToolSchema.builder()` | `ToolSchema.Builder` | An object schema, one argument at a time. |
| `json()`, `toString()` | `String` | The schema as the model is sent it. |

## ToolSchema.Builder

Each method adds one argument, in the order called, and lists it under `"required"` when
`required` is `true`. A blank or `null` description is left out. The last column is how a body
reads the value back, on [`tool-arguments.md`](tool-arguments.md).

| Method | The argument is | Its schema | Read it with |
|---|---|---|---|
| `string(name, description, required)` | Text | `"type":"string"` | `string` |
| `choice(name, description, required, List<String> allowed)` | One of `allowed` | `"type":"string","enum":[…]` | `string` |
| `choices(name, description, required, List<String> allowed)` | A list, each item one of `allowed` | `"type":"array","items":{"type":"string","enum":[…]}` | `strings` |
| `integer(name, description, required)` | A whole number | `"type":"integer"` | `integer` |
| `number(name, description, required)` | A number | `"type":"number"` | `number` |
| `bool(name, description, required)` | True or false | `"type":"boolean"` | `bool` |
| `strings(name, description, required)` | A list of text | `"type":"array","items":{"type":"string"}` | `strings` |
| `property(name, ToolSchema schema, required)` | Any shape, described by its own schema | The schema's members, as given; put the description inside it | `object`, `list` or the matching reader |
| `build()` | — | The object schema | — |

An argument declared twice, a blank argument name, or a `choice` or `choices` with no allowed
values throws `ToolRegistrationException`.

## ExecutionMode

Whether a tool may run at the same time as other tools. Which to choose, and how the calls of
one turn run in stages, is on [`../information/tools.md`](../information/tools.md#two-kinds-of-work).

| Value | A call runs |
|---|---|
| `WAIT` | Alone. The default, because it is never wrong, only slower. |
| `CONCURRENT` | Together with the `CONCURRENT` calls next to it, up to the worker limit. |
