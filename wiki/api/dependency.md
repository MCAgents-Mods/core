# Dependency

A mod compiles against `core-api` and `core-common` as `compileOnly`, and the
`mcagents_core` mod supplies both in game. A mod that opens holograms adds
`core-client` the same way, building for the Minecraft version core supports,
26.3. This page says which artifact holds what, and how to reach them.

## Add it

```groovy
repositories {
    maven {
        url = 'https://maven.pkg.github.com/MCAgents-Mods/core'
        credentials {
            username = System.getenv('GITHUB_ACTOR')
            password = System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    compileOnly 'io.github.mcagentsmods:core-api:{version}'
    compileOnly 'io.github.mcagentsmods:core-common:{version}'
    // Only to open holograms, and only in a module that compiles against Minecraft:
    // built for the one Minecraft version core supports, the latest release.
    compileOnly 'io.github.mcagentsmods:core-client:{version}'
}
```

`compileOnly`, because every core loader jar already carries `tool`, `api`, `common`,
`platforms/shared` and `platforms/client`. Bundling your own copy puts two of each class on the
classpath. GitHub Packages asks for credentials even to read, so the token needs
`read:packages`.

`core-client` names Minecraft's client classes and declares no dependency
of its own, so the module that uses it brings that Minecraft version through its own toolchain,
and calls it only from code that runs on the client. Core builds it for the latest Minecraft
release alone, and its id carries no Minecraft version, so a mod builds for that release too.
Up to 0.7.0 the Minecraft-facing ids ended in the version, as in `mcagents-core-client-26.3`.

`compileOnly` does not reach the test classpath. A module with tests repeats the same two
coordinates as `testImplementation`.

## What each artifact holds

| Artifact | Packages | Holds | Page |
|---|---|---|---|
| `core-tool` | `…core.tool` | Tools in pure Java: specs, schemas, arguments, results, the registry, the worker pool and the executor. Depends on the JDK alone, so plain Java that only needs tools names it alone, as `implementation`. | [`tool-registry.md`](tool-registry.md), [`tool-spec.md`](tool-spec.md), [`tool-arguments.md`](tool-arguments.md), [`tool-execution.md`](tool-execution.md) |
| `core-api` | `…core.api`, `.chat`, `.llm`, `.token`, `.model`, `.http`, `.hologram`, `.agent`, `.button` | Contracts only: interfaces, records, enums. No implementation. Brings `core-tool` with it. | Every page below; [`agent.md`](agent.md) for `Agent`, `AgentRequest` and `AgentAnswer` |
| `core-common` | `…core.common`, `.model`, `.http`, `.hologram`, `.agent` | The implementations: `MCAgentsProvider`, `OpenRouterClient`, `JdkHttpTransport`, the hologram's placement, layout, scrolling and text arithmetic, which `core-client` uses, and the agent layer: `Agents`, `ToolAgent`, `AgentSettings`, `AgentButtons`, `AgentsPaths`. | [`provider.md`](provider.md), [`model-client.md`](model-client.md), [`http.md`](http.md), [`agent.md`](agent.md), [`settings.md`](settings.md) |
| `core-common`, test fixtures | `…core.common.testing` | `StubTransport` and `StubKeySource`, for a consumer's tests. | [`test-fixtures.md`](test-fixtures.md) |
| `core-shared` | `…core.shared` and below | What the loader entry points start: sides, the shared key file, the token commands. | [`mod-runtime.md`](mod-runtime.md), [`key-file.md`](key-file.md), [`token-commands.md`](token-commands.md) |
| `core-client` | `…core.client.hologram`, `…core.client.agent` | `Holograms`, which opens and closes holograms, and the drawing and input behind it; the agents menu, its G key, its Settings hologram and start-up, and `ClientThreadTools`. Client only; published for the latest Minecraft release alone, compiled against it. | [`hologram.md`](hologram.md), [`hologram-panels.md`](hologram-panels.md), [`buttons.md`](buttons.md), [`tool-execution.md`](tool-execution.md#clientthreadtools) |

Every package is prefixed `io.github.mcagentsmods`. Every artifact is published under the
group `io.github.mcagentsmods` with the same version; `project-version` in core's
`gradle.properties` is that version. Before 0.9.0 each artifact id started with `mcagents-`,
as in `mcagents-core-api`. The Fabric, NeoForge and Forge mod jars are not on
GitHub Packages: each version's GitHub release carries one per loader, for a player or for a
development run that needs core loaded.

## In game

The player installs the core jar for their loader and Minecraft version
(`MCAgentsCoreFabric-{version}+{minecraft}.jar`, `MCAgentsCoreNeoforge-…` or
`MCAgentsCoreForge-…`), mod id `mcagents_core`. A mod built on core
declares that mod id as a dependency in its own manifest.

From 0.10.0 core carries what the MCAgents agents mod (`mcagents_agents`) did: the tool
registry, the agent, and the menu G shows. A mod that depended on agents depends on core alone,
and imports `io.github.mcagentsmods.core.tool`, `…core.api.agent`, `…core.api.button`,
`…core.common.agent` and `…core.client.agent` in place of the `…agents` packages; every class
kept its name. Core refuses to load beside the agents mod, on all three loaders, since the game
would otherwise have two menus on G and two tool registries that do not see each other.

From 0.6.0 core refuses to load beside MCAgents chat (mod id `mcagents`) older than 0.4.0,
on Fabric and NeoForge, the loaders chat ships for: those versions open the web console,
which core no longer has. The loader names the mod to update instead of starting the game.

## Related

- How the modules depend on each other and what each jar bundles:
  [`../information/modules.md`](../information/modules.md).
- A task-oriented walkthrough from key to reply:
  [`../guides/llm-providers.md`](../guides/llm-providers.md).
