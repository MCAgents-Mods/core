# Global Models

The global model is what every task without a model of its own uses: one chat model, one
embedding model, each with the provider it is called through, and the custom providers the
player added. The player sets it on chat's **Models** page; every MCAgents mod reads it
through `Models`, in `io.github.mcagentsmods.core.common.model` (`core-common`). The records
are in `io.github.mcagentsmods.core.api.model` (`core-api`).

```java
// A task that lets the player pick its own model keeps that choice, or null for none.
ModelChoice chat = Models.global().chatOr(ownChatOrNull);
client.complete(new ChatRequest(chat.model(), messages).withProvider(chat.provider()));

ModelChoice embedding = Models.global().embeddingOr(ownEmbeddingOrNull);
client.embed(embedding, text);
```

Read `Models.global()` per request rather than holding a copy, so a save on the Models page
reaches every task at once. What a `ModelChoice` and a `ModelProvider` are, and how a chosen
provider is called, is on [`model-routing.md`](model-routing.md).

## GlobalModels

`record GlobalModels(ModelChoice chat, ModelChoice embedding, List<ModelProvider> customProviders)`.

| Member | Meaning |
|---|---|
| `DEFAULTS` | `deepseek/deepseek-v4-flash` and `openai/text-embedding-3-small` (`DEFAULT_CHAT_MODEL`, `DEFAULT_EMBEDDING_MODEL`), both through OpenRouter, no custom providers. |
| `chatOr(own)`, `embeddingOr(own)` | `own` when the task set one, else the global choice. The fallback rule; `ModelChoice.orElse(own, fallback)` is the same for any pair. |
| `providers()`, `providerIds()` | Every provider a request can go to: OpenRouter, the built-in platforms (a custom provider with a built-in's id in its place), then the other custom providers in the order added. |
| `provider(id)` | That provider, if a request can go there. |
| `isCustom(id)` | Whether the player added it. |
| `withChat(choice)`, `withEmbedding(choice)` | A copy with another choice. |
| `withProvider(provider)` | A copy with that custom provider added, or replacing the one with its id. |
| `withoutProvider(id)` | A copy without it. A choice that named a removed provider moves to OpenRouter, unless the id is a built-in platform's, which still has its own address. |
| `routes(Supplier<GlobalModels>)` | Static. `ProviderRoutes` that look a chosen provider up in `providers()` per request, and send a request that chose none through OpenRouter. |

Custom providers are one per id. One named `openrouter` is dropped: the gateway's address is
fixed, so its key never goes anywhere else.

## Models

| Member | Meaning |
|---|---|
| `global()` | The global model now. |
| `chat(own)`, `embedding(own)` | `global().chatOr(own)` and `global().embeddingOr(own)`. |
| `routes()` | `ProviderRoutes` over the global model, for a model client's constructor. |
| `store()` | The `GlobalModelStore` behind it, for the page that edits it. |
| `start(Path file)` | Reads the global model from `file`. Only the first call does anything, and returns `true`. `AgentsRuntime.start` calls it with `AgentsPaths.modelsFile()`; a mod that needs the global model before core starts may call it first with the same file. Until it runs, the global model is `DEFAULTS` in memory. |

## GlobalModelStore

| Member | Meaning |
|---|---|
| `current()` | The global model now. |
| `save(models)` | Makes it current at once and writes it off the calling thread. The future fails if the file could not be written; the settings still apply until the game closes. |
| `saved()` | Whether the player's global model was ever saved. `false` means `current()` is `DEFAULTS`: the moment a mod that kept model settings of its own before this store existed moves them in, once. |
| `routes()` | `GlobalModels.routes(this::current)`. |
| `inMemory(initial)` | Static. A store that is never written, for tests. |

## The file

`{game_dir}/MCAgents/agents/models.properties`, beside the agents [`settings.md`](settings.md):

```properties
chat.provider=deepseek
chat.model=deepseek/deepseek-v4-flash
embedding.provider=openrouter
embedding.model=openai/text-embedding-3-small
provider.homelab=http://127.0.0.1:8080/v1
```

A missing model falls back to its default; a model with no platform at a direct provider is
read as `{provider}/{model}`; a `provider.` line with a bad id or an address that is neither
https nor loopback is skipped. No API key is ever written here: keys stay in the store of the
mod that asked for them, under the provider's id.
