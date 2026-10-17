---
name: memory-tasks-hologram-forms
description: Adding the form panel so every agents menu button can open a hologram instead of a flat screen, turning core's own Settings into one, and releasing 0.12.0.
---

# Task: forms on holograms

**Status:** in progress

## The request

On 2026-10-17 the owner asked: "Make every button show as GUI hologram likes chat. use same
theme. Plan then work immediately." The attached picture showed chat's flat Models screen as
the kind of page that should become a hologram. The message is the go-ahead, and the owner had
already asked for every branch to be opened as a pull request and rebase-merged in order.

Core's part: a panel kind that pages with fields can be built from, typing into any box on a
hologram, a callback when a hologram closes, and core's own Settings as a hologram. Chat's
part (its Keys, Models, Persona and Wallets pages and their dialogs) is chat's own task record,
`hologram-pages.md`, released against this release.

## Decisions

- **A new panel kind, `Form`:** a title band, rows top to bottom, and up to 3 buttons along the
  bottom. Rows are `FormText` (a wrapped line in a tone), `FormField` (a one-line box, optionally
  secret, optionally with suggestions), `FormArea` (a multi-line box filling the room left),
  `FormToggle`, `FormStepper` and `FormButtons`. Rows that do not fit are not drawn.
- **The mod holds a field's text.** `value` is read every frame and `onChange` gets every edit;
  core holds only the text while the player types it.
- **Suggestions open as a list** from an arrow at the box's right, over the rows below; a pick
  fills the box through `onPick` (by default `onChange`).
- **One typing screen for every box.** Enter ends a field and sends a prompt; Shift+Enter is a
  new line in an area and a prompt; Tab moves to the form's next field; Escape or a click stops.
- **Two form widths:** normal, 160 pixels like the tall panels, and wide, 240 pixels.
- **`Hologram` gets an `onClose` callback**, run once when the hologram closes for any reason.
  The two-argument constructor and `Hologram.of` stay.
- **SETTINGS opens a Settings form hologram** in the menu's place; `AgentsSettingsScreen` goes.
  Save writes and keeps the hologram open, saying it saved.
- **A minor release, 0.12.0:** new API. It follows 0.11.1, the compile-only release that landed first.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/hologram-forms-plan` | |
| 2 | Form panel | `feat/hologram-form` | |
| 3 | Settings hologram | `refactor/settings-hologram` | |
| 4 | Release | `release/{version}` | |

### Task 1 — chore/hologram-forms-plan

This record, and its row in the memory index.
