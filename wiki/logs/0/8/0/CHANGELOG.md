# 0.8.0

Released: 2026-10-16

Core supports only the latest Minecraft release, 26.3. The 26.2 jars are gone, and the
artifact ids no longer carry the Minecraft version.

## Upgrading

- **Players on Minecraft 26.2 stay on 0.7.0.** Core 0.8.0 ships one jar per loader, for 26.3:
  `MCAgentsCoreFabric-0.8.0+26.3.jar`, `MCAgentsCoreNeoforge-0.8.0+26.3.jar` and
  `MCAgentsCoreForge-0.8.0+26.3.jar`. No later release will support 26.2 again.
- **Drop the Minecraft version from core's artifact ids.** `mcagents-core-client-26.3`
  becomes `mcagents-core-client`, and the same for `mcagents-core-fabric`, `-neoforge` and
  `-forge`. `mcagents-core-api`, `mcagents-core-common` and `mcagents-core-shared` are
  unchanged. See [`wiki/api/dependency.md`](../../../../api/dependency.md).
- **Development clients drop the version too**: `./gradlew :platforms:{loader}:runClient`.

## Changed

- Each Minecraft-facing module is one Gradle subproject, `:platforms:{module}`, built from its
  version folder `platforms/{module}/26.3/`. Moving to the next Minecraft release replaces that
  folder, the catalog block and the version in `settings.gradle`. See
  [`wiki/information/modules.md`](../../../../information/modules.md#the-minecraft-version).
- The Release workflow attaches three jars, one per loader.

## Removed

- Minecraft 26.2: its subprojects, its catalog block (Fabric API 0.157.0+26.2, NeoForge
  26.2.0.90, Forge 26.2-65.1.3), its `RectangleSubmitter` and `AttackListener`, and its Forge
  `pack.mcmeta`.
- The `-26.2` and `-26.3` artifact ids. Their published versions stay on GitHub Packages.
