# Dependency

A mod compiles against `mcagents-core-api` and `mcagents-core-common` as `compileOnly`, and the
`mcagents_core` mod supplies both in game. This page says which artifact holds what, and how
to reach them.

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
}
```

`compileOnly`, because every core loader jar already carries `api`, `common` and
`platforms/shared`. Bundling your own copy puts two of each class on the classpath. GitHub
Packages asks for credentials even to read, so the token needs `read:packages`.

`compileOnly` does not reach the test classpath. A module with tests repeats the same two
coordinates as `testImplementation`.

## What each artifact holds

| Artifact | Packages | Holds | Page |
|---|---|---|---|
| `mcagents-core-api` | `…core.api`, `.chat`, `.llm`, `.token`, `.model`, `.http`, `.web` | Contracts only: interfaces, records, enums. No implementation. | Every page below |
| `mcagents-core-common` | `…core.common`, `.model`, `.http`, `.web` | The implementations: `MCAgentsProvider`, `OpenRouterClient`, `JdkHttpTransport`, `CoreWebRegistry`. | [`provider.md`](provider.md), [`model-client.md`](model-client.md), [`http.md`](http.md), [`web-console.md`](web-console.md) |
| `mcagents-core-common`, test fixtures | `…core.common.testing` | `StubTransport` and `StubKeySource`, for a consumer's tests. | [`test-fixtures.md`](test-fixtures.md) |
| `mcagents-core-shared` | `…core.shared` and below | What the loader entry points start: sides, the shared key file, the token commands. | [`mod-runtime.md`](mod-runtime.md), [`key-file.md`](key-file.md), [`token-commands.md`](token-commands.md) |
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
