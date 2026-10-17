---
name: memory-index
description: Index of .agents/memory/ — ongoing task records, session logs, durable decisions, and the current state of the repository.
---

# Memory Index

**Scope:** `.agents/memory/`
**Parent:** [root-index](root-index.md)

This index owns `.agents/memory/` and only `.agents/memory/`. Any memory file
created or removed is reflected here **in the same commit** that creates or removes
it.

**Read this index at the start of every session** and load only the rows whose scope
matches the request, so you continue prior work instead of restarting it. That
standing exception to the one-branch routing rule is why the `Scope` column exists —
match on it before opening anything. Writing here needs no approval; see
`{shared}/rules/memory-policy.md` and `{shared}/creators/memory-creator.md`.

## State

| File | Scope | Purpose |
|---|---|---|
| [`../memory/state/repository-state.md`](../memory/state/repository-state.md) | Whole repository | What exists today, what the stack is, what is not built yet, and the next obvious step. |

## Tasks

| File | Scope | Purpose |
|---|---|---|
| [`../memory/tasks/agents-setup.md`](../memory/tasks/agents-setup.md) | `.agents/`, `wiki/` | The agent instruction, knowledge, and memory system: what was built, and the decisions behind it. |
| [`../memory/tasks/no-session-links.md`](../memory/tasks/no-session-links.md) | `.agents/` instruction set | Making the no-session-links rule universal: what was threaded where, and the judgement calls behind it. |
| [`../memory/tasks/discovery-protocol-extraction.md`](../memory/tasks/discovery-protocol-extraction.md) | `.agents/` instruction set | Extracting the discovery protocol into its own rule, and which six inline copies were kept on purpose. |
| [`../memory/tasks/agents-instruction-rewrite.md`](../memory/tasks/agents-instruction-rewrite.md) | `.agents/`, `AGENTS.md` | Re-writing the instruction system against the connector: the sixteen duplicates removed, what was kept, and why. |
| [`../memory/tasks/change-propagation-promotion.md`](../memory/tasks/change-propagation-promotion.md) | `.agents/`, `AGENTS.md` | Dropping the local change-propagation rule once the shared set grew its own, and the release gate that blocks the merge. |
| [`../memory/tasks/mods-foundation.md`](../memory/tasks/mods-foundation.md) | Whole repository | Turning core into the mods foundation: Bukkit removed, the new namespace, one jar per loader, the shared language model client, and the web page registry. |
| [`../memory/tasks/artifact-names.md`](../memory/tasks/artifact-names.md) | Gradle builds, published coordinates | Renaming every module's jar to `MCAgentsCore{Module}` and its artifact id to `mcagents-core-{module}`. |
| [`../memory/tasks/publish-workflow.md`](../memory/tasks/publish-workflow.md) | `.github/workflows/`, releases | The Publish workflow that tests, publishes and tags each new version on `master`, and shipping 0.5.0 through it. |
| [`../memory/tasks/test-fixtures-capability.md`](../memory/tasks/test-fixtures-capability.md) | `common/build.gradle`, published metadata | Naming `mcagents-core-common`'s test fixtures after the artifact, which 0.5.0 got wrong, and releasing 0.5.1. |
| [`../memory/tasks/forge-pack-metadata.md`](../memory/tasks/forge-pack-metadata.md) | `platforms/forge/`, Forge resources | Adding `pack.mcmeta` to the Forge jar, whose absence crashes Forge 26.2 on world creation, and releasing 0.5.2. |
| [`../memory/tasks/api-wiki.md`](../memory/tasks/api-wiki.md) | `wiki/api/`, API documentation | Splitting the API reference into one page per scope under `wiki/api/`, and releasing 0.5.3. |
| [`../memory/tasks/hologram.md`](../memory/tasks/hologram.md) | `platforms/client/`, the hologram, the web console | The in-game hologram framework that replaces the web console, and releasing 0.6.0. |
| [`../memory/tasks/release-workflow.md`](../memory/tasks/release-workflow.md) | `.github/workflows/`, GitHub releases | The Release workflow that attaches the Forge, Fabric and NeoForge jars to each tag's GitHub release, and releasing 0.6.1. |
| [`../memory/tasks/minecraft-26-3.md`](../memory/tasks/minecraft-26-3.md) | Gradle builds, `platforms/`, Minecraft versions | Building every loader jar for Minecraft 26.2 and 26.3, one subproject per version, and releasing 0.7.0. |
| [`../memory/tasks/latest-minecraft.md`](../memory/tasks/latest-minecraft.md) | Gradle builds, `platforms/`, Minecraft versions, published coordinates | Dropping 26.2 so core supports only the latest Minecraft release, removing the Minecraft suffix from project and artifact ids, and releasing 0.8.0. |
| [`../memory/tasks/artifact-ids.md`](../memory/tasks/artifact-ids.md) | Gradle builds, published coordinates | Dropping `mcagents-` from every artifact id, publishing only the library modules, and releasing 0.9.0. |
| [`../memory/tasks/agents-merge.md`](../memory/tasks/agents-merge.md) | Whole repository, `tool/`, tool calling, the agents menu | Folding the agents mod into core: the tool module, the tool-calling agent, the menu G opens and its settings, so one foundation mod remains. |
| [`../memory/tasks/prompt-typing.md`](../memory/tasks/prompt-typing.md) | `platforms/client/`, the hologram's prompt | Making typed text reach a hologram's prompt box on Minecraft 26.3, which hands characters only to a screen that asks for text input, and releasing 0.10.1. |
| [`../memory/tasks/multi-hologram.md`](../memory/tasks/multi-hologram.md) | `platforms/client/`, `api/hologram`, holograms | Letting several holograms stand at once, the pick list panel and transcript buttons chat's session picker needs, and releasing 0.11.0. |
| [`../memory/tasks/compile-only.md`](../memory/tasks/compile-only.md) | Gradle builds, `platforms/` | Replacing every `implementation` in the loader modules with `compileOnly` plus a dev-client `runtimeOnly` twin, with no jar changing, and releasing 0.11.1. |
| [`../memory/tasks/hologram-forms.md`](../memory/tasks/hologram-forms.md) | `platforms/client/`, `api/hologram`, holograms, the agents menu | Adding the form panel so every agents menu button can open a hologram, core's Settings as one, and releasing 0.12.0. |
| [`../memory/tasks/model-providers.md`](../memory/tasks/model-providers.md) | `api/model`, `common/model`, model routing and settings | Routing each request to the provider the player picked, custom providers, the global model every task falls back to, and releasing 0.13.0. |

## Decisions

| File | Scope | Purpose |
|---|---|---|
| [`../memory/decisions/minecraft-latest-only.md`](../memory/decisions/minecraft-latest-only.md) | Minecraft versions, `platforms/`, jar and artifact names | The owner's permanent decision: only the latest Minecraft release, the version kept in jar names but not in artifact ids. |
