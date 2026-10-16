# Settings

The player sets two numbers in game: how many tool calls run at once, and how many rounds of
tool calls one question may take. `Agents.settings()` reads them, and `Agents.update(settings)`
changes them, applies them at once and saves them to
`{game_dir}/MCAgents/agents/settings.properties`.

```java
AgentSettings now = Agents.settings();          // AgentSettings[workerLimit=2, toolRounds=1]

Agents.update(now.withWorkerLimit(4).withToolRounds(3))
        .exceptionally(failure -> {
            LOGGER.warn("The agents settings were not saved", failure);
            return null;
        });
```

`AgentSettings` and `Agents` are in `io.github.mcagentsmods.core.common.agent`, in
`core-common`.

## The two settings

| Setting | Key in the file | Default | Range | What it does |
|---|---|---|---|---|
| Worker limit | `worker-limit` | 2 | 1 to 32 | How many tool calls run at once, across every question. It is the shared `WorkerPool`'s limit; see [`tool-execution.md`](tool-execution.md#workerpool). |
| Tool rounds | `tool-rounds` | 1 | 1 to 8 | How many rounds of tool calls one question may take, unless the request sets its own; see [`agent.md`](agent.md#agentrequest). |

The player changes both from **Settings** on the agents menu ([`buttons.md`](buttons.md)):
**-** and **+** step each value within its range, **Defaults** puts 2 and 1 back on the screen, **Save**
applies both at once through `Agents.update`, and **Cancel** leaves them as they were.

## The file

`{game_dir}/MCAgents/agents/settings.properties`, a Java properties file. After the update
above it holds:

```properties
#MCAgents agents settings
#Tue Oct 13 03:18:31 UTC 2026
tool-rounds=3
worker-limit=4
```

The path is the one the agents mod used before it became part of core, so a player's
settings carried over. A missing or unreadable file gives both defaults. A value that is missing or not a number gives
that value's default, and one out of range is brought into range, so a hand-edited file can
never stop the game. The file is written beside itself and moved over the old one, so a game
that stops mid-write leaves the old settings rather than half of the new ones. Where the path
comes from is on [`mod-runtime.md`](mod-runtime.md#the-agent-layers-start-up).

## AgentSettings

`record AgentSettings(int workerLimit, int toolRounds)`. The constructor brings both values
into range rather than refusing them: `new AgentSettings(100, 0)` is `[workerLimit=32, toolRounds=1]`.

| Member | Returns | What it does |
|---|---|---|
| `DEFAULTS` | `AgentSettings` | 2 workers and 1 round. |
| `DEFAULT_TOOL_ROUNDS` | `int` | 1. |
| `clampToolRounds(int rounds)` | `int` | Static. `rounds` brought into 1 to 8. The worker limit's twin is `WorkerPool.clampLimit`. |
| `withWorkerLimit(int limit)` | `AgentSettings` | A copy with that limit, brought into range. |
| `withToolRounds(int rounds)` | `AgentSettings` | A copy with that many rounds, brought into range. |
| `load(Path file)` | `AgentSettings` | Static. Reads a file as described above. Never throws. |
| `save(Path file)` | `void` | Writes the file as described above, creating its directory. Throws `IOException`. |

## Agents

The settings part of `Agents`, the access point other mods use. Its other methods are on
[`agent.md`](agent.md), [`buttons.md`](buttons.md), [`tool-registry.md`](tool-registry.md) and
[`tool-execution.md`](tool-execution.md).

| Method | Returns | What it does |
|---|---|---|
| `Agents.settings()` | `AgentSettings` | What applies now. |
| `Agents.update(AgentSettings settings)` | `CompletableFuture<Void>` | Applies `settings` at once, setting the shared pool's limit, then saves them off the calling thread. Completes when they are on disk. If they could not be written it fails, with an `UncheckedIOException` as the cause, and they still apply until the game closes. |
| `Agents.start(Path settingsFile)` | `void` | Loads the file and applies it. `AgentsRuntime` calls it once when the client starts ([`mod-runtime.md`](mod-runtime.md#the-agent-layers-start-up)); a mod has no reason to. |

Before `start` has run, `update` applies the settings in memory and completes at once without
saving, and `start` then replaces them with what the file holds.
