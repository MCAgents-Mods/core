# core

Shared foundation repository for **MCAgents-Mods**. It holds the agent and API
code that MCAgents' Minecraft mods build on, so that behavior common to several
mods lives in one place instead of being copied between them. Today that means
one thing: driving language model agents — OpenRouter, OpenAI, DeepSeek, and
Anthropic — from a mod, with no network code of its own.

```java
MCAgentsProvider agents = MCAgentsProvider.create();
agents.registerAnthropic(key);

agents.askAnthropic("claude-opus-4", "Name this village in three words.")
      .thenAccept(name -> minecraft.execute(() -> showName(name)));
```

It is API only. No commands, no permissions, and no memory — the core stores no
conversation and no per player state, so a consumer keeps whatever history it
wants and replays it.

## What is here today

- A Gradle multi project build on **Java 25**: `api`, `common`,
  `platforms/shared`, `platforms/client`, and one module per mod loader —
  `platforms/forge`, `platforms/neoforge` and `platforms/fabric` — each building
  its own jar, one per Minecraft version (26.2), all under
  `io.github.mcagentsmods.core`. See
  [`wiki/information/modules.md`](wiki/information/modules.md).
- `MCAgentsProvider` — one registered client per vendor, for a simple question
  and answer — and `OpenRouterClient`, the routed client with tool calling,
  embeddings, prompt caching, retries and key rotation. See
  [`wiki/api/provider.md`](wiki/api/provider.md) and
  [`wiki/api/model-client.md`](wiki/api/model-client.md).
- Holograms: panels a mod opens in the world in front of the player, who aims and
  clicks, holds Z to move them, and leaves them behind beyond two chunks. See
  [`wiki/api/hologram.md`](wiki/api/hologram.md).
- An agent instruction set under `.agents/` — placement rules, git conventions,
  task workflow, and the creator agents that maintain the trees.
- A documentation tree under `wiki/` for people, and an agent-facing knowledge and
  memory tree under `.agents/wiki/` and `.agents/memory/`.
- A centralized index tree in `.agents/index/` so a file can be found by reading a few
  small tables instead of scanning the repository.

## Quick start

```sh
git clone https://github.com/MCAgents-Mods/core.git
cd core
./gradlew build
```

The wrapper downloads Gradle and a Java 25 toolchain on the first run. Full
details: [`wiki/environments/setup.md`](wiki/environments/setup.md).

## Documentation

- [`wiki/information/overview.md`](wiki/information/overview.md) — what this
  repository is and how it is organized.
- [`wiki/guides/llm-providers.md`](wiki/guides/llm-providers.md) — getting from
  an API key to a model's reply.
- [`wiki/api/`](wiki/api/) — the public API, one page per scope, method by method;
  start with [`dependency.md`](wiki/api/dependency.md).
- [`wiki/information/modules.md`](wiki/information/modules.md) — the module
  graph, the dependency rules, and the published artifacts.
- [`wiki/environments/setup.md`](wiki/environments/setup.md) — getting a working
  copy, and building, testing, and publishing it.

The full map of the wiki is
[`.agents/index/project-wiki-index.md`](.agents/index/project-wiki-index.md).

## Working with agents

Start at [`AGENTS.md`](AGENTS.md) — the entry point, the auto-activation contract, and
the trigger table for the instruction set in `.agents/`.

## License

MIT — see [`LICENSE`](LICENSE).
