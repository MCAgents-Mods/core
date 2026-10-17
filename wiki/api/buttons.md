# Buttons

A mod puts its feature on the agents menu, the hologram the player shows with **G**, by
registering an `AgentButton` with `Agents.buttons()`. Keep the `AutoCloseable` that `register`
returns: closing it takes the button away again.

```java
AutoCloseable button = Agents.buttons().register(new AgentButton(
        "mymod.open", "gui.mymod.open", 100, MyScreen::open));
```

Each mod built on core adds its own, so the menu is where the player finds every agent
feature they have installed.

## AgentButton

`record AgentButton(String id, String label, int order, Runnable action)`, in
`io.github.mcagentsmods.core.api.button`, in `core-api`.

| Component | Meaning |
|---|---|
| `id` | Unique among buttons. 1 to 64 lowercase letters, digits, dots, dashes or underscores, conventionally `{modid}.{name}`. |
| `label` | A translation key, or the text itself when there is no translation. Not blank. The menu shows it in upper case. |
| `order` | Where the button sits: lower comes first, and equal orders sort by id. |
| `action` | What pressing it does. It runs on the game's thread, inside the click, so it must not block. |

A malformed id or a blank label throws `IllegalArgumentException`, and a `null` component
`NullPointerException`:

```
The button id 'MyMod.Open' must be 1 to 64 lowercase letters, digits, dots, dashes or underscores
```

## AgentButtons

The buttons on the agents menu, in `io.github.mcagentsmods.core.common.agent`, in
`core-common`. There is one per game, `Agents.buttons()`.

| Method | Returns | What it does |
|---|---|---|
| `register(AgentButton button)` | `AutoCloseable` | Adds the button. A button with the same id already there throws `IllegalArgumentException`. Closing the result removes this button, and nothing if another has since taken its id. |
| `list()` | `List<AgentButton>` | Every button, in the order the menu shows them. |
| `isEmpty()` | `boolean` | Whether there are none. |

The buttons are held by id in a `ConcurrentHashMap`, so a mod may register one from any thread
at any time. `AutoCloseable.close()` declares `Exception`, but the one `register` returns
never throws.

## The agents menu

`io.github.mcagentsmods.core.client.agent.AgentsMenu`, in `core-client`, builds
the menu and hands it to core's holograms, which draw it in the world and take the player's
clicks: see [`hologram.md`](hologram.md) for where it stands, the Z key that moves it and the
two-chunk range that closes it. The menu is a `ButtonGrid` titled **MCAGENTS**, five buttons to a row and three rows showing, scrolled
with the mouse wheel past that.

| What the player sees | When |
|---|---|
| One button for each `AgentButton`, in `list()` order | Always; the list is read each time the menu opens, so a button registered late appears the next time. |
| **SETTINGS** after them | Always. It opens the settings screen ([`settings.md`](settings.md)), which goes back to the game when it closes. |
| The menu staying open, and the reason in the log | A button's action threw. |

A button whose action opens another hologram, as chat's does, replaces the menu with it: the
menu runs every button through `Holograms.replacing`, so the new hologram opens where the menu
stood. A button whose action opens a screen leaves the menu standing.

| Member | What it is |
|---|---|
| `ID` | `mcagents_core.menu`, the menu's hologram id. |
| `toggle()` | Closes the menu when it is open, and opens it otherwise, as G does. Other holograms stay. |
| `open()` | Opens the menu in front of the player, moving it there if it is open already. |
| `close()` | Closes the menu, if it is open. |
| `isOpen()` | Whether the menu is open. |

Call them on the game's thread. A mod built on core needs none of them to add a button;
they are for a mod that wants to bring the menu back, from its own hologram for instance.

## The G key

`io.github.mcagentsmods.core.client.agent.AgentsKeys` builds the key that shows the menu.
Each loader module of this repository registers it; a mod built on core calls none of these.

| Member | What it is |
|---|---|
| `create(KeyMapping.Category category)` | Builds the key, `key.mcagents_core.agents`, bound to G, in the category it is given, core's own **MCAgents Core** (`HologramClient.KEY_CATEGORY`, beside the move key), for the loader to register. Each loader calls it first in its client start-up, because every loader refuses a key once the game's options exist. |
| `onEndTick(Minecraft client)` | Shows or hides the menu when the key was pressed. Each loader calls it at the end of every client tick. |

The player can rebind the key under **MCAgents Core** in Controls. It does nothing while a screen
is open.
