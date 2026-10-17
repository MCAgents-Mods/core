# 0.11.0

Released: 2026-10-17

Several holograms can stand in the world at once. Opening one no longer closes the others,
and a new hologram turns aside when another is in its way. Core also gains the panel chat's
session picker is built from: a list with buttons on its title bar and rows that can be faded.

## Upgrading

- **Players: replace the core jar.** The `v0.11.0` release carries
  `MCAgentsCoreForge-0.11.0+26.3.jar`, `MCAgentsCoreFabric-0.11.0+26.3.jar` and
  `MCAgentsCoreNeoforge-0.11.0+26.3.jar`; install the one for your loader. Chat 0.10.0 needs
  this release for its session picker.
- **`Holograms.openId()` is gone.** Use `Holograms.openIds()`, the ids of every hologram
  standing, oldest first, or `Holograms.isOpen(id)`. See
  [`wiki/api/hologram.md`](../../../../api/hologram.md).
- **A mod that closed another hologram by opening its own no longer does.** Close it with
  `Holograms.close(id)`, or open from `Holograms.replacing(id, action)` to take its place.

## Added

- **Up to 8 holograms at once** (`Holograms.MAX_OPEN`). Opening a ninth closes the one
  opened longest ago. Each is aimed at, clicked, scrolled, carried with Z, closed with its X
  and dismissed beyond 2 chunks on its own; the nearest one under the crosshair is the one
  aimed at.
- **A new hologram turns aside.** It opens in front of the player or, when an open hologram
  stands there, the spot turns around the player in 40° steps, right then left, up to 120°,
  until it is clear.
- **`Holograms.replacing(id, action)`:** the first hologram `action` opens takes the place of
  hologram `id`. The G menu uses it, so a button that opens a hologram replaces the menu, while
  a button that opens a screen leaves it standing. See
  [`wiki/api/buttons.md`](../../../../api/buttons.md).
- **`PickList` and `PickRow` panels:** a title bar with the title on the left and up to 2
  buttons on its right, then rows that are each enabled or faded; a faded row is drawn in grey
  and ignores clicks. See [`wiki/api/hologram-panels.md`](../../../../api/hologram-panels.md).
- **`Transcript` buttons and a live title:** up to 3 buttons under the entries, and a heading
  read every frame in place of the title. `new Transcript(title, entries)` still builds one
  with neither.

## Changed

- Opening a hologram whose id is already open moves that hologram in front of the player
  instead of adding a second; an id still names one hologram.
