# Tool Arguments

A tool's body is handed `ToolArguments`, the model's JSON already parsed, and returns a
`ToolResult`. Read each argument by name with the reader for its type. A missing or mistyped
argument throws `ToolArgumentException`, which the model receives as an error result it can
read and correct, so a body never checks for it.

```java
ToolArguments arguments = ToolArguments.parse("{\"item\":\"minecraft:oak_log\",\"count\":3.0}");

arguments.string("item");          // "minecraft:oak_log"
arguments.integer("count");        // 3: a whole number written with a decimal point is accepted
arguments.bool("exact", false);    // false: missing, so the fallback
arguments.string("slot");          // throws ToolArgumentException: The argument 'slot' is required
arguments.string("count");         // throws ToolArgumentException: The argument 'count' must be text
```

Inside a tool:

```java
Tool countItems = arguments -> {
    String item = arguments.string("item");
    long limit = arguments.integer("limit", 64);
    return ToolResult.ok(String.valueOf(count(item, limit)));
};
```

Every type on this page is in `io.github.mcagentsmods.core.tool`, in `core-tool`.
The schema that tells the model what to send is on [`tool-spec.md`](tool-spec.md).

## ToolArguments

| Method | Returns | Accepts |
|---|---|---|
| `string(name)` | `String` | Text only. A number is not turned into text. |
| `integer(name)` | `long` | A whole number, or a number with no fraction such as `3.0`. |
| `number(name)` | `double` | Any number. |
| `bool(name)` | `boolean` | `true` or `false` only. |
| `strings(name)` | `List<String>` | A list whose every item is text. |
| `list(name)` | `List<Object>` | Any list. |
| `object(name)` | `Map<String, Object>` | A nested object. |
| `has(name)` | `boolean` | Whether the model sent `name` with a value other than `null`. |

Four readers take a fallback: `string(name, fallback)`, `integer(name, fallback)`,
`number(name, fallback)` and `bool(name, fallback)`. The fallback answers when the argument is
missing or `null`; a value of the wrong type still throws. The list and object readers have
none, so check `has(name)` before reading an optional one.

| Method | Returns | What it does |
|---|---|---|
| `ToolArguments.parse(String json)` | `ToolArguments` | Static. Reads one call's arguments. `null` or blank means none. Anything but a JSON object, or JSON nested more than 64 levels deep, throws `ToolArgumentException`. |
| `ToolArguments.none()` | `ToolArguments` | Static. No arguments: `{}`. |
| `json()`, `toString()` | `String` | The arguments exactly as the model sent them. |
| `asMap()` | `Map<String, Object>` | Every argument by name, unmodifiable: objects are maps, arrays are lists, whole numbers are `Long`, other numbers and whole numbers too long for a `long` are `Double`, and the rest are `String`, `Boolean` or `null`. |

A body is never handed malformed JSON: the executor parses the arguments before the body runs
and answers bad ones itself.

## What the model reads when an argument is wrong

The message of the `ToolArgumentException`, behind the error prefix `ToolResult.error` adds:

| Case | Message |
|---|---|
| Missing, or `null` | `The argument 'item' is required` |
| The wrong type | `The argument 'item' must be text`, `… must be a whole number`, `… must be a number`, `… must be true or false`, `… must be a list`, `… must be an object` |
| A list with an item that is not text, read with `strings` | `Every item of 'tags' must be text` |
| Arguments that are not a JSON object | `The arguments must be a JSON object` |
| Arguments that are not JSON | `The arguments are not valid JSON.`, then where it went wrong |

## ToolArgumentException

An unchecked `IllegalArgumentException`. The readers throw it; a body may throw it too, for a
value that parses but makes no sense, such as an item id the game does not know. The executor
answers the call with its message as an error result, so the model reads what was wrong and
can call again. Any other exception also becomes an error result; see
[`tool-execution.md`](tool-execution.md).

## ToolResult

`record ToolResult(String text, boolean isError)`: what a call produced, as the model reads it.

| Member | What it does |
|---|---|
| `ToolResult.ok(String text)` | A call that worked. `text` is what the model reads. |
| `ToolResult.error(String reason)` | A call that failed. The text becomes `The tool call failed: ` followed by `reason`, because a model handed a bare sentence often repeats it to the player as though it were the answer. |
| `text()` | What the model reads. Never `null`. |
| `isError()` | Whether the call failed, so the model is not left to infer it. |

A failure is a result too: return `error` for something the model can act on, calling again
differently or telling the player what could not be done, rather than throwing.
