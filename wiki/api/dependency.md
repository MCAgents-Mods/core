# Dependency

A mod compiles against `mcagents-core-api` and `mcagents-core-common` as `compileOnly`, and the
`mcagents_core` mod supplies both in game. A mod that opens holograms adds
`mcagents-core-client` the same way. This page says which artifact holds what, and how to reach
them.

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
    compileOnly 'io.github.mcagentsmods:mcagents-core-api:{version}'
    compileOnly 'io.github.mcagentsmods:mcagents-core-common:{version}'
    // Only to open holograms, and only in a module that compiles against Minecraft.
    compileOnly 'io.github.mcagentsmods:mcagents-core-client:{version}'
}
```

`compileOnly`, because every core loader jar already carries `api`, `common`,
`platforms/shared` and `platforms/client`. Bundling your own copy puts two of each class on the
classpath. GitHub Packages asks for credentials even to read, so the token needs
`read:packages`.

`mcagents-core-client` names Minecraft's client classes and declares no dependency of its own,
so the module that uses it brings Minecraft 26.2 through its own toolchain, and calls it only
from code that runs on the client.

`compileOnly` does not reach the test classpath. A module with tests repeats the same two
coordinates as `testImplementation`.

## What each artifact holds

| Artifact | Packages | Holds | Page |
|---|---|---|---|
| `mcagents-core-api` | `…core.api`, `.chat`, `.llm`, `.token`, `.model`, `.http`, `.web`, `.hologram` | Contracts only: interfaces, records, enums. No implementation. | Every page below |
| `mcagents-core-common` | `…core.common`, `.model`, `.http`, `.web`, `.hologram` | The implementations: `MCAgentsProvider`, `OpenRouterClient`, `JdkHttpTransport`, `CoreWebRegistry`, and the hologram's placement and layout arithmetic, which `mcagents-core-client` uses. | [`provider.md`](provider.md), [`model-client.md`](model-client.md), [`http.md`](http.md), [`web-console.md`](web-console.md) |
| `mcagents-core-common`, test fixtures | `…core.common.testing` | `StubTransport` and `StubKeySource`, for a consumer's tests. | [`test-fixtures.md`](test-fixtures.md) |
| `mcagents-core-shared` | `…core.shared` and below | What the loader entry points start: sides, the shared key file, the token commands. | [`mod-runtime.md`](mod-runtime.md), [`key-file.md`](key-file.md), [`token-commands.md`](token-commands.md) |
| `mcagents-core-client` | `…core.client.hologram` | `Holograms`, which opens and closes holograms, and the drawing and input behind it. Client only; compiled against Minecraft 26.2. | [`hologram.md`](hologram.md), [`hologram-panels.md`](hologram-panels.md) |
| `mcagents-core-fabric`, `-neoforge`, `-forge` | `…core.{loader}` | One mod jar per loader, for a development run that needs core loaded. | — |

Every package is prefixed `io.github.mcagentsmods`. Every artifact is published under the
group `io.github.mcagentsmods` with the same version; `project-version` in core's
`gradle.properties` is that version.

## In game

The player installs the core jar for their loader (`MCAgentsCoreFabric`,
`MCAgentsCoreNeoforge` or `MCAgentsCoreForge`), mod id `mcagents_core`. A mod built on core
declares that mod id as a dependency in its own manifest.

## Related

- How the modules depend on each other and what each jar bundles:
  [`../information/modules.md`](../information/modules.md).
- A task-oriented walkthrough from key to reply:
  [`../guides/llm-providers.md`](../guides/llm-providers.md).
