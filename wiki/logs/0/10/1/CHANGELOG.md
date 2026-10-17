# 0.10.1

Released: 2026-10-17

Typing into a hologram's prompt works again on Minecraft 26.3. Clicking the box rang it blue
and showed the caret, but no typed letter ever reached it.

## Upgrading

- **Players: replace the core jar.** The `v0.10.1` release carries
  `MCAgentsCoreForge-0.10.1+26.3.jar`, `MCAgentsCoreFabric-0.10.1+26.3.jar` and
  `MCAgentsCoreNeoforge-0.10.1+26.3.jar`; install the one for your loader. Mods built on core,
  chat among them, need no update.
- **Nothing to change in a build.** No API changes.

## Fixed

- **The prompt box takes typed text on 26.3.** Minecraft 26.3 reads the keyboard through
  SDL3, which reports typed characters only while a screen has asked for text input. The
  screen core opens while the player types into a prompt never asked, so it got Enter,
  Backspace and the arrows but no letters. It now asks when it opens and stops asking when
  it closes, as the game's own sign editor does, and an input method's candidates open by
  the hint line at the top of the screen.
