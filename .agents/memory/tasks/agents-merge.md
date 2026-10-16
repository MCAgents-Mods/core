---
name: memory-tasks-agents-merge
description: Folding the MCAgents agents mod into core — the tool module, the tool-calling agent, the menu G opens and its settings — so one foundation mod remains, and releasing it as a minor.
---

# Task: merge the agents mod into core

**Status:** in progress

## The request

On 2026-10-16 the owner wrote, to start after the "Check 26.3 on every loader" work: "Merge
code in repo agents to core. Right now we have two core mods. So we will make it have only
one. After merge then update repo chat too. I will delete repo agents after work finish.
Update repo template too if need any." The owner was away and had asked for the recommended
option to be taken on every question, and for every branch to be opened as a pull request
and rebase-merged in order. The plan is `plans/agents-into-core.md` in the project's shared
files.

## What moves into core

`MCAgents-Mods/agents` 0.5.0 (mod id `mcagents_agents`) is the source. Every class keeps its
name; only its package and module change.

| agents | core |
|---|---|
| `tool/`, package `agents.tool`, artifact `agents-tool` | new module `tool/`, project `:tool`, artifact `core-tool`, package `core.tool` |
| `api`: `agent.Agent`, `AgentAnswer`, `AgentRequest`; `button.AgentButton` | `api`, packages `core.api.agent` and `core.api.button` |
| `api`: `MCAgentsAgents` | dropped; core's mod id serves, and the folder names move into `AgentsPaths` |
| `common`: `Agents`, `AgentButtons`, `AgentSettings`, `AgentsPaths`, `agent.ToolAgent`, `agent.Selection` | `common`, package `core.common.agent` |
| `platforms/shared`: `AgentsRuntime`, `AgentsKeys`, `AgentsMenu`, `ClientThreadTools`, `AgentsSettingsScreen` | `platforms/client`, package `core.client.agent` |
| the three loader entry points | folded into `CoreFabricClient`, `CoreNeoForgeClient`, `CoreForgeClient` |

## Decisions

- **Packages become `io.github.mcagentsmods.core.*`,** like every other class in core. Keeping
  `io.github.mcagentsmods.agents.*` inside core would save chat one import sweep and leave a
  second namespace behind for good.
- **The settings file stays at `{game}/MCAgents/agents/settings.properties`,** so a player's
  worker limit and tool rounds carry over from the agents mod.
- **Core declares `mcagents_agents` incompatible** in all three manifests, so an agents jar
  left in the mods folder is reported rather than opening a second menu on G.
- **The G key joins core's Controls category,** beside Z; its strings move to the
  `mcagents_core` namespace.
- **A minor release,** since new modules and API arrive.
- **The agents repository is not touched.** The owner deletes it; its history stays there
  until then.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/agents-merge-plan` | |
| 2 | Tool module | `feat/tool-registry` | |
| 3 | Agent, buttons and settings | `feat/tool-agent` | |
| 4 | Agents menu, G key and settings screen | `feat/agents-menu` | |
| 5 | Release | `release/{version}` | |

### Task 1 — chore/agents-merge-plan

This record, and its row in the memory index.

### Task 2 — feat/tool-registry

Agents' `tool/` is core's `tool/` module, project `:tool`, package `io.github.mcagentsmods.core.tool`,
jar `MCAgentsCoreTool`, published as `core-tool`. The classes and their five test classes are
unchanged but for the package, two comments that named the agents mod, and the pool's thread
names (`mcagents-tool-worker`, `mcagents-tool-pool`). It builds with core's root conventions
(Java 25 toolchain, JUnit wired once) instead of its own. `api` exposes it as an `api`
dependency, so `core-api` brings `core-tool`, and each loader jar bundles it. The four tool
API pages and `wiki/information/tools.md` moved from agents, without the parts about the agent,
its settings and `ClientThreadTools`, which arrive with that code in tasks 3 and 4. The module,
dependency, setup and overview pages, the README, the repository map, the state file, the
Publish header and both wiki indexes name the new module.
