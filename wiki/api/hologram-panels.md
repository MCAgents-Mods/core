# Hologram Panels

The kinds of panel a [hologram](hologram.md) is made of. `HologramPanel` is a sealed interface
in `mcagents-core-api`, under `io.github.mcagentsmods.core.api.hologram`, and each kind is a
record that implements it. A mod builds the records; core draws them and handles the player's
clicks and scrolling on them. Opening, closing and moving a hologram are on
[`hologram.md`](hologram.md).

```java
List<HologramButton> buttons = new ArrayList<>();
for (Tool tool : tools) {
    buttons.add(new HologramButton(tool.name(), () -> run(tool)));
}
Holograms.open(Hologram.of("mymod.tools", new ButtonGrid("mymod.tools.title", buttons)));
```

## HologramPanel

`sealed interface HologramPanel permits ButtonGrid`

| Member | Meaning |
|---|---|
| `title()` | The panel's title, shown at its top in upper case: a translation key, or the text itself when no translation has that key. |

## ButtonGrid

`record ButtonGrid(String title, List<HologramButton> buttons) implements HologramPanel`

A menu: a large title over a grid of buttons, with a scrollbar on the right and a footer
telling the player how to move the hologram and when it goes away.

| Member | Meaning |
|---|---|
| `title` | The large title at the top. |
| `buttons` | The buttons, left to right and then row by row, copied. May be empty. |
| `COLUMNS` | `5`: buttons in one row. The sixth starts a new row. |
| `VISIBLE_ROWS` | `3`: rows showing at once. |

With more than three rows, the grid scrolls a row at a time with the mouse wheel, or jumps to
where the player clicks on the scrollbar's track. The scrollbar is drawn even when everything
fits, with its thumb filling the track.

A panel is 250 by 130 pixels at 0.01 blocks to the pixel, so 2.5 blocks wide in the world.

## HologramButton

`record HologramButton(String label, Runnable action)`

| Member | Meaning |
|---|---|
| `label` | What the button says, in upper case: a translation key, or the text itself. Not blank. |
| `action` | What pressing it does. Runs on the client thread, inside the click, so it must not block. An action that throws is logged and the hologram stays open. |

A label too long for its button is drawn smaller, down to half size, so about twelve
characters fit.
