# 0.5.3

Released: 2026-10-13

The API documentation moves to `wiki/api/`, one page per scope. No code, jar or API changes.

## Upgrading

- **Nothing to change in a build.** The jars are the same as 0.5.2's apart from the version.
- **Links to `wiki/reference/api.md` break.** Point them at the page for their scope under
  `wiki/api/`; [`wiki/api/dependency.md`](../../../../api/dependency.md) lists every page.

## Added

- `wiki/api/`: fifteen pages, each covering one thing a caller does. Besides the provider,
  the model client and their types, the shared module's start-up and sides, the shared key
  file and the token commands are documented method by method for the first time.

## Changed

- The README, the guides and the agent repository map link to the new pages.

## Removed

- `wiki/reference/api.md`, the single page that covered every public type.

## Fixed

- The API pages no longer say `MCAgentsProvider` is the only public class in `common`, nor
  that core registers an `/agents` command; no loader registers one, and
  `wiki/api/token-commands.md` says how a mod wires it.
