# 0.9.0

Released: 2026-10-16

Core's artifact ids drop the repeated `mcagents-`, and only the library modules are
published to GitHub Packages; the loader jars are on each version's GitHub release only.

## Upgrading

- **Ask for the new coordinates.** The group stays `io.github.mcagentsmods`; each artifact id
  loses its `mcagents-` prefix:

  | Before | From 0.9.0 |
  |---|---|
  | `mcagents-core-api` | `core-api` |
  | `mcagents-core-common` | `core-common` |
  | `mcagents-core-shared` | `core-shared` |
  | `mcagents-core-client` | `core-client` |

  A mod that writes `testFixtures('io.github.mcagentsmods:core-common:0.9.0')` gets the test
  fixtures as before. See [`wiki/api/dependency.md`](../../../../api/dependency.md).
- **Take the loader jars from the GitHub release.** `mcagents-core-fabric`, `-neoforge` and
  `-forge` have no 0.9.0 on GitHub Packages. The Release workflow attaches
  `MCAgentsCore{Loader}-0.9.0+26.3.jar` to the `v0.9.0` release, as it did for 0.8.0.
- Players change nothing: jar names, the mod id and what the jars do are the same.

## Changed

- Published artifact ids are `core-{module}`, as in `core-api` and `core-client`; the extra
  test-fixture capability is `core-common-test-fixtures`.
- The Publish workflow sends `api`, `common`, `shared` and `client` to GitHub Packages, and no
  loader jar.

## Removed

- The Fabric, NeoForge and Forge publications. The loader subprojects still build their jars.
