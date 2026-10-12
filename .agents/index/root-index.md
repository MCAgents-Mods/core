---
name: root-index
description: Root router — the single entry point to every index this repository can reach, local and shared, plus the shared override table.
---

# Root Index

## Indexes

| Index | Scope | Load when |
|---|---|---|
| [`agents-index.md`](agents-index.md) | This repository's instruction set | You need a rule specific to this repository. |
| `agents://index/root-index.md` | The shared instruction set | You need a branching, commit, pull request, planning, directory, versioning, or creator convention. |
| [`agent-wiki-index.md`](agent-wiki-index.md) | `.agents/wiki/` agent knowledge | You need an SOP, domain guideline, or operating context written for agents. |
| [`project-wiki-index.md`](project-wiki-index.md) | `wiki/` human documentation | You need to read or write documentation a person will read. |
| [`project-wiki-api-index.md`](project-wiki-api-index.md) | `wiki/api/` public API pages | You are about to call core, or must document a public type. |
| [`memory-index.md`](memory-index.md) | `.agents/memory/` dynamic state | You need prior task state, session history, or must record progress. |
| [`logs-index.md`](logs-index.md) | `wiki/logs/` versioned change logs | You need release history or must record a change. |

## Shared overrides

| `name` | Local file | Replaces | Why |
|---|---|---|---|

No overrides — this repository uses the shared set unchanged.

**This file lists indexes only** — never rules, never documentation, never links to
leaf content. Adding, removing, or renaming any file in `.agents/index/` updates the
index table in the same commit, and adding or dropping an override updates the
override table in the same commit. Read exactly one branch per task, plus
`memory-index.md`; do not preload the others.
