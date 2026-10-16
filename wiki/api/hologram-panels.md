# Hologram Panels

The kinds of panel a [hologram](hologram.md) is made of. `HologramPanel` is a sealed interface
in `core-api`, under `io.github.mcagentsmods.core.api.hologram`, and each kind is a
record that implements it. A mod builds the records; core draws them and handles the player's
clicks, scrolling and typing on them. Opening, closing and moving a hologram are on
[`hologram.md`](hologram.md).

```java
// A menu: one wide panel of buttons.
List<HologramButton> buttons = new ArrayList<>();
for (Tool tool : tools) {
    buttons.add(new HologramButton(tool.name(), () -> run(tool)));
}
Hologram menu = Hologram.of("mymod.tools", new ButtonGrid("mymod.tools.title", buttons));

// A chat: three tall panels side by side, reading what the mod already holds in memory.
Hologram chat = Hologram.of("mymod.chat",
        new SelectList("mymod.sessions", store::sessionNames, store::selectedIndex, store::select,
                List.of(new HologramButton("mymod.new", store::newSession))),
        new Transcript("mymod.conversation", store::entries),
        new PromptInput("mymod.prompt", PromptInput.SEND, PromptInput.DEFAULT_MAX_LENGTH,
                () -> !store.waiting(), store::ask));
```

There are two shapes of panel. The **button grid** is wide and short: a menu. The **list**,
the **transcript** and the **prompt** are tall, made to stand side by side: each has its title
in a band across the top and a sunken box under it with a scrollbar down its right side.

**Suppliers are read every frame**, on the client thread. They must answer at once from
memory: never read a file or a database, or wait on a future, inside one. A supplier that
throws is logged once and read as empty. **Callbacks run on the client thread**, inside the
click or key that caused them, and must not block either; one that throws is logged.

## HologramPanel

`sealed interface HologramPanel permits ButtonGrid, SelectList, Transcript, PromptInput`

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

The panel is 250 by 130 pixels at 0.01 blocks to the pixel, so 2.5 blocks wide in the world.

## HologramButton

`record HologramButton(String label, Runnable action)`

| Member | Meaning |
|---|---|
| `label` | What the button says, in upper case: a translation key, or the text itself. Not blank. |
| `action` | What pressing it does. Runs on the client thread, inside the click, so it must not block. An action that throws is logged and the hologram stays open. |

A label too long for its button is drawn smaller, down to half size, so about twelve
characters fit.

## The tall panels

The list, the transcript and the prompt are each 160 by 230 pixels, so 1.6 by 2.3 blocks.
Three of them stand in an arc around the player, the outer two turned towards them.

### SelectList

`record SelectList(String title, Supplier<List<String>> rows, IntSupplier selected, IntConsumer onSelect, List<HologramButton> buttons) implements HologramPanel`

A list the player picks one row from, such as a list of chat sessions.

| Member | Meaning |
|---|---|
| `title` | The title in the top band. |
| `rows` | The rows, top to bottom, shown as given: not translated and not upper-cased. A row too wide is cut and ends in three dots. Read every frame. |
| `selected` | The chosen row's index, highlighted, or -1 for none. Read every frame. |
| `onSelect` | Called with a row's index when the player clicks it. The list does not move the highlight itself: `selected` decides it. |
| `buttons` | Up to `MAX_BUTTONS` buttons in a row under the list, sharing its width. Copied; may be empty. |
| `MAX_BUTTONS` | `3`. |
| `SelectList(title, rows, selected, onSelect)` | A list with no buttons under it. |

Fourteen rows show when there are buttons under the list, and the rest scroll a row at a time
with the mouse wheel, or jump to a click on the scrollbar. The list opens scrolled to the
chosen row.

### Transcript

`record Transcript(String title, Supplier<List<TranscriptEntry>> entries) implements HologramPanel`

A conversation to read.

| Member | Meaning |
|---|---|
| `title` | The title in the top band. |
| `entries` | The entries, oldest first. Read every frame, and wrapped again only when the list differs from the last one read. |

`record TranscriptEntry(String speaker, String text)`

| Member | Meaning |
|---|---|
| `speaker` | Who said it, shown as `<speaker>` on a line of its own, as given. |
| `text` | What they said, wrapped under the speaker at spaces, a word too long for a line cut between two characters, and line breaks kept. |

A blank line separates two entries. Nineteen lines show; the mouse wheel moves three lines a
notch, and the scrollbar jumps. While the last line shows, new entries keep the view on the
last line. Once the player scrolls up, the view stays where they put it until they scroll back
to the bottom.

### PromptInput

`record PromptInput(String title, String send, int maxLength, BooleanSupplier ready, Consumer<String> onSend) implements HologramPanel`

A box the player types into, with a send button under it.

| Member | Meaning |
|---|---|
| `title` | The title in the top band. |
| `send` | The send button's label. Not blank. |
| `maxLength` | The most characters the box takes, 1 to `MAX_LENGTH`. Typed or pasted text past it is dropped. |
| `ready` | Whether sending is allowed now. The send button is greyed while it answers `false`, or while the box holds only blanks. Read every frame. |
| `onSend` | Called with the text when the player sends it. The box empties afterwards; when `onSend` throws, the text stays. |
| `MAX_LENGTH` | `32000`. |
| `DEFAULT_MAX_LENGTH` | `4000`. |
| `SEND` | `hologram.mcagents_core.send`, "Send" in core's own language file. |
| `PromptInput(title, onSend)` | A box of `DEFAULT_MAX_LENGTH` characters under a `SEND` button that is always ready. |

The empty box says "Click here to type". Clicking it starts typing: core opens a screen of
its own that draws nothing over the world except one line at the top saying how to send and
stop, and takes the keys. While it does, the box is ringed in blue and shows the caret.

| Key | What it does |
|---|---|
| Enter | Sends the text and stops typing. Nothing happens while sending is not allowed. |
| Shift+Enter | Starts a new line. |
| Escape, or any click | Stops typing and keeps the text. |
| Backspace, Delete | Deletes the character before or after the caret. |
| Left, Right | Move the caret one character. |
| Home, End | Move the caret to the start or end of its line. |
| Ctrl+V | Pastes. A carriage return becomes a line break, a tab a space, and other control characters are dropped. |

While typing, the player cannot look around or scroll the hologram. Aiming at the send button
and clicking sends the same way as Enter. The text is kept in memory while the hologram is
open and is gone when it closes; typing stops on its own when the hologram closes.
