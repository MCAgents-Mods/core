# Key File

Every MCAgents mod on a client reads its API keys from one file, `mcagents.json` in the
player's Minecraft directory, so a player adds a key once and every mod finds it. A dedicated
server keeps its own file, `config/mcagents.json` under its run directory. `SharedTokenStore`
reads and writes either; it is a [`TokenStore`](tokens.md), so registering it with the
provider hands the keys to core.

```java
SharedTokenStore store = new SharedTokenStore(gameDirectory, logger);
MCAgentsProvider.instance.registerStore(LlmVendor.OPENROUTER, store);
```

The types are in `core-shared`: `…shared.store` for the file and the client's
directory, `…shared.server.ServerRunDirectory` for the server's.

## The file

```json
{
  "openrouter": { "token": ["sk-or-v1-..."] },
  "openai":     { "token": [] },
  "deepseek":   { "token": [] },
  "anthropic":  { "token": [] }
}
```

One section per vendor, named by `LlmVendor.code()`, each holding a list of keys tried in
order. A missing file is created with an empty section per vendor. A single key written
without the brackets is accepted. A blank entry is skipped as a placeholder; an entry with a
space or an unusual character is skipped with one warning.

## Where it is

| Side | File | Resolved by |
|---|---|---|
| Client | `mcagents.json` in the `MCAGENTS_DIR` environment variable's directory when set, else the game directory the loader reported, else the operating system's Minecraft directory | `MinecraftDirectory.resolveFile(loaderDirectory)` |
| Dedicated server | `config/mcagents.json` under the run directory the loader reported, else under the working directory, never a home directory | `ServerRunDirectory.resolveFile(runDirectory)` |

The operating system's Minecraft directory is `%APPDATA%\.minecraft` on Windows,
`~/Library/Application Support/minecraft` on macOS, and `~/.minecraft` elsewhere.
`MinecraftDirectory.isUsable(directory)` says whether a directory exists and can be written,
or can be created.

A server never reads a player's file: it is a different machine, owner and bill.

## SharedTokenStore

| Member | Meaning |
|---|---|
| `SharedTokenStore(Path loaderDirectory, Logger)` | Opens the client's shared file, resolved as above. Creates it when missing. |
| `atFile(Path file, Logger)` | Opens exactly this file, ignoring `MCAGENTS_DIR`. The server uses it. |
| `file()` | The path, for a message. |
| `load`, `add`, `evict`, `reload`, `describe` | The `TokenStore` contract; see [`tokens.md`](tokens.md). `describe()` is the path. |

## Safe to share

Several mods may hold the file open at once, so:

- every write goes to a temporary file beside it and is moved into place atomically;
- every write re-reads first, so a key another mod added is not lost;
- one lock per file is shared by every store in the process, so a single-player game's client
  and server stores cannot interleave;
- a file that cannot be read is never written. The keys stay on disk until the player fixes
  the file, and the parser's error text, which may quote a key, is not logged;
- where the file system supports POSIX permissions the file is made readable by its owner
  only.

Every method touches disk. Call them off the render thread.
