---
name: minecraft-platform-knowledge
description: Platform targets — the three mod loaders core builds for, Minecraft 26.2, the two physical sides, and the threading constraints shared code must honour.
---

# Minecraft Platform

## Supported platforms

`core` is consumed by **mods only**. The organization holds Minecraft mods, so server
plugin platforms — Bukkit, Spigot, Paper, Folia — are out of scope, and so are Sponge
and the proxies (Velocity, BungeeCord). Do not write code, documentation, or
instructions targeting them until that decision changes.

| Loader | What it is | Module |
|---|---|---|
| **Forge** | The original Forge loader, MinecraftForge. | `platforms:forge` |
| **NeoForge** | A Forge fork with its own API and event bus. | `platforms:neoforge` |
| **Fabric** | A lightweight loader with its own API. | `platforms:fabric` |

`platforms:shared` holds what the loaders share. **Each loader builds its own jar**
and the jars are never merged: a loader never loads another loader's classes.

**Minecraft 26.2** is the one version every loader targets — decided with the owner on
2026-10-11, because `MCAgents-Mods/chat` already targets it. Minecraft 26.x ships
unobfuscated, so there is no mappings coordinate. Moving the version means moving all
three loaders at once, and only after each loader's maven publishes a build for it.

## Two physical sides

A mod runs on a **client** (a window and a player) and on a **dedicated server**
(neither). Client classes are absent from a dedicated server's distribution, so client
code reached there is a `NoClassDefFoundError`, not a logic error — and a green build
proves nothing about it. Client-only code never sits where a dedicated server can reach
it. See [`../../wiki/information/mod-sides.md`](../../wiki/information/mod-sides.md).

## Threading — the constraint that actually matters

* **The game thread renders or ticks the game. Blocking it freezes the game.** Every
  remote call in `core` returns a `CompletableFuture` and does its work off the calling
  thread.
* **A future completes on whichever thread finished the work.** A callback that touches
  the world, an entity, a screen, or an inventory must hop back onto the client or
  server thread first.
* **Static mutable state and shared collections** may be reached from several threads at
  once. Unsynchronized shared state is a data race, not a style preference.

## What this means for `core`

`core` is a shared foundation consumed by several mods, so a mistake here propagates to
every consumer:

* **Write it in `api` or `common` by default**, where it names no loader at all and
  every loader gets it.
* **Never assume which thread a caller is on**, and never block the one it is on.
* **Isolate and clearly mark loader-specific code**, so a consumer can tell what runs
  where without reading the implementation.
* **Thread-safety is part of `core`'s public contract.** For anything `core` exposes,
  document which thread it may be called from.

## Dependency coordinates

Every coordinate is declared once, in `gradle/libs.versions.toml`, and never inline in
a module.

| Catalog entry | Toolchain | Module |
|---|---|---|
| `forge`, `forgegradle` | ForgeGradle 7 | `platforms:forge` |
| `neoforge`, `moddevgradle` | ModDevGradle | `platforms:neoforge` |
| `minecraft`, `fabric-loader`, `fabric-api`, `fabric-loom` | Fabric Loom | `platforms:fabric` |

Loader types are referenced **only** in the module for that loader. `api`, `common` and
`platforms:shared` use no toolchain and must never name a loader or Minecraft type.

## Changing this rule

This file is an instruction, so it is **not yours to edit on your own initiative** — even
when you are confident it is wrong. Collect the finding and propose it, per
`{shared}/rules/discovery-protocol.md`.
