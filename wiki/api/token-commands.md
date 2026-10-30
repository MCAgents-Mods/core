# Token Commands

`ModTokenCommands` is the logic behind the `/agents` command that manages the [key file](key-file.md):
list each vendor's state, add a key, remove one, and reload. It answers in plain strings that
never contain a key, and it registers the file's keys with `MCAgentsProvider.instance` so they
are used at once. Since 0.15.0 every core loader registers `/agents` on a **dedicated server**
(see [below](#the-agents-command)); on a client no command is registered, and a mod that wants
one wires these methods itself.

```java
ModTokenCommands commands = clientEntrypoint.commands();
reply(commands.add("openrouter", typedKey)); // "Added #1:a3f9 to openrouter. 1 token(s) stored."
commands.status().forEach(line -> reply(line));
```

`ModTokenCommands` is in `io.github.mcagentsmods.core.shared`; the server's checks are in
`…shared.server`.

## ModTokenCommands

| Method | Answers with |
|---|---|
| `status()` | One line naming the file, then one line per vendor: `ready`, `not configured`, or `expired — every token was rejected and removed`, followed by its masked handles. |
| `add(platform, token)` | Stores the key and puts it into use. Refuses an unknown platform or a malformed key without repeating what was typed. |
| `remove(platform, typed)` | Removes the key named by its handle or by its value. |
| `reload()` | Re-reads the file and registers every vendor's keys with `MCAgentsProvider.instance`, reporting how many vendors have a usable key. `MCAgents is not ready.` when no provider exists yet. |
| `suggestFirstArgument()` | `status`, `reload` and every vendor code, for tab completion. |
| `suggestRemovable(platform)` | That vendor's masked handles, for tab completion. |

Constructors: `ModTokenCommands(Path loaderDirectory, Logger)`, the same with a request
timeout, and `ModTokenCommands(SharedTokenStore, Logger, Duration)` over a store already open.
The timeout rides on every registered key; it defaults to 60 seconds.

A **handle** names a stored key without showing it: its position and last four characters,
such as `#2:a3f9`. `TokenHandles` in `io.github.mcagentsmods.core.common` builds and resolves
them. Completing on the keys themselves would put live keys on a player's screen.

## On a client

`ClientEntrypoint.commands()` returns the commands over the player's shared file, and
`credentialFile()` its path; both are set once the client half starts (see
[`mod-runtime.md`](mod-runtime.md)). The player owns the keys, so nothing is checked.
`ClientCredentialDisplay.wrap(lines)` wraps status lines to about 50 characters for the chat
box.

## On a dedicated server

Every operation goes through `ServerEntrypoint`, which checks the caller first and answers
with a refusal instead of running it:

| Method | Action checked |
|---|---|
| `status(CommandCaller)` | `VIEW_STATUS` |
| `addToken(CommandCaller, platform, token)` | `ADD_TOKEN` |
| `removeToken(CommandCaller, platform, handleOrToken)` | `REMOVE_TOKEN` |
| `reload(CommandCaller)` | `RELOAD` |

| Type | Meaning |
|---|---|
| `CommandCaller(uniqueId, name, permissionLevel)` | Who is asking. `console()` is level 4; `player(uuid, name, level)` a player. A negative level reads as 0. |
| `ServerAction` | What they are trying to do. Each value has a `description()` for the refusal. |
| `ServerCommandAuthority` | Who may do it: permission level 4 (`CommandCaller.OWNER_LEVEL`) by default, never below 1. `allows`, `refusalFor` and `require` check a caller. |

Even viewing the status needs level 4: which platforms a server owner pays for is not a
player's business.

## The `/agents` command

`AgentsCommand` in `io.github.mcagentsmods.core.server` (the `platforms/server` module) is
registered by Fabric, NeoForge and Forge on a dedicated server only:

| Command | Runs |
|---|---|
| `/agents` or `/agents status` | `ServerEntrypoint.status` |
| `/agents reload` | `ServerEntrypoint.reload` |
| `/agents {platform} token add {token}` | `ServerEntrypoint.addToken` |
| `/agents {platform} token remove {handle}` | `ServerEntrypoint.removeToken`; tab completion offers the masked handles from `removableHandles`, and only to a caller allowed to remove a key |

It is guarded twice. Brigadier's `requires` hides the whole command from any source below
permission level 4 (`CommandCaller.OWNER_LEVEL`), so a player cannot see, complete or run it;
and each call still goes through `ServerEntrypoint`'s authority check, so a second route into
the same methods is refused the same way. The caller is built from the source the server
authenticated, with its level read from the source's permission set. Every reply goes to the
caller alone and never contains a key. A key typed into chat still passes through the client
and the server log, so prefer the server console for `token add`.
