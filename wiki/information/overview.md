# Overview

`core` is the shared foundation repository for **MCAgents-Mods**. It holds the
agent and API code that MCAgents' Minecraft mods build on, so that behavior
common to several mods lives in one place instead of being copied between them.

What that means today is one capability: driving language model agents —
OpenRouter, OpenAI, DeepSeek, and Anthropic — from a mod, either through
`MCAgentsProvider` or through `OpenRouterClient`, the routed client with tool
calling, embeddings, prompt caching and retries. It also runs the local web
console every mod registers its pages into. The core is API only. It has no commands, no permissions, and no
memory: it stores no conversation and no per player state, so a consumer keeps
whatever history it wants and replays it.

## Current state

The `api` and `common` modules carry the agent API and its implementation.
`platforms/shared` holds what every loader shares, including the two physical
sides, and `platforms/{forge,neoforge,fabric}` each build one mod jar for
Minecraft 26.2. What exists today is:

| Path | What it is |
|---|---|
| `README.md` | Project overview and quick start. |
| `AGENTS.md` | Entry point for agents: the auto-activation contract, the trigger table, and the reading order. |
| `LICENSE` | MIT license, `Copyright (c) 2026 MCAgents`. |
| `settings.gradle`, `build.gradle`, `gradle.properties` | The Gradle multi project build configuration. |
| `gradlew`, `gradlew.bat`, `gradle/` | The Gradle wrapper and the version catalog. |
| `api/`, `common/`, `platforms/` | The six build modules — see [`modules.md`](modules.md). |
| `.agents/` | The agent instruction set — rules, git conventions, planning, platform knowledge, prompts, creators — plus the index, agent-wiki, and memory trees. |
| `wiki/` | This documentation tree. |
| `.github/workflows/publish.yml` | The Publish workflow, which ships each new version from `master`. |

One GitHub Actions workflow, `.github/workflows/publish.yml`, publishes and tags a
release when a new version reaches `master`. Nothing runs on a pull request. Build,
test, and publish commands, and how a release is published, are on
[`../environments/setup.md`](../environments/setup.md).

## How the repository is organized

The project separates three kinds of content, and keeps them strictly apart:

* **Instructions** — rules an agent follows — live under `.agents/`, one topic per
  file, at `.agents/{folder}/{file}.md`.
* **Documentation** — pages a human reads — live under `wiki/`, at
  `wiki/{folder}/{file-name}.md`. This page is one of them.
* **Agent knowledge** — procedures and orientation written for agents rather than
  people — lives under `.agents/wiki/{type}/{file-name}.md`. It links back here for
  the underlying facts instead of repeating them.
* **Agent memory** — task records, decisions, and the current state of the
  repository — lives under `.agents/memory/{type}/{file-name}.md`.
* **Overviews** — `README.md` and `AGENTS.md` — stay short and point elsewhere.
  Detail is never left in them; it is moved down into `wiki/`.

Navigation runs through a centralized index tree. Every index is a file in
`.agents/index/`: the root router lists the indexes, and each index lists what its own
scope contains. There is no `INDEX.md` anywhere in the repository. The point is that an
agent can find the one file it needs by reading a few small tables, rather than loading
the whole repository. The map of this wiki is
[`../../.agents/index/project-wiki-index.md`](../../.agents/index/project-wiki-index.md).

## Change logs

Released versions are recorded under `wiki/logs/{Major}/{Minor}/{Patch}/`. The
current version is `0.5.0` — see
[`../../.agents/index/logs-index.md`](../../.agents/index/logs-index.md).
