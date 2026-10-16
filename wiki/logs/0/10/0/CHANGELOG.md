# 0.10.0

Released: 2026-10-16

Core takes in the MCAgents agents mod. The tool registry, the agent that answers a prompt with
the tools it needs, the menu G shows and its settings screen are part of core, so one
foundation mod remains where there were two.

## Upgrading

- **Remove the MCAgents agents jar.** Core does what it did, and refuses to load beside it:
  Fabric and NeoForge name it incompatible, and Forge asks for an agents version that was
  never released.
- **Players keep their settings.** The worker limit and tool rounds are still read from
  `{game}/MCAgents/agents/settings.properties`. The G key moves to core's Controls category,
  beside Z, so a player who rebound it binds it again once.
- **Mods import from core's packages.** Every class keeps its name:

  | Before, in agents | From 0.10.0, in core |
  |---|---|
  | `io.github.mcagentsmods.agents.tool`, `agents-tool` | `io.github.mcagentsmods.core.tool`, `core-tool` |
  | `io.github.mcagentsmods.agents.api.agent`, `agents-api` | `io.github.mcagentsmods.core.api.agent`, `core-api` |
  | `io.github.mcagentsmods.agents.api.button`, `agents-api` | `io.github.mcagentsmods.core.api.button`, `core-api` |
  | `io.github.mcagentsmods.agents.common`, `agents-common` | `io.github.mcagentsmods.core.common.agent`, `core-common` |

  `core-api` brings `core-tool`, so a mod that compiles against `core-common` needs nothing
  more. See [`wiki/api/dependency.md`](../../../../api/dependency.md).
- **Depend on `mcagents_core` alone** in a mod's manifest.

## Added

- `tool/`, published as `core-tool`: `ToolRegistry`, `ToolSpec`, `ToolArguments`, the worker
  pool and the rest of the pure-Java tool layer. See
  [`wiki/api/tool-registry.md`](../../../../api/tool-registry.md).
- The agent: `Agents.agent(client)`, `ToolAgent`, `AgentRequest` and `AgentAnswer`, and the
  player's agent settings. See [`wiki/api/agent.md`](../../../../api/agent.md) and
  [`wiki/api/settings.md`](../../../../api/settings.md).
- The agents menu on G, with `AgentButton` and `Agents.buttons()` for each mod's button, and its
  settings screen. See [`wiki/api/buttons.md`](../../../../api/buttons.md).

## Changed

- Each loader's client start-up registers the G key beside Z and starts the agent layer after
  core's own runtime.
