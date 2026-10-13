# Local Setup

## Requirements

* **git**
* **A JDK** to run Gradle with. The build itself compiles on **Java 25**, but you
  do not have to install it: the Gradle toolchain support downloads a Java 25 JDK
  the first time you build, using the
  [foojay resolver](https://github.com/gradle/foojay-toolchains) configured in
  `settings.gradle`.
* **Network access** on the first build, to fetch Gradle 9.5.0, the Java 25
  toolchain, the libraries from Maven Central, the three loader toolchains from
  the Fabric, NeoForged and MinecraftForge mavens, and Minecraft itself from
  Mojang. The first build decompiles Minecraft for NeoForge and Forge, which takes
  a few minutes; later builds reuse the cache.

Gradle itself does not need to be installed — use the wrapper (`./gradlew`)
checked into the repository.

## Get a working copy

```sh
git clone https://github.com/MCAgents-Mods/core.git
cd core
```

The default branch is `master`.

## Build

```sh
./gradlew build
```

That compiles every module, runs the tests, and produces one jar per module under
`{module}/build/libs/`. The three that matter are the mod jars:

| Loader | Jar |
|---|---|
| Forge | `platforms/forge/build/libs/MCAgentsCoreForge-{version}.jar` |
| NeoForge | `platforms/neoforge/build/libs/MCAgentsCoreNeoforge-{version}.jar` |
| Fabric | `platforms/fabric/build/libs/MCAgentsCoreFabric-{version}.jar` |

Drop the one matching your loader into the game's `mods/` folder. Fabric also
needs Fabric API.

Useful variants:

```sh
./gradlew :api:build                 # one module only
./gradlew :platforms:fabric:build    # one loader's jar
./gradlew clean                      # delete the root build directory
./gradlew javaToolchains             # show which JDK the build resolved
```

## Run a development client

Each loader module starts a development client with core loaded, in that module's own
`run/` directory, which git ignores:

```sh
./gradlew :platforms:fabric:runClient
./gradlew :platforms:neoforge:runClient
./gradlew :platforms:forge:runClient
```

The client loads the mod the way its jar would: `api`, `common` and `shared` are part of
`mcagents_core`, not separate pieces. NeoForge is told so through the mod's source sets;
Forge treats the directory holding `mods.toml` as the whole mod, so its run task first
copies every bundled module into `platforms/forge/build/devMod/` and starts the game from
there. The first run of each loader downloads the game's assets.

## The request timeouts

Through `MCAgentsProvider`, one request either answers or fails within 60 seconds —
`LlmCredentials.DEFAULT_TIMEOUT`. **This is the only place a timeout is
defined.** Consumer mods have no timeout setting, pass none, and cannot override
it, so a consumer can rely on its future completing without owning a timer of its
own.

A timeout is not a credential failure. Nothing is evicted and nothing is retried:
a model that has not answered in this long is unlikely to answer sooner on a
second attempt, and retrying doubles the bill.

The routed client, `OpenRouterClient`, sends through `JdkHttpTransport`, which
allows 10 seconds to connect and 120 seconds for an answer. There a timeout is a
`TRANSPORT` failure and **is** retried, up to three attempts, so a request can
take up to six minutes before its future fails.

## Test

```sh
./gradlew test                       # every module
./gradlew :common:test               # one module
./gradlew test --tests '*TokenHandles*'   # one class
```

Tests are **JUnit 5**, wired once in the root `build.gradle` so every module has
a test source set without asking for one. A module with no `src/test/java`
reports `NO-SOURCE` and costs nothing.

Test sources live beside the code they cover, at
`{module}/src/test/java/{same package}`. Only what the module compiles against
is on the test classpath: modules declare `api` and `common` as `compileOnly`,
which does not reach tests, so a module with tests repeats those as
`testImplementation` in its own build file.

A run writes an HTML report to `{module}/build/reports/tests/test/index.html`
and the machine-readable results to `{module}/build/test-results/test/`.

Nothing runs the tests on a pull request — the Publish workflow below builds only
when a release reaches `master` — so run this command before opening one.

## Publish the modules

Every module is published to GitHub Packages as
`io.github.mcagentsmods:mcagents-core-{module}` — `api`, `common`, `shared`,
`forge`, `neoforge` and `fabric`:

```sh
./gradlew publish
```

Publishing reads the `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables for
credentials. To try a release locally without publishing anywhere public:

```sh
./gradlew publishToMavenLocal
```

### How a release is published

Nobody runs `./gradlew publish` by hand for a release. The **Publish** workflow,
`.github/workflows/publish.yml`, runs on every push to `master`:

1. It reads `project-version` from `gradle.properties` and looks for the tag
   `v{version}` on GitHub. If the tag exists, the run stops: a merge that leaves the
   version alone publishes nothing.
2. Otherwise it builds and tests every module, publishes all six to GitHub Packages,
   and only then tags the commit `v{version}`.

So a release is the merge that changes `project-version`. A failed run leaves no tag,
and the next push to `master` — or **Run workflow** on the Publish workflow's page in
the Actions tab — tries again. GitHub Packages refuses to overwrite a version, so if
a run failed after some modules were already uploaded, delete those package versions
before retrying.

## Start a piece of work

Never commit to `master`. Branch from it, named `{type}/{primary-noun}`:

```sh
git checkout master
git pull origin master
git checkout -b docs/my-change
```

When the work is done:

```sh
git push -u origin docs/my-change
```

Then open one pull request for the branch. The branch types, commit message
format, pull request title and body rules, and the merge procedure are defined in
`.agents/` — start from [`../../AGENTS.md`](../../AGENTS.md).

## Environment variables

| Variable | Read by | Purpose |
|---|---|---|
| `GITHUB_ACTOR` | `build.gradle` | Username for the GitHub Packages repository. |
| `GITHUB_TOKEN` | `build.gradle` | Token for the GitHub Packages repository. |

Both are only needed when running `./gradlew publish`; the Publish workflow sets
them from the run's own token. Nothing else in the repository reads the
environment.
