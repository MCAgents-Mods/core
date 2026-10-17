# 0.12.0

Released: 2026-10-17

A page with fields can now be a hologram. Core gains the `Form` panel, typing into any box on
it, and a callback when a hologram closes, and its own **SETTINGS** button opens a Settings
hologram instead of a flat screen. Chat 0.11.0 builds its Keys, Models, Persona and Wallets
pages from it.

## Upgrading

- **Players: replace the core jar.** The `v0.12.0` release carries
  `MCAgentsCoreForge-0.12.0+26.3.jar`, `MCAgentsCoreFabric-0.12.0+26.3.jar` and
  `MCAgentsCoreNeoforge-0.12.0+26.3.jar`; install the one for your loader. Chat 0.11.0 needs
  this release for its pages.
- **Mods built on core: nothing breaks.** Every 0.11 call still compiles. `Hologram` gains a
  third component, `onClose`; `new Hologram(id, panels)` and `Hologram.of(...)` still build one
  with nothing to run. A `switch` over `HologramPanel` must add a `Form` case.
- **The settings screen is gone.** `AgentsSettingsScreen` and its package
  `io.github.mcagentsmods.core.client.agent.gui` are deleted; SETTINGS opens
  `SettingsHologram`. The settings file and its keys do not change.

## Added

- **The `Form` panel:** a title, rows top to bottom and up to 3 buttons along the bottom, at
  one of two widths (`FormWidth.NORMAL`, 160 pixels, or `WIDE`, 240). The rows are
  `FormText` (wrapped text in a `FormTone`), `FormField` (a one-line box, optionally drawn as
  stars with `hidden()`, optionally with suggestions behind an arrow), `FormArea` (a multi-line
  box filling the room left), `FormToggle`, `FormStepper` and `FormButtons`. The mod holds the
  text: `value` is read every frame and `onChange` gets every edit. Rows that do not fit are not
  drawn. See [`wiki/api/hologram-panels.md`](../../../../api/hologram-panels.md#form).
- **Typing into any box.** Clicking a field or an area starts typing into it, as the prompt
  does: Enter ends a field, Shift+Enter is a new line in an area, Tab moves to the form's next
  box, Ctrl+Backspace deletes back to the start of the line, and Escape or a click elsewhere
  stops.
- **`Hologram.onClose`**, run once when a hologram closes for any reason: its X, a replacing
  hologram, distance, or the player leaving the world. `withOnClose(Runnable)` adds one. See
  [`wiki/api/hologram.md`](../../../../api/hologram.md).
- **Three note colours** in `HologramStyle`: green, red and amber, for `FormTone.GOOD`, `BAD`
  and `WARN`.

## Changed

- **A new hologram turns aside in 20° steps** instead of 40°, still up to 120° each way, so a
  small one such as a dialog opens just past the edge of the hologram in its way rather than
  far beyond it.
- **SETTINGS opens the Settings hologram** in the menu's place: the two steppers (tool calls
  at once, tool rounds) with their help lines, DEFAULTS and SAVE. SAVE writes the settings
  file and the hologram stays open, saying it saved. See
  [`wiki/api/settings.md`](../../../../api/settings.md).
