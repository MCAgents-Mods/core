---
name: repository-rules
description: Rules specific to the MCAgents-Mods/core repository — its current state, what exists, and what must not be assumed or introduced.
---

# Repository Rules

This is a hub. It records what is true about **this** repository and links out to
the specialized rules rather than restating them.

## Current state — read this before assuming anything

`MCAgents-Mods/core` is a **Gradle multi project build on Java 25**. The tracked files
are:

* `README.md` — project overview
* `LICENSE` — MIT, `Copyright (c) 2026 MCAgents`
* `settings.gradle`, `build.gradle`, `gradle.properties`, `gradlew`,
  `gradlew.bat`, `gradle/` — the build, the wrapper, and the version catalog
* `api/`, `common/`, `platforms/` — the six modules, all packaged under
  `io.github.mcagentsmods.core`; `platforms/{forge,neoforge,fabric}` each build
  one mod jar for Minecraft 26.2
* `.agents/` — this repository's own instructions (`rules/`, `knowledge/`), plus
  three reserved structural trees: `.agents/index/` (every index), `.agents/wiki/`
  (agent knowledge), and `.agents/memory/` (dynamic state)
* `wiki/` — the human documentation tree, including `wiki/logs/` release history

## Which instruction set this repository is on

This is a **consuming repository**. The universal conventions — branching, commits,
pull requests, task workflow, the directory architecture, versioning, memory policy,
the discovery protocol, no-session-links, and the five creators — are served by the
**`lxagents-agents-base`** MCP connector and are addressed as
`agents://{folder}/{file}.md`, referred to in prose as `{shared}`.

**Nothing shared is copied here.** `.agents/` carries only what is this repository's
own; there is no `git/`, `planning/`, `prompts/`, or `creators/` folder, and creating
one is the duplication this split exists to prevent. A local file whose frontmatter
`name` matches a shared file's `name` would override that shared file whole, and every
such override needs a row with a stated reason in
[`../index/root-index.md`](../index/root-index.md). There are none today.

If the connector cannot be reached, say so plainly and work from this local set alone.
Do not reconstruct the shared rules from memory, and do not clone or paste them in.

The module graph, the dependency rules, and the published coordinates are
documented once, in
[`../../wiki/information/modules.md`](../../wiki/information/modules.md). Do not
restate them here; read that page rather than inferring the layout.

The two workflows are `.github/workflows/publish.yml`, which publishes and tags a
release when a new `project-version` reaches `master`, and
`.github/workflows/release.yml`, which then attaches the Forge, Fabric and NeoForge
jars to that tag's GitHub release. **Nothing runs on a pull request.** Do not describe
a check that does not exist, and do not write instructions that assume one. Anything
you would have to guess at is not a rule — it is a fabrication.

## What the project does

`core` holds the shared agent and API code that MCAgents' Minecraft mods build
on. Today that is one capability: driving language model agents —
OpenRouter, OpenAI, DeepSeek, and Anthropic — through two public entry points:
`io.github.mcagentsmods.core.common.MCAgentsProvider`, one client per vendor, and
`io.github.mcagentsmods.core.common.model.OpenRouterClient`, the routed client with
tool calling, embeddings, prompt caching, and retries.

Three constraints define the project, and a change that breaks one of them is
wrong regardless of how well it works:

* **API only.** No gameplay: no items, blocks, gameplay listeners, or
  permissions. `core` ships as one mod jar per loader so that every MCAgents mod
  calls the same copy of it, but the jar adds nothing to the game — it is the
  foundation other mods are built on, never a feature a player uses directly.
* **No memory.** Nothing stores a conversation, a cache, or per player state. A
  shared core that accumulated history would leak memory in every consumer at
  once.
* **Nothing blocks.** Every remote call returns a `CompletableFuture` and works
  off the calling thread. A tick or a rendered frame must never wait on a network
  call.

The loader modules hold only an entry point and a manifest each; everything they
start lives in `platforms/shared`, through `CoreRuntime`.

## Rules

* **Do not fabricate architecture.** `wiki/` describes the repository as it is.
  No speculative architecture pages, no placeholder API reference, no TODO-filled
  documents, and no page describing a module that holds no code.
* **Record commands only once they are real.** The build, test, and publish
  commands live in
  [`../../wiki/environments/setup.md`](../../wiki/environments/setup.md). Never
  write a command you have not seen work in this repository.
* **A change carries its documentation with it** — see
  `{shared}/rules/change-propagation.md`. Code and structure changes update the
  `wiki/` pages and indexes they invalidate, in the same commit.
* **The default branch is `master`.** Branch from it, never commit to it directly —
  see `{shared}/git/branching-strategy.md`.
* **The license is fixed.** MIT, held by MCAgents. Do not change the license, the
  copyright holder, or the year without explicit user instruction — that is a
  legal statement, not a code change.
* **Never bump the version yourself** — see `{shared}/rules/versioning.md`. The
  version carrier is `project-version` in `gradle.properties`, currently `0.12.0`.
  `wiki/logs/` carries `0/1/0` through `0/8/0`; creating another version directory
  requires user approval, exactly like editing that property.
* **Never create an `INDEX.md`.** Every index is a file in `.agents/index/`, named
  `{scope}-index.md`. This is absolute — see `{shared}/rules/directories.md`.
* **Write memory as you work.** Finishing a meaningful unit of work without recording
  it under `.agents/memory/` is an incomplete task, and needs no approval — see
  `{shared}/rules/memory-policy.md`.
* **Placement is not a judgment call.** New instructions and documents go where
  `{shared}/rules/directories.md` says, including creating a new folder when
  none fits.
* **Keep `README.md` and `AGENTS.md` overviews.** Detail belongs in `wiki/`; rules
  belong in `.agents/{folder}/`. If detail creeps into either overview, move it down
  rather than leaving it. `AGENTS.md` carries the auto-activation contract and the
  trigger table, never a rule body.

## When this file goes stale

This file's "Current state" and "What the project does" sections describe the
repository at a moment in time. The moment a CI workflow lands, a `platforms/*`
module gains real code, or one of the three constraints above is deliberately
changed, they are wrong. The live version of that snapshot is
[`../memory/state/repository-state.md`](../memory/state/repository-state.md), which is
updated freely; this file changes only by proposal. Correcting them is part of the change that caused it —
subject to the Discovery Protocol below, propose the rewrite rather than silently
reshaping the rules.

## Changing this rule

This file is an instruction, so it is **not yours to edit on your own initiative** — even
when you are confident it is wrong. Collect the finding and propose it, per
`{shared}/rules/discovery-protocol.md`.
