# Adding Pages to the Web Console

Core runs one local web console for every MCAgents mod. A mod does not start a
server of its own. It registers its pages, and core serves them next to every
other mod's, under one header, behind one token. For every type named here, see
[`../api/web-console.md`](../api/web-console.md).

## Register a module

Implement `WebModule` and register it once, from your client initializer:

```java
public final class ChatPages implements WebModule {

    @Override public String id() { return "chat"; }
    @Override public String title() { return "MCAgents Chat"; }

    @Override public List<NavLink> navigation() {
        return List.of(new NavLink("Knowledge", "knowledge/"), new NavLink("Chat", "chat/"));
    }

    @Override public Optional<String> page(String path) {
        return switch (path) {
            case "knowledge/" -> Optional.of("pages/knowledge.html");
            case "chat/" -> Optional.of("pages/chat.html");
            default -> Optional.empty();
        };
    }

    @Override public String resourceRoot() { return "mcagents/web/"; }

    @Override public WebResponse handle(WebRequest request) {
        if (request.is("GET", "knowledge")) {
            return WebResponse.ok(knowledgeJson());
        }
        return WebResponse.error(404, "No such endpoint.");
    }
}

CoreWebRegistry.shared().register(new ChatPages());
```

Registering opens no port. The console starts when something calls
`CoreWebRegistry.shared().open()`, usually a button in your mod's screen, and
`open()` returns the URL to send the browser to. Use `console.url("chat")` to
land on your own first page. Core closes the console when the game shuts down.

The module id must be 1 to 32 characters of `a`–`z`, `0`–`9` and `_`. `core`,
`api` and `assets` are reserved, and an id another mod already registered is
refused.

## Where your files are served

| Your module answers | At | Needs the token |
|---|---|---|
| `page(path)` | `/{id}/{path}` | Yes |
| a file under `resourceRoot()` | `/assets/{id}/{file}` | No |
| `handle(request)` | `/api/{id}/{path}` | Yes |

Assets are served without the token because a `<link>` or `<script>` tag cannot
carry one. **Never put player data in a resource.** Anything on the machine can
read an asset. Player data belongs behind `/api/`.

Resources are read through your module's own class loader, so they are found on
every loader, NeoForge's per-mod module layers included.

## Write a page

Every page loads core's shell and names itself on `<body>`:

```html
<link rel="stylesheet" href="/assets/core/css/main.css">
<link rel="stylesheet" href="/assets/core/css/layout.css">
<link rel="stylesheet" href="/assets/core/css/components.css">
<link rel="stylesheet" href="/assets/chat/css/chat.css">
…
<body data-module="chat" data-section="knowledge">
<div id="site-header"></div>
…
<div id="site-footer"></div>
<script src="/assets/core/js/site.js"></script>
<script src="/assets/chat/js/knowledge.js"></script>
</body>
```

`data-section` marks the header link whose section matches. A link's section is
the first segment of its path unless the `NavLink` names one.

`site.js` must be loaded at the end of `<body>`. It fills in the header and
footer and exposes `window.MCAgents`:

| Member | What it does |
|---|---|
| `api(path, options)` | Calls your module's API: `api('/knowledge')` is `/api/chat/knowledge`. Sends the token in a header. Throws with the server's `error` text when the answer is not OK. |
| `apiOf(id, path, options)` | Calls another module's API. |
| `link(path)` | One of your own pages, with the token: `link('chat/')`. |
| `withToken(href)` | Any console path, with the token. |
| `el`, `replace`, `showState` | Build DOM. `el` sets text as text, never as markup. |
| `confirm`, `prompt` | The console's own dialogs, in place of the browser's. |
| `debounce`, `onVisible`, `paintToggle`, `formatDate` | Small helpers the shell's own pages use. |

## The content security policy

Every answer carries a policy that allows scripts, styles, images and requests
from the console itself and nothing else. Two consequences for a page:

* **No inline script.** A `<script>` block in the page, or an `onclick`
  attribute, does not run. Put the code in a file under your resource root, and
  pass values through `data-` attributes.
* **Nothing from the internet.** No CDN font, stylesheet or script. The console
  has to work on a machine with no network, and the policy enforces it.

Inline `style` attributes are allowed.

## Handle a request

`handle` runs on one of the console's four worker threads, never on the game
thread, and may block. Up to 64 more requests wait for a worker; past that the
console closes the connection, so a slow handler holds up the console, never
the game. A request carries:

* `method()`, upper case;
* `path()`, everything after `/api/{id}/`, decoded once — do not decode it again;
* `query()`, decoded, without the token;
* `body()`, at most 256 KiB. A larger body is refused with a 413 before your
  module sees it.

To refuse input, throw `IllegalArgumentException`: its message goes back to the
page as a 400, so write it for the player. Any other exception is logged, and the
page gets a 500 that names nothing. Never put a key, a file path, or an
exception's text in an answer.

`WebResponse.ok(json)` and `WebResponse.json(status, json)` send JSON you built.
`WebResponse.quote(text)` turns a string into a safe JSON string literal if you
build a small answer by hand.

## Unregister

`CoreWebRegistry.shared().unregister("chat")` removes a module. Its paths answer
404 from the next request.
