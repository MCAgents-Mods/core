---
name: memory-state-repository-state
description: Current known state of MCAgents-Mods/core — what exists, the stack, what is not built yet, and the next obvious step.
---

# Repository State

This file is **overwritten in place** and is always current. It is not a log — for
history, read `wiki/logs/`.

## 2026-10-13

**Stack.** Gradle multi project build, Java 25, root project `core`, group
`io.github.mcagentsmods`. Version `0.5.3`, carried by `project-version` in
`gradle.properties`. Default branch `master`. License MIT,
`Copyright (c) 2026 MCAgents`.

**What exists and works.**

- `api` and `common` — the language model API and its implementation, with two
  entry points: `MCAgentsProvider`, one client per vendor for OpenRouter, OpenAI,
  DeepSeek, and Anthropic; and `OpenRouterClient`, the routed client moved from
  `MCAgents-Mods/chat` with tool calling, embeddings, prompt-cache hints, retries,
  key rotation and direct provider routes. `mcagents-core-common` publishes `StubTransport`
  and `StubKeySource` as test fixtures.
- The local web console, in `common.web`: one loopback server, started on demand, that
  every mod registers its pages into through `CoreWebRegistry.shared()`, and the shared
  page shell under `mcagents_core/web/`.
- Holograms, in `platforms/client`: panels a mod opens in the level with
  `Holograms.open`, described by the records in `api.hologram`. The player aims and
  left-clicks, scrolls with the wheel, holds Z to carry one, and clicks its X to close
  it; it also closes beyond two chunks or on a change of dimension. One panel kind so
  far, the button grid. Checked by hand on 2026-10-13 in each loader's dev client, and
  on Forge also from the built jar, where the mixin loads through the manifest.
- Credential pooling, rotation, and storage owned entirely by `core`: the shared
  `mcagents.json` and the `/agents` client command logic with masked handles in tab
  completion. Reviewed for security on 2026-10-11 (task 7 in
  [`../tasks/mods-foundation.md`](../tasks/mods-foundation.md)): only a 401 or 402 evicts a
  key, keys must be printable ASCII, no redirect is followed, answers are size- and
  time-capped, and an unreadable key file is never written.
- Two request timeouts: 60 seconds on the `MCAgentsProvider` path
  (`LlmCredentials.DEFAULT_TIMEOUT`), and 10 seconds to connect and 120 to answer in
  `JdkHttpTransport`, which the routed client uses. Each path also has a deadline over the
  whole exchange, body included.
- Seven build modules: `api`, `common`, `platforms/shared` (the loader-agnostic mod
  code, with the client and server halves as packages), `platforms/client` (the
  holograms, against vanilla Minecraft) and one module per loader —
  `platforms/forge`, `platforms/neoforge`, `platforms/fabric` — each building its own
  mod jar, id `mcagents_core`, for Minecraft 26.2. The Bukkit family and the
  universal engine jar were removed on 2026-10-11 — see [`../tasks/mods-foundation.md`](../tasks/mods-foundation.md).
- A JUnit 5 test harness wired once in the root `build.gradle`. Tests exist for
  the `api.model` records, `common` token handling, the routed model client, and
  `platforms/shared`: the environment, client and
  server packages, `CoreRuntime`, and the side separation check; and `common.hologram`:
  the grid, the two-chunk range and the placement arithmetic.

**What is not built yet.**

- **No pull request checks.** The one workflow, Publish (`.github/workflows/publish.yml`,
  added 2026-10-12), builds, tests, publishes and tags only when a new `project-version`
  reaches `master`. `./gradlew test` is still the only thing that runs the tests before a
  merge. See [`../tasks/publish-workflow.md`](../tasks/publish-workflow.md).
- **Never played by a person.** On 2026-10-13 each loader's development client
  (`runClient`) started with core loaded and a demo hologram was driven by hand, but no
  published jar has been loaded in a player's game, and no dedicated server has been
  started.
- **No mod opens a hologram yet.** Core opens none itself; the agents mod's menu key
  is planned for its 0.2.0.
- **Two request stacks remain**: `MCAgentsProvider` and `OpenRouterClient` each have their
  own records, JSON and transport. Folding them together is deferred to a later minor.
- **The `/agents` client commands are not wired** into any loader's command tree;
  `ModTokenCommands` holds the logic only.

**Agent system.** This is a **consuming repository**. The universal conventions are
served by the `lxagents-agents-base` MCP connector and addressed as
`agents://{folder}/{file}.md`; `AGENTS.md` carries the bootstrap block that resolves
them. `.agents/` holds only what is this repository's own — `rules/repository.md` and
`knowledge/minecraft-platform.md` — plus the three reserved trees: `.agents/index/`
(all seven indexes, `project-wiki-api-index.md` routing `wiki/api/`), `.agents/wiki/` (agent knowledge), and `.agents/memory/` (dynamic
state). `rules/change-propagation.md` was here until the shared set grew its own; the
trigger row now points at `{shared}/rules/change-propagation.md`.

There is no `git/`, `planning/`, `prompts/`, or `creators/` folder; sixteen local
copies of shared files were removed, and the override table in `root-index.md` is
empty, meaning nothing here overrides the shared set. No `INDEX.md` exists anywhere
in the repository. See
[`../tasks/agents-instruction-rewrite.md`](../tasks/agents-instruction-rewrite.md),
and [`../tasks/agents-setup.md`](../tasks/agents-setup.md) for the generation that
preceded it.

**Next obvious step.** The rest of 0.6.0: the chat panels (a list, a transcript and a
text box), then removing the web console — see [`../tasks/hologram.md`](../tasks/hologram.md).
