# Mod Sides

How `core` runs on a client and on a dedicated server from one loader jar,
without either side loading the other's code.

## The problem this solves

A mod runs in two physically different processes:

| | Client | Dedicated server |
|---|---|---|
| Window, keyboard, local player | Yes | No |
| Client classes on the classpath | Yes | **No** |
| Hosts a world | Sometimes (single player) | Always |
| Who owns the API credentials | The player | The server owner |
| Who may be trusted | The one person present | Nobody, by default |

The third row is the one that bites. Client classes are not merely unused on a
dedicated server — they are **absent from the distribution**. Touching one there
is not a logic error you can catch and log; it is a `NoClassDefFoundError` while
the class is being linked, and it takes the server down at whatever moment the
code path is first reached. A green build proves nothing about it.

The last two rows are why the split is not only about crashes. On a client, "may
I add a credential?" has no adversary — the player owns the machine and pays the
bill. On a server the same question is the only thing between a player and
someone else's billing account.

## The shape

```
platforms/shared              io.github.mcagentsmods.core.shared
 ├── …shared.client               client only
 ├── …shared.server               server only
 └── everything else              both sides, no side-specific assumptions
platforms/client              io.github.mcagentsmods.core.client     client only, as a whole
platforms/forge               Forge entry point
platforms/neoforge            NeoForge entry point
platforms/fabric              Fabric entry points
```

The `client` and `server` packages **never import each other**, and nothing
else in `platforms:shared` imports either of them. That is not a convention —
`SideSeparationTest` fails the build when it stops being true.

## Three parts

### 1. Which side is this?

`ModEnvironment` answers, from three sources, most authoritative first:

1. **What the loader said.** `ModEnvironment.install(side)` — a loader is told
   its distribution before it loads a single mod class, so this is never a
   guess. A loader entry point calls it first, before anything else runs.
2. **The `mcagents.side` system property.** The escape hatch. Accepts `client`,
   `dedicated_server`, or `server`. An unreadable value is ignored rather than
   fatal.
3. **A classpath probe** for `net.minecraft.client.main.Main`, which exists in
   every client distribution and no dedicated server one.

The probe is last because it is a guess about someone else's jar. It fails
towards `DEDICATED_SERVER`: an unrecognised classpath reads as a server, so the
client half is never started where its classes might not exist.

### 2. Starting one half without linking the other

`ModBootstrap` holds each entry point as a **string**, not as a type:

```java
ModEnvironment.install(PhysicalSide.CLIENT);
SideEntrypoint running = ModBootstrap.start(
        ModContext.of(PhysicalSide.CLIENT, gameDirectory, logger));
```

It resolves only the name matching the side actually running. A dedicated server
therefore never loads, links, or verifies a client class — not because the class
was stripped from the jar, but because nothing ever asked for it. Both halves
travel in each loader's jar and only one is ever touched.

Shared code imports neither half, which is what keeps that true: if it did,
someone could write `new ClientEntrypoint()` there and the compiler would allow
it. `SideSeparationTest` is what stops that.

Each loader's entry point does exactly this through `CoreRuntime.start(side,
gameDirectory)`, with the side the loader itself reported, and calls
`CoreRuntime.stop()` when the game shuts down.

`platforms:client` is kept off the server the loader's way instead, because it
is wired to the loader's own client events rather than started by name. Fabric
loads `CoreFabricClient` only as a `client` entry point, NeoForge constructs
`CoreNeoForgeClient` only for `Dist.CLIENT`, and `CoreForgeMod` calls
`CoreForgeClient.start()` only when Forge reports the client. Forge's mixin,
which draws the hologram, is listed under `client` in its configuration, so Mixin
skips it on a server. Those four classes are the only ones that name
`platforms:client`, so on a dedicated server nothing asks for it.

### 3. Failing legibly when the wiring is wrong

`SideGuard.requireClient(...)` at the top of a client-only constructor turns a
mistake into a `WrongSideException` naming the feature and both sides, instead
of a `NoClassDefFoundError` several frames later naming a Minecraft class nobody
here wrote.

This is a diagnostic, not a security boundary. The boundary is part 2.

## The markers

`@ClientOnly` and `@ServerOnly` are the loader-neutral spelling of Fabric's
`@Environment` and Forge's and NeoForge's `@OnlyIn`. Those cannot be used in
`platforms:shared`, which compiles without a loader toolchain so it stays plain
Java — and testable without a game.

Marking a class does not make it safe. Putting it in the right package and never
linking it does.

## The asymmetry, which is deliberate

`@ClientOnly` means the class does not exist elsewhere. `@ServerOnly` cannot
mean that, because **server classes do exist on a client** — a client hosting a
single player world runs server logic. So:

* `SideGuard.requireClient` is the check that matters, and the client entry
  point asserts it.
* The server entry point asserts nothing, on purpose. Refusing to construct it
  on a client would break single player for no reason.
* `SideGuard.requireDedicatedServer` exists for the narrow case of something
  that genuinely must not run inside a client process. Reach for it rarely.

## What lives on each side

| | Client | Dedicated server |
|---|---|---|
| Credential file | `MCAgents/core/mcagents.json` under the player's Minecraft directory, **shared** by every MCAgents mod they install | `MCAgents/core/mcagents.json` under the server's run directory |
| Resolved by | `MinecraftDirectory` | `ServerRunDirectory` |
| Who may manage it | The player, unconditionally | Permission level 4 only, checked on every operation |
| Fallback when the loader reports no directory | The conventional Minecraft directory for the operating system | The process working directory — **never** a home directory |

A server reaching into a player's `mcagents.json` would be spending someone
else's money, which is why the two resolvers are separate classes on separate
sides rather than one class with a branch in it.

## Testing it

Both halves are tested in `platforms:shared`. `SideSeparationTest` reads the
module's sources and fails if a side imports the other, or if shared code imports
either — the claim the split makes, checked rather than asserted.
`ModBootstrapTest` names a class that does not exist to prove a distribution
missing a half fails with a message naming it.

See [`../environments/setup.md`](../environments/setup.md) for how to run them.
