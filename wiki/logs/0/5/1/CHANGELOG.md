# 0.5.1

Released: 2026-10-12

A fix to how `mcagents-core-common`'s test fixtures are published. Nothing in the mod
jars changes.

## Upgrading

- **Depend on 0.5.1 to use the test fixtures from the published artifacts.**
  `testFixtures('io.github.mcagentsmods:mcagents-core-common:0.5.1')` now resolves
  outside a composite build. Against 0.5.0 it fails with "Unable to find a variant …
  with the requested capability"; a build that worked around it by requesting
  `io.github.mcagentsmods:common-test-fixtures` keeps working on 0.5.1.

## Fixed

- The test-fixtures variants of `mcagents-core-common` carried only the capability
  `io.github.mcagentsmods:common-test-fixtures`, named after the Gradle project rather
  than the published artifact, so `testFixtures(...)` on the published coordinates could
  not find them. They now also carry `mcagents-core-common-test-fixtures`.
