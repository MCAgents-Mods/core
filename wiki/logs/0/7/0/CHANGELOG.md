# 0.7.0

Released: 2026-10-13

Core runs on Minecraft 26.3 as well as 26.2. Every loader jar is now built once per
Minecraft version, so a player picks the jar for their loader and their game.

## Upgrading

- **Pick the jar for your Minecraft version.** The jars are named
  `MCAgentsCore{Loader}-0.7.0+{minecraft}.jar`: `MCAgentsCoreFabric-0.7.0+26.3.jar` for Fabric
  on 26.3, and so on. Each declares its own Minecraft version and refuses the other.
- **The Minecraft-facing artifacts carry the Minecraft version.** `mcagents-core-client`
  becomes `mcagents-core-client-26.2` and `mcagents-core-client-26.3`, and the same for
  `mcagents-core-fabric`, `-neoforge` and `-forge`. A mod that opens holograms depends on the
  one for the Minecraft version it builds against; see
  [`wiki/api/dependency.md`](../../../../api/dependency.md). `mcagents-core-api`,
  `mcagents-core-common` and `mcagents-core-shared` are unchanged.
- **Building core needs Gradle on Java 25.** Fabric Loom 1.18, the first for 26.3, refuses an
  older JVM. The wrapper is Gradle 9.7.1.

## Added

- **Minecraft 26.3**, on Fabric (Loader 0.19.5, Fabric API 0.162.0+26.3), NeoForge 26.3.0.76
  and Forge 26.3-66.0.11. Holograms look and behave as on 26.2.
- **One Gradle subproject per Minecraft version** for `platforms/client` and each loader,
  `platforms/{module}/{minecraft}`, with the versions listed once in `settings.gradle`. Code
  that differs between versions is a class of the same name in each version folder. See
  [`wiki/information/modules.md`](../../../../information/modules.md).
- **Development clients per version**: `./gradlew :platforms:{loader}-{minecraft}:runClient`.

## Changed

- The jar names gain `+{minecraft}`, and the Release workflow attaches six jars, one per
  loader and Minecraft version.
- Fabric Loader 0.19.5 is the oldest the Fabric jars accept, for 26.2 as well.
