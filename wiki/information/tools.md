---
name: wiki-information-tools
description: What a tool is in core — its spec, categories, failures as results, the two kinds of work, blocking and asynchronous bodies, and the worker limit — with the calls on the API pages.
---

# Tools

A tool is code the agent's model can call. Any code registers one: a mod reading the game, or
plain Java that never touches Minecraft. Everything a tool needs is in core's `tool/`
module (`io.github.mcagentsmods.core.tool`), which depends on the JDK alone.

This page says what the parts mean. The calls are on the API pages:
[registering a tool](../api/tool-registry.md), [describing it](../api/tool-spec.md),
[reading its arguments](../api/tool-arguments.md) and [running it](../api/tool-execution.md).

## What a tool is

A tool is a spec and a body, registered together under one name in `ToolRegistry.shared()`,
the registry the agent reads.

* The **spec** is everything the model is told: a name, a description, the categories the tool
  belongs to, and the schema of its arguments. The description is all the model reads when it
  decides whether to call the tool, so it says what the tool does and when it helps.
* The **body** is the code that runs when the model calls it. It reads the model's arguments
  and returns a result as text.

Names are shared by every mod, so a tool's name starts with its mod's name. Registering hands
back a handle; closing it removes the tool, for when what backs it goes away.

## Categories

A category is a group of tools the agent picks as a whole. A tool belongs to one or more: a
tool that counts items might sit in `inventory` and in `minecraft`. The agent's first step
picks categories from their ids and descriptions alone, so only the tools in those categories
are described to the model afterwards; see [`agent.md`](agent.md).

A category nobody declared is created when a tool names it. Declaring one gives it the
description the model reads, and the first description given stays, so mods that share a
category such as `minecraft` cannot keep overwriting each other.

## Failures are results

Every way a call can go wrong, from an unknown tool to a timeout, reaches the model as an error
result rather than ending the player's question. The model reads what went wrong and can call
again differently, or tell the player what it could not do. The full list, with what the model
reads for each, is on [the execution page](../api/tool-execution.md#toolexecutor).

## Two kinds of work

When the model asks for several tools in one turn, the calls run in the order asked, in
stages:

* `CONCURRENT` calls next to each other run together. Use it for a tool that only reads, or
  whose calls cannot affect each other.
* A `WAIT` call runs alone: after everything asked before it has finished, and before anything
  asked after it starts. Use it for a tool that changes something — the world, a file, the
  player's inventory. It is the default because it is never wrong, only slower.

`[read, read, write, read]` runs both reads together, then the write, then the last read. The
results come back in the order asked.

## Blocking and asynchronous tools

* A **blocking** body has done its work when it returns. It runs on a worker thread.
* An **asynchronous** body returns a stage that completes later: a request to a server, or
  work handed to the game's thread. The call keeps its worker slot until the stage completes.

Neither runs on the game's thread. A tool that must touch the game hands that part to the
game's thread and returns the stage;
[`ClientThreadTools`](../api/tool-execution.md#clientthreadtools) does that for a whole tool.

## The worker limit

All tool calls share one worker pool. At most its limit run at once, whether they block or
wait on a stage, and the rest queue, so a slow server counts against the limit exactly as a
long computation does. The player sets the limit in game ([settings](../api/settings.md)), and
it applies at once, as [the pool's page](../api/tool-execution.md#workerpool) describes.
