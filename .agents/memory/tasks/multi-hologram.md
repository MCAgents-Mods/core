---
name: memory-tasks-multi-hologram
description: Letting several holograms stand in the world at once, adding the pick list panel and transcript buttons that chat's session picker needs, and releasing 0.11.0.
---

# Task: several holograms at once

**Status:** done

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
| 1 | Task record | `chore/multi-hologram-plan` | #52 |
| 2 | Several holograms at once | `feat/multiple-holograms` | #53 |
| 3 | Pick list and transcript buttons | `feat/pick-list` | #54 |
| 4 | Release 0.11.0 | `release/0.11.0` | #55 |

### Task 1 — chore/multi-hologram-plan

This record, and its row in the memory index.

### Task 2 — feat/multiple-holograms

`HologramClient` keeps the standing holograms in a list, oldest first, replaced rather than
changed so another thread reads a whole one. Every frame each hologram finds the point the
crosshair meets, and only the nearest keeps it (`aimed`); a click, the mouse wheel and Z go to
that one, and one hologram is carried at a time. Each tick checks every hologram's range and
level on its own. Opening an id that is open closes that one first; past `Holograms.MAX_OPEN`
(8) the oldest closes. `HologramSpace.clearOf` turns a new hologram's spot around the player
(`TURNS`: 0, 40, -40, 80, -80, 120, -120 degrees) until its `HologramFootprint`, a circle of
half its width around its anchor, meets no standing one's.

`Holograms.openId()` became `openIds()`, and `Holograms.replacing(id, action)` lets the first
hologram `action` opens take the place of hologram `id`. The G menu runs every button through
it, so CHAT still replaces the menu while KEYS, MODEL and the other screen buttons leave it
standing. `wiki/api/hologram.md` and `wiki/api/buttons.md` say so; the `Hologram` record no
longer says one hologram is open at a time.

### Task 3 — feat/pick-list

`PickList` and `PickRow` join the panel kinds in `api`. A pick list's title band is a nav bar:
the title on the left, shrunk and then cut to the room left, and up to 2 buttons on the right,
52 pixels wide each, ending 20 pixels short of the panel's right edge so the close button
keeps its place (`ColumnLayout.navButton`, `navTitleRoom`). Its rows are drawn as given; a row
that is not enabled is drawn in grey on a paler band, never lightens, and a click on it does
nothing. `Transcript` gains a `heading` supplier, read every frame and shown in place of the
title while it gives text, and up to 3 buttons under the entries; `Transcript(title, entries)`
still builds one with neither. `ColumnPanelView` shares the band and the footer buttons
between the list, the pick list and the transcript. `wiki/api/hologram-panels.md` describes
both, with an example of a picker that fades the conversations already open.

### Task 4 — release/0.11.0

`project-version` is `0.11.0`, a minor release: new API (`PickList`, `PickRow`, the
`Transcript` buttons and heading, `Holograms.replacing`), and `Holograms.openId()` is
replaced by `openIds()`. `wiki/logs/0/11/0/` says what players and mod authors do and what was
added; the logs index has its row, and the repository rule, map, state and overview name the
version. The `PR` column is filled. In the Fabric, NeoForge and Forge 26.3 dev clients, with
chat's session picker on top, G then CHAT replaced the menu with the session list, NEW CHAT
opened chat holograms beside it, two of them took typed questions, their rows were faded (and,
on Fabric, ignored clicks), and closing one with its X brought its row back. Screenshots are in the
project's shared files under `reports/multi-hologram/`.
