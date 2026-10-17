# 0.13.0

Released: 2026-10-17

A request now goes to the provider the player picked, and core holds the global model every
MCAgents mod falls back to. A model choice is a provider and a model; a direct provider is
called at its own address even with no key stored, and fails saying which key is missing,
instead of going through OpenRouter without a word. Chat 0.12.0's Models page edits the global
model and adds custom providers.

## Upgrading

- **Players: replace the core jar.** The `v0.13.0` release carries
  `MCAgentsCoreForge-0.13.0+26.3.jar`, `MCAgentsCoreFabric-0.13.0+26.3.jar` and
  `MCAgentsCoreNeoforge-0.13.0+26.3.jar`; install the one for your loader. Chat 0.12.0 needs
  this release.
- **Mods built on core: nothing breaks.** Every 0.12 call still compiles and routes as before.
  `ChatRequest` and `AgentRequest` gain a `provider` component; their old constructors stay and
  leave it empty, which keeps today's routing. A record pattern over either must add the
  component.

## Added

- **`ModelChoice(provider, model)`** and **`ModelProvider(id, baseUrl)`** in `core.api.model`.
  `ModelChoice.qualified` stores an id typed without a platform as `{provider}/{id}`;
  `ModelChoice.orElse(own, fallback)` is the fallback rule. See
  [`wiki/api/model-routing.md`](../../../../api/model-routing.md).
- **Routing by provider.** `ProviderRoutes.route(ModelChoice)`, `ChatRequest.withProvider`,
  `AgentRequest.of(ModelChoice, prompt)` and `ModelClient.embed(ModelChoice, text)`. A direct
  provider is sent the bare id when the model is its own (`deepseek/deepseek-v4-flash` →
  `deepseek-v4-flash`) and the whole name otherwise; OpenRouter always gets the whole name. A
  provider with no known address fails asking for it to be added as a custom provider.
- **The global model:** `GlobalModels` (chat and embedding choices and the player's custom
  providers, with `chatOr(own)`, `embeddingOr(own)`, `providers()`, `withProvider`,
  `withoutProvider` and `routes(...)`), `GlobalModelStore`, and the access point
  `core.common.model.Models` (`global()`, `store()`, `routes()`, `chat(own)`,
  `embedding(own)`). It is kept in `{game_dir}/MCAgents/agents/models.properties`
  (`AgentsPaths.modelsFile()`), read when core starts. A custom provider named like a built-in
  one replaces its address. See [`wiki/api/global-models.md`](../../../../api/global-models.md).
