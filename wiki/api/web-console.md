# Web Console

One loopback web server that every MCAgents mod registers its pages into. A mod registers a
`WebModule` with `CoreWebRegistry.shared()`, the one registry in the game; `open()` starts the
console and returns its URL, token included. How to write a module and its pages:
[`../guides/web-pages.md`](../guides/web-pages.md).

```java
CoreWebRegistry registry = CoreWebRegistry.shared();
registry.register(myModule);
String url = registry.open().url(myModule.id());
```

The contracts are in `io.github.mcagentsmods.core.api.web`;
`io.github.mcagentsmods.core.common.web.CoreWebRegistry` implements `WebRegistry`.

## WebRegistry and CoreWebRegistry

| Method | Meaning |
|---|---|
| `register(WebModule)` | Adds a module. Throws `IllegalArgumentException` for an invalid or reserved id, or a resource root with a leading slash, no trailing slash, or `..`; `IllegalStateException` for an id already registered. |
| `unregister(String id)` | Removes one. `false` when nothing had that id. |
| `moduleIds()` | Every registered id, in registration order. |
| `open()` | The running console, starting one if none runs. Throws `WebConsoleException` when no port can be opened. |
| `current()` | The running console, if any. |
| `close()` | Stops the running console and keeps the registrations. Core calls it at shutdown. |

## WebModule

| Method | Meaning |
|---|---|
| `id()` | `[a-z0-9_]{1,32}`, and not `core`, `api` or `assets`. The prefix of every path the module owns. |
| `title()` | The module's name on the console's front page and in the header. |
| `navigation()` | `List<NavLink>`: the header's links, in order. |
| `page(String path)` | The resource for `/{id}/{path}`, or empty for a 404. |
| `resourceRoot()` | The classpath folder pages and assets are read from, such as `mcagents/web/`. |
| `resourceLoader()` | The class loader they are read through. The module's own by default. |
| `handle(WebRequest)` | Answers `/api/{id}/…`. Defaults to a 404. |

`NavLink(label, path, section)`: `path` is relative to the module and may not start with `/`,
or contain `..`, a scheme, a query or a fragment. `section` defaults to the path's first
segment.

`WebRequest(method, path, query, body)`: `is(method, path)` and `parameter(name)` read it.

`WebResponse(status, contentType, body)`: `ok(json)`, `json(status, json)`,
`error(status, message)`, and `quote(text)` for a JSON string literal.

## WebConsole

One run of the server.

| Method | Meaning |
|---|---|
| `url()` | The front page, `/core/`, with the token. |
| `url(String moduleId)` | That module's first navigation link, with the token. |
| `port()` | The loopback port. |
| `isRunning()` | Whether it still listens. |
| `close()` | Stops it, giving requests in flight two seconds. Safe to call twice. |
| `stopped()` | A future that completes when it has stopped. |

A closed console is not reopened: `open()` starts a new one, on a new port, with a new token.

Every answer carries no `Access-Control-Allow-Origin`, `Cache-Control: no-store`,
`Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
`Cross-Origin-Resource-Policy: same-origin`, and a `Content-Security-Policy` that allows only
the console's own scripts, styles, images and requests.
