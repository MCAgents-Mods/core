---
name: memory-tasks-mods-foundation
description: Turning core into the foundation for MCAgents-Mods — Bukkit removed, the io.github.mcagentsmods namespace, one jar per mod loader, the shared language model client, and the web page registry.
---

# Task: core becomes the mods foundation

**Status:** done — pull requests #1 to #8 merged in order by rebase

## The request

The owner moved this repository into the `MCAgents-Mods` organization, which holds
Minecraft mods only, and asked for four things across `core` and `chat`:

1. Delete every Bukkit project and lay core out as `api/`, `common/` and
   `platforms/{shared, forge, neoforge}`, where `shared` holds the code the loaders share
   and **each loader builds its own jar**. Packages become
   `io.github.mcagentsmods.core.{api, common, shared, forge, neoforge}`.
2. Make `chat` use core's code to talk to OpenRouter, so a later mod does not write the
   same client again.
3. Move the client web console's server out of `chat` into core; `chat` registers its
   pages into the core mod instead, so any mod can add pages.
4. A security review: memory that is never released, exploits, duplicate code.

## Decisions taken with the owner, 2026-10-11

- **Fabric stays.** `chat` is a Fabric-only mod, so core also builds
  `platforms/fabric` — without a Fabric jar `chat` has no core to call or register into.
  The owner chose this over porting `chat` or bundling core inside it.
- **Minecraft 26.2**, the version `chat` already targets. It settles the version
  question `knowledge/minecraft-platform.md` left open.
- **Gradle identity:** group `io.github.mcagentsmods`, root project `core`, published
  artifacts `core-api` and `core-common`, `git-org-name=MCAgents-Mods`.
- **Mod id `mcagents_core`.** `chat` keeps `mcagents`.
- **Version 0.5.0** for this release, approved by the owner.
- **`.agents/plans/` is ignored** from task 1, as `chat` already does; approved with the
  plan.

## Tasks

| # | Title | Scope | Branch | PR |
|---|---|---|---|---|
| 1 | Task record | This file, its index row, the `.agents/plans/` ignore | `chore/mods-foundation-plan` | #1 |
| 2 | Remove Bukkit | `platforms/bukkit/*`, `platforms/engine`, the Spigot, Paper and Folia coordinates, and the docs describing them | `refactor/bukkit-removal` | #2 |
| 3 | New namespace | `io.github.mcagents.core` → `io.github.mcagentsmods.core`, group, root name, org property | `refactor/core-namespace` | #3 |
| 4 | Platform layout | `platforms/{shared,forge,neoforge,fabric}`, loader toolchains, one jar per loader | `refactor/platform-layout` | #4 |
| 5 | Shared language model client | Tool calls, embeddings, prompt-cache hints, retries, provider routes, one HTTP transport | `feat/shared-llm-client` | #5 |
| 6 | Web registry | The loopback web host and `WebRegistry`, the shared page shell | `feat/web-registry` | #6 |
| 7 | Security review | Fixes for confirmed findings | `fix/security-review` | #7 |
| 8 | Release | `0.5.0`, changelog, indexes, closing this record | `release/0.5.0` | #8 |

`chat` carries the other half under the same branch names for tasks 5–7, and its pull
requests merge after this repository's.

### Task 1 — chore/mods-foundation-plan

Created this record and its row in `memory-index.md`, and added `.agents/plans/` to
`.gitignore` so the working plan can never be committed. Baseline before any change:
`./gradlew build` passes on `master` at `f8f7eef`.

### Task 2 — refactor/bukkit-removal

Deleted `platforms/bukkit/{core,spigotmc,papermc,foliamc}` and `platforms/engine` —
the Bukkit plugins, `YamlTokenStore`, `CoreCommand`, every `plugin.yml` and
`config.yml`, and the shaded universal jar — along with the Spigot, Paper and Folia
coordinates. The per-family group qualification in the root `build.gradle` went with
them: it existed only because two families each had a module named `core`.

Docs and instructions now describe a mods-only repository: `README.md`, `AGENTS.md`,
the overview, modules, setup, mod-sides, provider-guide and API pages, the repository
map, `rules/repository.md`, `knowledge/minecraft-platform.md` (Bukkit and Folia
removed; threading restated for the client and server threads), and the state file.
The request timeout section now names `LlmCredentials.DEFAULT_TIMEOUT`, the only
timeout left — `request_timeout_seconds` was read by the deleted Bukkit config only.
Released changelogs under `wiki/logs/` are history and were not touched.

`./gradlew build` passes with seven modules.

### Task 3 — refactor/core-namespace

Every package moved from `io.github.mcagents.core` to `io.github.mcagentsmods.core`,
directories with them. Gradle identity: `project-group=io.github.mcagentsmods`,
`git-org-name=MCAgents-Mods`, root project `core`, so the jars and the published
artifacts are `core-api` and `core-common`. The POM's project URL now points at the
GitHub repository instead of the old website.

Runtime names a player can see were deliberately left alone: the shared credential file
`mcagents.json` and the `mcagents.side` system property. Released changelogs and older
task records keep the old names as history. The mod packages still sit under
`…core.mods`; task 4 moves them into `…core.shared`.

### Task 4 — refactor/platform-layout

`platforms/mods/{core,client,server}` became one module, `platforms/shared`, package
`…core.shared`, with the two physical sides as the `…shared.client` and
`…shared.server` packages. The guarantee the old module boundary gave — neither half
links the other — is now `SideSeparationTest`, which reads the module's sources and
fails if a side imports the other or if shared code imports either. `ModBootstrap`
gained a package-private `load(side, className)` so the missing-half failure is still
tested now that both halves are always on the test classpath.

`CoreRuntime` in `platforms/shared` is the one start and stop every loader calls, so the
three entry points stay a few lines each.

Three loader modules, each with its own toolchain and its own jar carrying `api`,
`common` and `platforms/shared`:

| Module | Toolchain | Loader | Entry point | Shutdown hook |
|---|---|---|---|---|
| `platforms/forge` | ForgeGradle 7.0.40 | Forge 26.2-65.1.3 | `CoreForgeMod` | `GameShuttingDownEvent` |
| `platforms/neoforge` | ModDevGradle 2.0.148 | NeoForge 26.2.0.90 | `CoreNeoForgeMod` | `GameShuttingDownEvent` |
| `platforms/fabric` | Loom 1.17.19 | Fabric 0.19.3, API 0.157.0+26.2 | `CoreFabricMod` (server), `CoreFabricClient` (client) | `SERVER_STOPPED`, `CLIENT_STOPPING` |

All three declare mod id `mcagents_core` and Minecraft `[26.2,26.3)`. Minecraft 26.2 is
recorded in `knowledge/minecraft-platform.md`; the "no decided version" note is gone.
`run/` and `run-data/` are ignored for the loaders' dev runs.

**Not proven:** none of the three jars has been loaded in a real game. They compile
against each loader's real API and their manifests name the classes that exist, nothing
more. The `/agents` client commands are still not wired into a command tree.

`./gradlew build` passes: 82 tests, all three mod jars produced.

### Task 5 — feat/shared-llm-client

`MCAgents-Mods/chat`'s model client moved into core, so chat and any later mod call the
same code:

| Where | What |
|---|---|
| `api.model` | `ModelClient`, `ChatRequest`, `ChatCompletion`, `PromptMessage`, `ToolDefinition`, `ToolCall`, `TokenUsage`, `ApiKeySource`, `ProviderRoute`, `ProviderRoutes`, `ModelException`, `ModelName`, `ModelPlatform` |
| `api.http` | `HttpTransport`, `HttpCall`, `HttpReply` |
| `common.model` | `OpenRouterClient` and its package-private `OpenRouterPayloads` |
| `common.http` | `JdkHttpTransport` |
| `common` test fixtures | `StubTransport`, `StubKeySource`, published as `core-common`'s test-fixtures variant |

Decisions:

- **One `ChatRole`.** `api.chat.ChatRole` gained `TOOL` rather than core carrying a second
  role enum. `ChatMessage` refuses `TOOL`, because it cannot name the call a result
  answers and every vendor rejects a tool turn without one. Chat switches from
  `wireName()` to `code()`.
- **Attribution is the caller's.** `OpenRouterClient.Attribution` carries `HTTP-Referer`
  and `X-Title`; the default names core, `NONE` sends neither, and a control character is
  refused so a name cannot inject a header. Chat passes its own.
- **Keys stay with the mod.** Core never stores a key for this client; the mod supplies an
  `ApiKeySource`.
- `ModelPlatform.baseUrlSettingKey` came along unchanged. It is a naming convention, not
  storage, and chat's screens and website already use it.

Chat's tests for these behaviours came with the code: client, direct routes, tool payloads,
and the `ModelName` and `ModelPlatform` half of chat's naming test. The `ModelCatalog` and
`ModelPreferences` half stays in chat with those classes. `AttributionTest` and
`ExchangeShapeTest` are new.

**Left for task 7**, found while porting:

- Core now has two request shapes: `api.chat` with `MCAgentsProvider` and `LlmClient`, and
  `api.model` with `OpenRouterClient`. `TokenUsage` and `ChatRequest` exist in both, and
  `LlmVendor` and `ModelPlatform` both list the OpenAI-dialect base URLs.
- `ModelPlatform.isUsableBaseUrl` matches by prefix, so `http://localhost.example.com`
  passes as loopback and would receive a key in clear text.
- `JdkHttpTransport` follows redirects, which can carry the `Authorization` header to
  another host.
- `close()` shuts the retry scheduler down with `shutdownNow()`, so a request waiting on a
  backoff never completes.
- A response whose `choices[0]` or `message` is not a JSON object throws
  `IllegalStateException`, which is classed as `TRANSPORT` and retried, billing the request
  again.

`./gradlew build` passes: 136 tests, all three mod jars produced, and each jar carries the
new packages but not the fixtures.

### Task 6 — feat/web-registry

Chat's `LocalWebConsole` became core's console, generalised from one mod's pages to every
mod's:

| Where | What |
|---|---|
| `api.web` | `WebRegistry`, `WebModule`, `NavLink`, `WebRequest`, `WebResponse`, `WebConsole`, `WebConsoleException` |
| `common.web` | `CoreWebRegistry` (public, `shared()`), and package-private `LocalWebHost`, `ShellModule`, `WebResources` |
| `common/src/main/resources/mcagents_core/web/` | The shell moved from chat: `main.css`, `layout.css`, `components.css`, `site.js`, the header and footer partials; plus core's own front page and `index.js` |

Decisions:

- **Paths are namespaced per module**: `/{id}/…` pages, `/assets/{id}/…` assets,
  `/api/{id}/…` handlers. Core's shell is itself a module under the reserved id `core`, so
  it routes the same way and no mod can take its paths. `api` and `assets` are reserved too.
- **The console starts on demand**, on the first `open()`, not when core loads. A port is
  opened only when a player asks for the console, which is how chat already behaved. Core
  closes it in `CoreRuntime.stop()`.
- **The header is built from the registry.** `GET /api/core/navigation` lists every module
  and its links; `site.js` builds the header from it with `textContent`, one group per mod
  when there is more than one.
- **A page names itself on `<body>`** (`data-module`, `data-section`) instead of setting
  `window.SITE_ROOT` and `window.PAGE_SECTION` in an inline script, because the console now
  sends a `Content-Security-Policy` with `script-src 'self'`. Chat's pages change to match.
- Kept from chat: loopback bind, the 256-bit per-run token in the URL or
  `X-MCAgents-Token`, the `Host` check, no CORS, constant-time token comparison, four
  daemon workers, and the character whitelist on asset paths.
- Changed from chat while moving it: a body over 256 KiB is refused with a 413 instead of
  silently cut short; the `Host` check no longer accepts junk after a port (`127.0.0.1:80:x`)
  or a name after `[::1]`; `.class` files are never served as assets; the API path is
  decoded once, where chat's routes decoded it twice; and every answer adds
  `X-Frame-Options`, `Cross-Origin-Resource-Policy` and the content security policy.

**Not proven:** `com.sun.net.httpserver` and `java.net.http` are in the JDK, and chat uses
both in game on Fabric, but neither has been checked on Forge or NeoForge, whose module
layers may not resolve `jdk.httpserver` or `java.net.http` unless something requires them.
That needs a real launch, with the rest of the "never launched" list from Task 4.

`./gradlew build` passes: 162 tests, with `LocalWebHostTest` and `CoreWebRegistryTest` new, and all
three jars carry `common.web` and the shell resources.

### Task 7 — fix/security-review

A review of core for leaked memory, exploits and duplicate code, after tasks 5 and 6
brought chat's code in. Fixed here:

| Finding | Fix |
|---|---|
| `TokenPool.reject` and `rotate` acted on whichever key was first, so a request that finished late could evict a key it never used | Both take the key the request used and act on that key only; the provider passes it, and swaps the client only by compare-and-replace (`ClientRegistry.replace`) |
| Store and client construction ran inside the pool and registry locks | Both moved outside the locks |
| A 403 evicted the key, though providers send it for a refused prompt or a region | `ModelException.Kind.forStatus`: 401 and 402 are a dead key, 403 is `REQUEST`; `AgentException.isAuthFailure()` is 401 only |
| `isUsableBaseUrl` matched by prefix, so `http://localhost.example.com` passed | Parsed as a URI; http only for exactly `localhost`, `127.0.0.1` or `[::1]`; no userinfo, query or fragment; `LlmCredentials` enforces it |
| Both HTTP clients followed redirects with `Authorization` | `Redirect.NEVER` |
| No cap on an answer's size and no deadline on its body | `HttpLimits`: 8 MiB (routed client) and 16 MiB (`HttpLlmClient`) caps, and a deadline over the whole exchange |
| `close()` left requests waiting on a backoff hanging, and `HttpClient.close()` blocks | Waiters fail with the new `Kind.CLOSED`; the transport uses `shutdownNow()` |
| An answer of the wrong shape was retried as `TRANSPORT`, billing it again | Read failures are `MALFORMED_RESPONSE` |
| A refused header could quote the key in an exception | Keys must be printable ASCII without spaces (`LlmCredentials.isWellFormedKey`), checked by `LlmCredentials`, the key file and the commands; a synchronous `send` failure is replaced by a fixed message with no cause; `HttpCall.toString()` names headers only; `HttpLlmClient` errors are truncated and use fixed text |
| The token commands echoed what was typed, which may be a key | They never repeat input |
| A key file that failed to parse could be overwritten, and the parser's text was logged | Nothing is written while the last read failed; only the exception type is logged |
| Two stores on one file in one process (single player) could interleave | One lock per file across the process; the move is `ATOMIC_MOVE` where supported |
| The server's store followed the client's `MCAGENTS_DIR` override | `SharedTokenStore.atFile` opens exactly the file the server logs |
| `ApiKeySource.next(platform)` handed gateway keys to any platform by default | The default answers only for `openrouter` |
| The web console's fixed pool had an unbounded queue any local process could fill | 64 queued requests, then the connection is closed |
| `LlmVendor` and `ModelPlatform` each listed the base URLs; `directBaseUrls` lost its order | `LlmVendor` reads `ModelPlatform`; the key file's template reads `LlmVendor`; the map keeps its order |

**Deferred, reported to owen:**

- **D1–D5, two request stacks.** `MCAgentsProvider`/`HttpLlmClient` and `OpenRouterClient`
  still each have their own request records, JSON handling and transport. Folding the
  provider's OpenAI-dialect vendors onto `OpenRouterClient`, `TokenPool` onto
  `ApiKeySource`, and `HttpLlmClient` onto `HttpTransport` is an API change for the next
  minor version, not a fix.
- **`ping` downloads the whole model catalogue** to test a key.
- **A cross-process lock on `mcagents.json`.** Two game instances sharing one file can still
  interleave a read and a write; the in-process lock and atomic move do not cover that.
- **The web console's 400** passes a handler's `IllegalArgumentException` text to the page,
  which is the token holder; a JDK exception thrown inside a handler can reach it too.

Chat's switches over `ModelException.Kind` need a `CLOSED` case.

`./gradlew build` passes: 179 tests, with `TokenPoolTest`, `LlmCredentialsTest` and
`SharedTokenStoreTest` new.

### Task 8 — release/0.5.0

`project-version` is `0.5.0`, as approved. The changelog is
`wiki/logs/0/5/0/CHANGELOG.md`, with an Upgrading section, since every consumer has to
change its imports and dependency, and its row is in `logs-index.md`. There were no session
files under `.agents/memory/sessions/` to fold into a digest.

The owner approved opening and merging the pull requests. They were #1 to #8, merged into
`master` in task order by rebase, with the rest of the stack rebased onto `master` after
each merge.
