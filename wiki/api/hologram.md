# Hologram

A hologram is a set of panels standing in the level in front of the player, who aims at them
and clicks instead of opening a screen. A mod describes one with `Hologram`, in
`core-api` under `io.github.mcagentsmods.core.api.hologram`, and opens it with
`Holograms`, in `core-client` under `io.github.mcagentsmods.core.client.hologram`. Core
draws it, takes the player's clicks, mouse wheel and move key, and removes it. Core opens no
hologram of its own. This page covers opening, closing and what the player does; the kinds of
panel are on [`hologram-panels.md`](hologram-panels.md).

```java
Hologram menu = Hologram.of("mymod.menu",
        new ButtonGrid("mymod.menu.title", List.of(
                new HologramButton("Chat", () -> Holograms.open(chatHologram())),
                new HologramButton("mymod.menu.settings", this::openSettings))));

// From a key handler, on the client thread: the first press opens it, the next closes it.
Holograms.toggle(menu);
```

## Hologram

`record Hologram(String id, List<HologramPanel> panels)`

| Member | Meaning |
|---|---|
| `id` | Which hologram it is: 1 to 64 lowercase letters, digits, dots, dashes or underscores, conventionally `{modid}.{name}`. One hologram per id is open at a time; a mod that shows several of a kind gives each its own id, such as `{modid}.{name}.{key}`. |
| `panels` | The panels, left to right, copied. At least one and at most `MAX_PANELS`. |
| `of(String id, HologramPanel... panels)` | The same, from a list of arguments. |
| `MAX_PANELS` | `3`. More would run off the sides of the screen. |

The constructor throws `IllegalArgumentException` for an id or a panel count outside those
limits, and `NullPointerException` for a null one. A hologram is plain data: building one opens
nothing.

## Holograms

`final class Holograms`, all static.

| Method | Meaning |
|---|---|
| `open(Hologram)` | Opens it in front of the player, beside any already standing. One with the same id closes first, so it moves rather than doubles. With no player in a world it logs a warning and does nothing. |
| `toggle(Hologram)` | Closes the hologram with this one's id when it is open, and otherwise opens this one. What a key that shows and hides a hologram calls. |
| `close(String id)` | Closes the hologram with this id, if it is open. |
| `closeAll()` | Closes every hologram. |
| `replacing(String id, Runnable action)` | Runs `action`; the first hologram it opens takes the place of the hologram with this id, which closes. When `action` opens none, that one stays. A menu runs its buttons this way. |
| `isOpen(String id)` | Whether the hologram with this id is open. |
| `openIds()` | The ids of the holograms open, as a `List<String>`, the one opened longest ago first. |
| `MAX_OPEN` | `8`, the most holograms that stand at once. |

**Several at once.** Up to `MAX_OPEN` holograms stand at once, and the player aims at, clicks,
scrolls, carries and closes each on its own. Opening one more closes the one opened longest
ago, so opening never fails.

**Threading.** Call `open`, `toggle`, `close`, `closeAll` and `replacing` from the client
thread. A call from any other thread is handed to the client thread and takes effect on its
next turn, so `isOpen` answers `false` until then. `isOpen` and `openIds` may be read from any
thread.

A button's action runs on the client thread, inside the click. It must not block: hand slow
work to a future, and come back to the client thread with `Minecraft.execute`. An action that
throws is logged and the hologram stays open.

## Where it stands

A hologram opens in front of the player's eyes, a little below them, facing the player: 2.4
blocks away, or further for a wider hologram so that its edges stay on screen. Several panels
stand in an arc around the player, the outer ones turned towards them. It then stays at that
place in the world until the player moves it.

When a hologram already stands on that spot, the new one turns around the player instead: 40°
to the player's right, then 40° to the left, then 80° and 120° each way, and opens on the first
spot that is clear. When none is clear, it opens straight ahead, in front of the other. A spot
is clear when the two holograms' widths, measured on the ground around their middles, do not
meet.

Nothing is saved. An open hologram lives in memory and is gone when it closes; it does not
survive leaving the world.

## What the player does

| The player | What happens |
|---|---|
| Aims the crosshair at a panel, within 12 blocks and with nothing nearer in the way | The button under the crosshair lightens. Where holograms stand one behind another, the nearest one is aimed at. |
| Left-clicks a button | Its action runs. The block behind the hologram is not hit. |
| Left-clicks a list's row, or a prompt's box | The mod is told which row was chosen, or typing starts; see [`hologram-panels.md`](hologram-panels.md). |
| Turns the mouse wheel while aiming at a panel | The panel scrolls, and the hotbar slot does not change. |
| Holds the move key while aiming at a hologram | That hologram follows the crosshair at the distance it was taken from, turned to face the player. One hologram is carried at a time. |
| Lets go of the move key | The hologram stays where it is. |
| Clicks the X in the top right corner of the last panel | The hologram closes. |
| Is more than two chunks from the hologram's chunk, on either axis | The hologram closes. Placed in chunk 1, it stays while the player is in chunks 2 and 3 and goes in chunk 4. The chunk counted is the one the hologram stands in now, after any move. |
| Changes dimension or leaves the world | Every hologram closes. |

The move key is **Z** unless the player rebinds it, under *Controls*, in the *MCAgents Core*
category, as *Move a hologram (hold)*. Every button grid says so in its footer, naming the key
as it is bound.

While a screen is open, such as the inventory or chat, the holograms are drawn but take no
clicks, wheel or move key.

## Text

A title or a label is a translation key, or the text itself when no translation has that key,
and is shown in upper case. Translations go in the calling mod's own `lang` files, as for any
other text.

## Related

- The panel kinds and how each is drawn: [`hologram-panels.md`](hologram-panels.md).
- Depending on `core-client`: [`dependency.md`](dependency.md).
- How each loader hooks the hologram into the game: [`mod-runtime.md`](mod-runtime.md).
