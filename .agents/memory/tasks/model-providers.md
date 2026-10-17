---
name: memory-tasks-model-providers
description: Routing each request to the provider the player picked, custom providers, and the global model every task falls back to; released as 0.13.0.
---

# Task: model providers and the global model

**Status:** done (2026-10-17), released as 0.13.0

## The request

On 2026-10-17 the owner asked, as the third of four tasks: "In model GUI page. If provider set
as what it is then use that provider url. Also make it can add custom too, multiple as that.
This model page will be global model if each task not set their specific model then fallback
to these model. Plan then work immediately." The attached picture showed chat's Models page
with provider `deepseek` and the note "Called through OpenRouter". The owner had already asked
for every branch to be opened as a pull request and rebase-merged in order.

Core's part: route a request by the provider it names, the records for a provider and a model
choice, and the global model store every mod reads. Chat's part (the Models page fields, custom
providers, migrating its stored models) is chat's own task record, `model-providers.md`,
released against this release.

## Decisions

- **The provider is its own setting, separate from the model id.** A `ModelChoice` is a
  provider (the API called: `openrouter`, a built-in platform, or a custom one) and a stored
  model name, `{platform}/{model}` as the naming rule keeps it.
- **A direct provider is called even with no key**, and the request fails saying which key is
  missing. Before, a platform with no key quietly went through OpenRouter.
- **What a direct call names:** the bare id when the stored name's platform is the provider,
  else the whole stored name. OpenRouter always gets the whole name.
- **Custom providers:** an id (one lowercase segment) and a base URL (https, or http on
  loopback). One named like a built-in platform replaces that platform's URL.
- **The global model lives in core**, in `{game}/MCAgents/agents/models.properties`, behind
  `GlobalModelStore`; `Models` is the access point. A task with no model of its own uses
  `GlobalModels.chatOr(own)` / `embeddingOr(own)`.
- **Requests carry their provider:** `ChatRequest` and `AgentRequest` gain an optional
  provider (the old constructors stay), `ModelClient.embed(ModelChoice, text)` is added with a
  default, and `ProviderRoutes.route(ModelChoice)` resolves it. A request with no provider is
  routed as before.
- **A minor release, 0.13.0:** new API.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/model-providers-plan` | #63 |
| 2 | Routing by provider | `feat/provider-routing` | #64 |
| 3 | Global models | `feat/global-models` | #65 |
| 4 | Release 0.13.0 | `release/0.13.0` | #66 |

### Task 1 — chore/model-providers-plan

This record, and its row in the memory index.

### Task 2 — feat/provider-routing

`ModelChoice` (provider and stored model, `qualified`, `orElse`) and `ModelProvider` (id and
address, `builtIns()`) in `core-api`. `ProviderRoute.to(provider, model)` names the model the
way the provider does; `ProviderRoutes.route(ModelChoice)` goes to that provider and fails
naming it when no address is known. `ChatRequest` and `AgentRequest` carry an optional
provider (old constructors kept), `ModelClient.embed(ModelChoice, text)` is a default method,
`OpenRouterClient` routes a chosen provider exactly and `ToolAgent` sends every step to the
question's provider. `ChosenProviderTest` and `ModelChoiceTest` cover it; the routing, agent,
exchange and client pages say so.

### Task 3 — feat/global-models

`GlobalModels` (chat and embedding choices, custom providers; `chatOr`/`embeddingOr`,
`providers()`, `withProvider`/`withoutProvider`, `routes(...)`) and `GlobalModelStore`
(`current`, `save`, `saved`, `inMemory`) in `core-api`. `Models` in `core-common` is the access
point over `{game_dir}/MCAgents/agents/models.properties` (`AgentsPaths.modelsFile()`), read by
`AgentsRuntime.start` or by a mod that starts it first; `ModelSettingsFile` reads and writes it,
skipping what it cannot use. `GlobalModelsTest` and `ModelSettingsFileTest` cover it;
`wiki/api/global-models.md` documents it. Chat moves its stored models in and edits it from its
Models page.

### Task 4 — release/0.13.0

`project-version` 0.13.0, `wiki/logs/0/13/0/CHANGELOG.md`, its row in the logs index, and the
version in the repository state, rules, map and overview.
