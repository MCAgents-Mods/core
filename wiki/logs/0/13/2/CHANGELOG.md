# 0.13.2

Released: 2026-10-18

A build change only: every platform module is now one folder with one `build.gradle` and one
`src/`. The loader jars carry exactly what 0.13.1's did.

## Upgrading

- **Players: nothing to do.** The `v0.13.2` release carries
  `MCAgentsCoreForge-0.13.2+26.3.jar`, `MCAgentsCoreFabric-0.13.2+26.3.jar` and
  `MCAgentsCoreNeoforge-0.13.2+26.3.jar`, with the same contents as 0.13.1's; replacing the jar
  is optional. Mods built on core need no update.
- **Nothing to change in a build.** No API changes; the published modules keep their artifact
  ids and dependencies, and the project paths, such as `:platforms:fabric`, are unchanged.
- **A core checkout moves its sources.** Anything that pointed into a `platforms/{module}/26.3/`
  folder, such as `platforms/fabric/26.3/build/libs/`, now points at `platforms/{module}/`.

## Changed

- **One build file per platform module.** `platforms/client`, `platforms/fabric`,
  `platforms/forge` and `platforms/neoforge` each hold their whole build in their own
  `build.gradle`, in place of a `26.3/` subproject that applied a `{module}.gradle` script, and
  their sources in one `src/`: Forge's `AttackListener` and `pack.mcmeta` and the client's
  `RectangleSubmitter` moved up from the version folders. `settings.gradle` includes the module
  folders directly.
- **The catalog names the Minecraft version.** Its Minecraft block lost the `-26-3` suffixes:
  `minecraft`, `fabric-api`, `neoforge` and `forge`. Each module's build reads the version from
  the `minecraft` entry and still names its jar `+26.3`; `platforms/minecraft.gradle` is gone.
- **The Release workflow** takes the one mod jar from `platforms/{loader}/build/libs/`.
