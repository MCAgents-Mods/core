# 0.6.1

Released: 2026-10-13

Each version's mod jars are attached to its GitHub release. No code, jar or API changes.

## Upgrading

- **Nothing to change in a build or a game.** The jars are the same as 0.6.0's apart from
  the version.
- **Players: download core from the repository's Releases page.** The `v0.6.1` release
  carries `MCAgentsCoreForge-0.6.1.jar`, `MCAgentsCoreFabric-0.6.1.jar` and
  `MCAgentsCoreNeoforge-0.6.1.jar`; install the one for your loader.

## Added

- `.github/workflows/release.yml`, the Release workflow. After each Publish run on `master`,
  when the `v{version}` tag has no GitHub release yet, it builds the Forge, Fabric and
  NeoForge jars from the tag, creates the release with them attached and this changelog as
  its notes, and then deletes the copy of the jars the run kept in Actions. It can also be
  run by hand, with a `tag` input for an earlier tag that has no release.

## Changed

- The setup page, the overview, the repository map and the repository rule describe two
  workflows, Publish and Release.
