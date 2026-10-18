---
name: memory-decisions-minecraft-latest-only
description: The owner's permanent decision that every MCAgents mod supports only the latest Minecraft release, with the game version kept in jar names but not in artifact ids.
---

# Decision: only the latest Minecraft release

**Decided:** 2026-10-16, by the owner, as permanent. It replaces the 2026-10-13 choice to keep
26.2 beside 26.3.

- Core, and every MCAgents mod, supports exactly one Minecraft version: the latest release,
  26.3 today. No older version is kept, restored or added beside it.
- Jar names keep the game's version as a suffix: `MCAgentsCoreFabric-{version}+26.3.jar`.
- Gradle project names and artifact ids do not carry it: `:platforms:client`,
  `mcagents-core-client`.
- The version is named once, by the catalog's Minecraft block; since 0.13.2 each
  Minecraft-facing module is a single folder, `platforms/{module}/`, with no version folder.
  Moving to a new release replaces that block, and whatever the release breaks, in one change,
  and bumps the minor version; the old version's files are deleted in the same change.

**Why:** the owner: "we will not have another version support". Keeping two versions doubled
every build, release and dev-client check for players who can update the game.
