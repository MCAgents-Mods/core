---
name: memory-tasks-prompt-typing
description: Making typed text reach a hologram's prompt box again on Minecraft 26.3, where the game hands characters only to a screen that asks for text input, and releasing it as a patch.
---

# Task: typing into a hologram's prompt on 26.3

**Status:** in progress

## The request

On 2026-10-17 the owner reported, with a screenshot of the chat hologram's User Prompt panel:
"I can't write the prompt or I can write and it didn't appear, Check it and find the cause
then plan and fix it." The box rang blue and showed its caret, but no typed text. The owner
was away and had asked for the recommended option to be taken on every question, and for
every branch to be opened as a pull request and rebase-merged in order.

## The cause

Minecraft 26.3 reads the keyboard through SDL3, which reports typed characters only while a
window has text input switched on. The game's `TextInputManager`
(`Minecraft.textInputManager()`) switches it on for an owner (`startTextInput`) and off
again (`stopTextInput`); `EditBox`, `MultiLineEditBox` and the sign editor ask for it, and
`Gui.setScreen` switches it off whenever the screen changes. `TypingScreen` never asked, so
on 26.3 it got the keys (Enter, Backspace, the arrows) but never a character: `charTyped`
was not called at all. Typing worked when it was checked on 26.2 on 2026-10-13, which still
read the keyboard through GLFW.

Reproduced on 2026-10-17 in the Fabric, NeoForge and Forge 26.3 dev clients with chat 0.9.0:
clicking the box opened `TypingScreen` (the hint line showed, the box rang, the caret
blinked), and "hello world" typed into it left the box empty.

## Decisions

- **The fix is core's alone.** `TypingScreen` is core's, and nothing in chat types into a
  hologram itself; chat's own text fields are `EditBox`es, which already ask for text input.
  Chat needs no release: a player who updates core gets the fix.
- **`TypingScreen` asks for text input in `init()` and gives it back in `removed()`,** as the
  game's sign editor does. `init()` runs again when the window is resized; asking twice is
  harmless.
- **The input method's area is the hint line at the top of the screen,** so a candidate
  window opens there rather than over the hologram, which is drawn in the world and has no
  place on the screen.
- **A patch release, 0.10.1:** no API changes.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/prompt-typing-plan` | |
| 2 | Ask for text input while typing | `fix/prompt-typing` | |
| 3 | Release 0.10.1 | `release/0.10.1` | |

### Task 1 — chore/prompt-typing-plan

This record, and its row in the memory index.

### Task 2 — fix/prompt-typing

`TypingScreen` asks the game for text input in `init()` (`startTextInput(this)`), points the
input method at the hint line (`setTextInputArea`, the line's own box at the top of the
screen), and gives text input back in `removed()` (`stopTextInput(this)`). The class comment
says why. `wiki/api/hologram-panels.md` says that the screen takes the typed text and where an
input method's candidates open.

Checked by hand on 2026-10-17 in the Fabric 26.3 dev client with chat 0.9.0: "hello world",
Shift+Enter and "Can you read me?" showed in the box on two lines; Backspace deleted; Escape
kept the text and E then opened the inventory, so the keys went back to the game; a second
click went on typing where the caret was; Enter sent the text, which the conversation panel
showed, and emptied the box. The NeoForge and Forge 26.3 dev clients showed the same two
lines in the box, and Enter sent them. `./gradlew build` passed.
