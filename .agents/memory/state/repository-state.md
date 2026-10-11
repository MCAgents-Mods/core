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

- `api` and `common` — the language model API and its implementation, with two
  entry points: `MCAgentsProvider`, one client per vendor for OpenRouter, OpenAI,
  DeepSeek, and Anthropic; and `OpenRouterClient`, the routed client moved from
  `MCAgents-Mods/chat` with tool calling, embeddings, prompt-cache hints, retries,
  key rotation and direct provider routes. `core-common` publishes `StubTransport`
  and `StubKeySource` as test fixtures.
- Credential pooling, rotation, and storage owned entirely by `core`: the shared
  `mcagents.json` and the `/agents` client command logic with masked handles in tab
  completion.
- Two request timeouts: 60 seconds on the `MCAgentsProvider` path
  (`LlmCredentials.DEFAULT_TIMEOUT`), and 10 seconds to connect and 120 to answer in
  `JdkHttpTransport`, which the routed client uses.
- Six build modules: `api`, `common`, `platforms/shared` (the loader-agnostic mod
  code, with the client and server halves as packages) and one module per loader —
  `platforms/forge`, `platforms/neoforge`, `platforms/fabric` — each building its own
  mod jar, id `mcagents_core`, for Minecraft 26.2. The Bukkit family and the
  universal engine jar were removed on 2026-10-11 — see [`../tasks/mods-foundation.md`](../tasks/mods-foundation.md).
- A JUnit 5 test harness wired once in the root `build.gradle`. Tests exist for
  the `api.model` records, `common` token handling, the routed model client, and
  `platforms/shared`: the environment, client and
  server packages, `CoreRuntime`, and the side separation check.

**What is not built yet.**

- **No CI pipeline.** `./gradlew test` is the only thing that runs the tests.
- **Never launched in a game.** The three jars build and their manifests agree with
  their entry points, but none has been loaded in a real client or server yet.
- **The `/agents` client commands are not wired** into any loader's command tree;
  `ModTokenCommands` holds the logic only.

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
