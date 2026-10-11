---
name: memory-state-repository-state
description: Current known state of MCAgents-Mods/core — what exists, the stack, what is not built yet, and the next obvious step.
---

# Repository State

This file is **overwritten in place** and is always current. It is not a log — for
history, read `wiki/logs/`.

## 2026-10-11

**Stack.** Gradle multi project build, Java 25, root project `core`, group
`io.github.mcagentsmods`. Version `0.4.0`, carried by `project-version` in
`gradle.properties`. Default branch `master`. License MIT,
`Copyright (c) 2026 MCAgents`.

**What exists and works.**

- `api` and `common` — the language model API and its implementation.
  `MCAgentsProvider` is the single public entry point, driving OpenRouter, OpenAI,
  DeepSeek, and Anthropic.
- Credential pooling, rotation, and storage owned entirely by `core`: the shared
  `mcagents.json` and the `/agents` client command logic with masked handles in tab
  completion.
- One request timeout, 60 seconds (`LlmCredentials.DEFAULT_TIMEOUT`). It is the only
  timeout in MCAgents; consumers cannot override it.
- Seven build modules. `platforms/mods/*` covers the two loaders plus the client and
  server physical sides. The Bukkit family and the universal engine jar were removed
  on 2026-10-11 — see [`../tasks/mods-foundation.md`](../tasks/mods-foundation.md).
- A JUnit 5 test harness wired once in the root `build.gradle`. Tests exist for
  `common` token handling and for the mod-side environment, client, and server
  packages.

**What is not built yet.**

- **No CI pipeline.** `./gradlew test` is the only thing that runs the tests.
- **No loader toolchain.** `platforms/mods/neoforge` and `platforms/mods/fabric` are
  plain Java modules; resolving NeoForge or Fabric needs ModDevGradle or Loom, which
  has not landed.
- **No decided Minecraft version range.** Nothing version-specific has been written,
  deliberately.

**Agent system.** This is a **consuming repository**. The universal conventions are
served by the `lxagents-agents-base` MCP connector and addressed as
`agents://{folder}/{file}.md`; `AGENTS.md` carries the bootstrap block that resolves
them. `.agents/` holds only what is this repository's own — `rules/repository.md` and
`knowledge/minecraft-platform.md` — plus the three reserved trees: `.agents/index/`
(all six indexes), `.agents/wiki/` (agent knowledge), and `.agents/memory/` (dynamic
state). `rules/change-propagation.md` was here until the shared set grew its own; the
trigger row now points at `{shared}/rules/change-propagation.md`.

There is no `git/`, `planning/`, `prompts/`, or `creators/` folder; sixteen local
copies of shared files were removed, and the override table in `root-index.md` is
empty, meaning nothing here overrides the shared set. No `INDEX.md` exists anywhere
in the repository. See
[`../tasks/agents-instruction-rewrite.md`](../tasks/agents-instruction-rewrite.md),
and [`../tasks/agents-setup.md`](../tasks/agents-setup.md) for the generation that
preceded it.

**Next obvious step.** Either a CI workflow — which would make `./gradlew test` run
on every pull request and let the "no CI" caveat come out of three separate files —
or the first real loader code, which brings its toolchain with it. Neither is
started; both are the user's call.
