# 0.5.0

Released: 2026-10-11

Core becomes the foundation for MCAgents mods: Bukkit is gone, each mod loader
builds its own jar, and core now holds the model client and the web console
that every mod shares.

## Upgrading

Every consumer has something to change.

- **Packages moved** from `io.github.mcagents.core` to
  `io.github.mcagentsmods.core`, and the Maven group to `io.github.mcagentsmods`.
  The artifacts are `mcagents-core-api` and `mcagents-core-common`, and every
  module now publishes as `mcagents-core-{module}` with a jar named
  `MCAgentsCore{Module}`. Update every import and dependency coordinate.
- **Bukkit is gone.** There is no Spigot, Paper or Folia plugin and no universal
  jar. A server plugin built on 0.4.0 has nothing to run against; stay on 0.4.0.
- **Depend on the mod id `mcagents_core`** and take the jar for your loader:
  Fabric, Forge or NeoForge, Minecraft 26.2.
- **A 403 no longer retires a key.** `AgentException.isAuthFailure()` is true for
  401 only. If you treated a 403 as a dead key, stop.
- **A switch over `ModelException.Kind` needs a `CLOSED` case.**
- **A key must be printable ASCII with no space.** `LlmCredentials` and the key
  file refuse anything else.

## Added

- **One jar per loader**: `platforms/fabric`, `platforms/forge` and
  `platforms/neoforge`, each bundling `api`, `common` and the loader-agnostic
  `platforms/shared`. `CoreRuntime` is the one start and stop all three call.
- **The routed model client**, moved from `MCAgents-Mods/chat`:
  `api.model.ModelClient` and `common.model.OpenRouterClient`, with tool calls,
  embeddings, prompt-cache hints, retries with backoff, key rotation across up
  to five keys, and direct routes to a platform's own API. Keys stay with the
  mod, through `ApiKeySource`. `OpenRouterClient.Attribution` lets each mod name
  itself to OpenRouter.
- **`api.http`**: `HttpTransport`, `HttpCall` and `HttpReply`, with
  `JdkHttpTransport` as the real transport, and `StubTransport` and
  `StubKeySource` published as `mcagents-core-common` test fixtures.
- **The web console**, moved from chat and opened to every mod: register a
  `WebModule` with `CoreWebRegistry.shared()` and its pages, assets and API
  appear under `/{id}/`, `/assets/{id}/` and `/api/{id}/` behind one token and
  one header. See `wiki/guides/web-pages.md`.
- `ChatRole.TOOL`, and `ModelException.Kind.CLOSED` and `Kind.forStatus`.
- `SharedTokenStore.atFile`, which opens exactly one file with no environment
  override.
- **The Publish workflow**, `.github/workflows/publish.yml`. A push to `master`
  whose `project-version` has no `v{version}` tag yet builds and tests, publishes
  all six modules to GitHub Packages, and only then tags the commit. 0.5.0 is the
  first version it ships; see `wiki/environments/setup.md`.

## Changed

- The namespace, group and artifacts, as in Upgrading.
- `LlmVendor` reads its base URLs from `ModelPlatform`, so the two cannot drift.
- `ModTokenCommands` never repeats what was typed, since it may be a key.

## Removed

- The Bukkit family — `platforms/bukkit/{core,spigotmc,papermc,foliamc}` — and
  the universal engine jar, with `YamlTokenStore`, `CoreCommand`, `plugin.yml`,
  `config.yml` and `request_timeout_seconds`. The request timeout is
  `LlmCredentials.DEFAULT_TIMEOUT`.

## Fixed

- A request that failed late could retire or rotate a key it never used. Both
  now act on the key the request sent, and the client is swapped only if no
  other failing request swapped it first.
- An answer of the wrong shape was retried, billing it again. It now fails as
  `MALFORMED_RESPONSE`.
- Closing the routed client left requests waiting on a backoff hanging forever.
- Closing `JdkHttpTransport` blocked until requests in flight finished.

## Security

- No HTTP client follows a redirect, which would re-send the key to another
  host.
- A plain-HTTP base URL is accepted only for exactly `localhost`, `127.0.0.1` or
  `[::1]`. Before, any address starting with `http://localhost` passed.
- Answers are capped at 8 MiB (routed client) and 16 MiB (`MCAgentsProvider`),
  and each exchange has a deadline that covers the body.
- No exception, log line, command reply or `HttpCall.toString()` repeats a key.
- A key file that fails to parse is never written over, and its parser error is
  not logged. Writes are atomic where the filesystem allows and locked per file
  within the game.
- The server's key file can no longer be redirected by the client's
  `MCAGENTS_DIR` variable.
- `ApiKeySource.next(platform)` no longer hands gateway keys to other platforms
  by default.
- The web console binds to loopback, needs a 256-bit token, checks `Host`, sends
  a content security policy, refuses bodies over 256 KiB, and bounds its request
  queue.

## Not proven

None of the three jars has been loaded in a real game yet, and the web console's
use of `jdk.httpserver` and `java.net.http` has not been checked on Forge or
NeoForge.
