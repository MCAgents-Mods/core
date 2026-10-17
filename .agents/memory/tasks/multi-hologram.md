---
name: memory-tasks-multi-hologram
description: Letting several holograms stand in the world at once, adding the pick list panel and transcript buttons that chat's session picker needs, and releasing 0.11.0.
---

# Task: several holograms at once

**Status:** in progress

## The request

On 2026-10-17 the owner asked: "I want it can spawn multiple hologram at once. Also I want it
can spawn multiple chat hologram at once too. After click chat in GUI then make another GUI to
show all chat session available. And then have button to add button "New Chat" at right as
nav bar. And when click chat make it spawn hologram and just make chat session that was spawn
faded not disappear and can't click." The owner approved the plan on 2026-10-17 and had
already asked for every branch to be opened as a pull request and rebase-merged in order.

Core's part: holograms stop replacing each other, and core gains the panel chat's session
picker is built from. Chat's part (the picker itself, one chat hologram per session) is chat's
own task record, `session-picker.md`, released as chat 0.10.0 against this release.

## Decisions

- **Up to 8 holograms stand at once.** Opening a ninth closes the one opened longest ago, so
  opening never fails and the frame cost stays bounded.
- **An id names one hologram.** Opening an id that is already open moves that hologram in
  front of the player instead of adding a second. `Holograms.openId()` becomes `openIds()`.
- **Each hologram is aimed at, clicked, scrolled, carried with Z, closed with its X and
  dismissed beyond 2 chunks on its own.** The nearest one under the crosshair is the one aimed
  at.
- **A new hologram turns aside when another is in the way:** it opens in front of the player,
  or, when that spot overlaps an open hologram, the spot turns around the player in 40° steps,
  right then left, up to 120°, until it is clear.
- **A new panel kind, `PickList`:** a title bar with up to 2 buttons on its right, rows that
  are each enabled or faded (a faded row ignores clicks), and a scrollbar. `SelectList` keeps
  its meaning.
- **`Transcript` gains a live title and up to 3 buttons under it.** Its existing constructor
  stays.
- **A minor release, 0.11.0:** new API, and `openId()` is replaced.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/multi-hologram-plan` | |
| 2 | Several holograms at once | `feat/multiple-holograms` | |
| 3 | Pick list and transcript buttons | `feat/pick-list` | |
| 4 | Release 0.11.0 | `release/0.11.0` | |

### Task 1 — chore/multi-hologram-plan

This record, and its row in the memory index.
