---
name: memory-tasks-api-wiki
description: Splitting core's one-page API reference into wiki/api/, one page per scope, so an agent reads only the API it is about to use, and releasing 0.5.3 with it.
---

# Task: API wiki

**Status:** in progress

## The request

On 2026-10-12 the owner asked, for `core` and `agents`: "add folder `wiki/api/**.md` so agents
can know how to use it by reading. Each file contain it own scope. If it out scope then create
another `.md` file." It is task 1 of two in the owner's request; task 2 is the in-game
hologram that replaces the web console, and it follows this release.

Today `wiki/reference/api.md` is one 421-line page covering every public type in `api` and
`common`. Nothing documents the public types of `platforms/shared` method by method.

## Decisions

The owner was away and had asked for the recommended option to be taken on every question.

- **One page per scope, in `wiki/api/`.** A scope is one thing a caller does: create and call
  the provider, hand core a key store, build a request, route a model name, and so on. A page
  that would need a heading unrelated to its name is split.
- **`wiki/reference/api.md` is deleted, not kept beside the new pages.** A second copy of
  every table would drift. Every link to it moves to the page for its scope.
- **The shared module gets pages too.** Start-up and sides, the shared key file, and the
  token commands are three scopes, so they are three pages rather than the one
  `mod-runtime.md` the first plan named.
- **The folder gets its own index**, `project-wiki-api-index.md`: it holds more than ten
  pages, which is the shared split threshold.
- **Release 0.5.3.** A patch: documentation only, no code or jar changes. Approved with the
  plan.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/api-wiki-plan` | |
| 2 | API pages | `docs/api-wiki` | |
| 3 | Release 0.5.3 | `release/0.5.3` | |

### Task 1 — chore/api-wiki-plan

This record, and its row in the memory index.
