---
name: agents-index
description: Index of this repository's own instruction files — the repository rules and the Minecraft platform knowledge.
---

# Agents Index

**Scope:** `.agents/` instruction folders — this repository's own instructions only
**Parent:** [root-index](root-index.md)

This index is the **sole authority** that indexes the local instruction tree. It does
not manage `.agents/index/`, `.agents/wiki/`, or `.agents/memory/` — those are
reserved structural folders owned by [`root-index.md`](root-index.md),
[`agent-wiki-index.md`](agent-wiki-index.md), and
[`memory-index.md`](memory-index.md). Any instruction file added to or removed from
`.agents/` is reflected in this index **in the same commit**.

**It lists local files only.** Branching, commits, pull requests, the task workflow,
the standing prompts, the creators, and the universal rules are served by the
`lxagents-agents-base` connector and are never listed here — route to them through
`agents://index/root-index.md`, as [`root-index.md`](root-index.md) directs.

## Rules

| File | Purpose |
|---|---|
| [`../rules/repository.md`](../rules/repository.md) | What is actually true about this repository right now, and the constraints a change must not break. |

## Knowledge

| File | Purpose |
|---|---|
| [`../knowledge/minecraft-platform.md`](../knowledge/minecraft-platform.md) | Which mod loaders and Minecraft version are targeted, and the threading constraints shared code must honour. |
