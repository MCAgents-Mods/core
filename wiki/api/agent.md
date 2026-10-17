# Agent

`Agents.agent(modelClient)` gives an `Agent`, which answers an `AgentRequest` with the
registered tools the prompt needs and completes with an `AgentAnswer`. You supply core's
`ModelClient` and a model id; the agent sends every request through that client. How the
three steps and tool rounds work is on [`../information/agent.md`](../information/agent.md).

```java
Agent agent = Agents.agent(modelClient);

agent.answer(AgentRequest.of(model, "How many oak logs do I have?")
                .withCategories(List.of("inventory")))
        .thenAccept(answer -> show(answer.text()));
```

With one tool, `mymod_count_items` in `inventory`, and a model that calls it once and then
answers, the question takes three requests: step 1 is skipped because the request allows one
category, step 2 picks the tool, and step 3 takes two, one that calls the tool and one that
answers. The answer carries:

```
text()          You carry 12 oak logs.
categories()    [inventory]
tools()         [mymod_count_items]
toolRounds()    1
stoppedShort()  false
toolResults()   [ToolOutcome[callId=c1, toolName=mymod_count_items, result=ToolResult[text=12, isError=false]]]
```

`modelClient` is core's client, built as the
[model client page](model-client.md)
says; `model` is a model id as core stores it, see
[model routing](model-routing.md).

## Agent

`io.github.mcagentsmods.core.api.agent.Agent`, in `core-api`.

| Method | Returns | What it does |
|---|---|---|
| `answer(AgentRequest request)` | `CompletableFuture<AgentAnswer>` | Answers the question, calling the tools it needs. |

The future fails only when a model request fails, with `ModelException`
([failures](failures.md)); never
because a tool failed, since a tool's failure is a result the model reads. It may complete on
any thread, the model client's or a worker's, so a callback that touches the game hands that
part to the game's thread first, with `Minecraft.getInstance().execute`.

## Agents.agent

| Method | Returns | What it does |
|---|---|---|
| `Agents.agent(ModelClient client)` | `Agent` | An agent over every tool in `ToolRegistry.shared()`, running them on the shared worker pool, with the player's tool rounds. Static, in `io.github.mcagentsmods.core.common.agent`. |

It holds nothing of its own and reads the registry and the player's tool rounds afresh for
every question, so building one per question costs nothing, and keeping one sees later tools
too.

## AgentRequest

`record AgentRequest(String model, String prompt, List<PromptMessage> conversation, Set<String> categories, int toolRounds, String provider)`,
in `io.github.mcagentsmods.core.api.agent`. `PromptMessage` is core's; see
[model exchange](model-exchange.md).

| Component | Meaning | From `of` |
|---|---|---|
| `model` | The model id to ask, as core stores it. Not blank. | Given. |
| `prompt` | The player's request. Steps 1 and 2 pick categories and tools by it. Not blank. | Given. |
| `conversation` | The messages the answer is written from, oldest first. `null` or empty becomes the prompt alone, as a user message. | The prompt alone. |
| `categories` | The category ids the agent may use. Empty means every category. | Every category. |
| `toolRounds` | How many times the model may ask for tools before it must answer: 1 to `MAX_TOOL_ROUNDS`, or `PLAYER_SETTING`. | `PLAYER_SETTING`. |
| `provider` | The provider every step is sent to ([`model-routing.md`](model-routing.md#modelchoice)). Blank means none chosen: the model is routed by its name alone. The five-argument constructor leaves it blank. | Blank. |

| Member | What it does |
|---|---|
| `AgentRequest.of(String model, String prompt)` | Static. One question, answered from the prompt alone, with every category and the player's tool rounds. |
| `AgentRequest.of(ModelChoice choice, String prompt)` | Static. The same, asked of that model at that provider, such as `Models.global().chatOr(own)` ([`global-models.md`](global-models.md)). |
| `withProvider(String provider)` | Sends every step to that provider; blank means none chosen. |
| `withConversation(List<PromptMessage> messages)` | Answers from a whole conversation, such as a persona and earlier turns, which should end with the question. The prompt is still what steps 1 and 2 read. |
| `withCategories(Collection<String> ids)` | Limits the question to those categories, such as your mod's own. A category with no tool in it is ignored; an empty collection means every category again. |
| `withToolRounds(int rounds)` | This question's own number of rounds instead of the player's. |
| `isLimitedToCategories()` | Whether `categories` is not empty. |
| `PLAYER_SETTING` | `0`: use the rounds the player set; see [`settings.md`](settings.md). |
| `MAX_TOOL_ROUNDS` | `8`. |

A blank model or prompt, or tool rounds outside 0 to 8, throws `IllegalArgumentException`; a
`null` model or prompt throws `NullPointerException`. The `with` methods return a new request.

## AgentAnswer

`record AgentAnswer(String text, String model, TokenUsage usage, List<String> categories, List<String> tools, List<ToolOutcome> toolResults, int toolRounds, boolean stoppedShort)`,
in `io.github.mcagentsmods.core.api.agent`.

| Member | Meaning |
|---|---|
| `text` | The answer for the player. |
| `model` | The model that wrote it, as the provider reported it; empty when not reported. |
| `usage` | The tokens of every request the question took, added together. Core's `TokenUsage`; see [model exchange](model-exchange.md). |
| `categories` | The categories the agent picked, in the order picked. |
| `tools` | The names of the tools offered to the model for the answer. |
| `toolResults` | Every call the model made and what it produced, in order; see `ToolOutcome` on [`tool-execution.md`](tool-execution.md). |
| `toolRounds` | How many rounds of tool calls ran. |
| `stoppedShort` | Whether the model still wanted tools when its rounds ran out. The text is then what it had written, or `I kept needing more tools and did not reach an answer. Ask again, or narrow the question.` when it had written nothing. |
| `usedTools()` | Whether any tool ran. |

## ToolAgent

`io.github.mcagentsmods.core.common.agent.ToolAgent`, in `core-common`: the
`Agent` that `Agents.agent` builds. Build one yourself for an agent over a registry or a pool
of your own, or in a test with a scripted model client.

| Constructor argument | Type | What it supplies |
|---|---|---|
| `client` | `ModelClient` | Where every request goes. |
| `registry` | `ToolRegistry` | The tools to choose from. A snapshot is taken per question. |
| `executor` | `ToolExecutor` | What runs the tools the model calls; see [`tool-execution.md`](tool-execution.md). |
| `playerToolRounds` | `IntSupplier` | The rounds for a request that asks for `PLAYER_SETTING`, read per question and brought into 1 to 8. |

`Agents.agent(client)` builds one over `ToolRegistry.shared()`, an executor on
`WorkerPool.shared()`, and `Agents.settings().toolRounds()`.
