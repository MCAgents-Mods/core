# Model Routing

How the [model client](model-client.md) decides where a request goes and which key it sends.
A stored model name is `{platform}/{model}`, lowercase. `ProviderRoutes` turns it into a
`ProviderRoute`: through OpenRouter, or straight to the platform's own API. `ApiKeySource`
then supplies the key for that platform. A request can also name its provider, a
`ModelChoice`: it then goes to that provider and nowhere else. All types are in
`io.github.mcagentsmods.core.api.model`.

```java
ModelName name = ModelName.of("openai", "gpt-4o-mini");   // openai/gpt-4o-mini
ProviderRoute direct = ProviderRoute.direct(name, ModelPlatform.knownBaseUrl("openai").orElseThrow());
ProviderRoute gateway = ProviderRoute.throughOpenRouter(name.qualified());
```

## ModelName

A stored model id: `platform` (who serves it, one segment, no `/`) and `model` (the
platform's own id). The shape is the shared model-naming convention, which is OpenRouter's own
format.

| Member | Meaning |
|---|---|
| `of(platform, platformModel)` | Builds a name, lowercasing both halves. Refuses a model id that already carries the platform prefix, rather than doubling it. |
| `parse(stored)` | Reads a stored id back, split at the first `/`. Empty for an id with no platform segment. |
| `normalise(raw)` | What to write: trimmed and lowercased, qualified or not. Apply it before writing, never when comparing. |
| `isQualified(stored)` | Whether the id has a platform segment, and so can take a direct route. |
| `qualified()` | `platform/model`: what is stored, and what OpenRouter is sent. |
| `bare()` | The platform's own id: what a direct call names. |

An unqualified id the player typed is kept as it is and goes through OpenRouter.
`openrouter/gpt-4o-mini` is not a shorter spelling of `gpt-4o-mini`; it names a model that
does not exist.

## ModelPlatform

Constants and helpers for the platform half.

| Member | Meaning |
|---|---|
| `OPENROUTER`, `OPENROUTER_BASE_URL` | `openrouter`, `https://openrouter.ai/api/v1`: the fallback for everything. |
| `knownBaseUrl(platform)`, `knownDirectBaseUrls()` | The platforms reachable without configuring a URL: `openai`, `deepseek`, `mistralai`, `groq`, `xai`, `together`. Only OpenAI-compatible APIs are listed; Anthropic and Google go through OpenRouter. |
| `baseUrlSettingKey(platform)` | `api.base-url.{platform}`: the settings key a mod stores a platform's base URL under. A stored value wins over the known one. |
| `isUsableBaseUrl(url)` | Whether a key may be sent there: `https`, or plain `http` to a loopback host for a local gateway. No user info, query or fragment. |
| `normalise(platform)`, `isOpenRouter(platform)` | Trimmed and lowercased, blank meaning OpenRouter; and whether it is the gateway. |

## ModelChoice

`record ModelChoice(String provider, String model)`: a stored model name and the provider whose
API it is called through. The provider is the route, kept apart from the name, so one model
keeps one name whichever API answers it.

| Member | Meaning |
|---|---|
| `provider` | `openrouter`, a built-in platform, or a custom provider's id. Lowercased; blank is `openrouter`. |
| `model` | The stored name, lowercased, never blank. |
| `throughOpenRouter(model)` | The model through the gateway. |
| `qualified(provider, model)` | The same, but an unqualified model at a direct provider becomes `{provider}/{model}`, as the naming rule wants. |
| `isThroughOpenRouter()`, `withModel(model)`, `withProvider(provider)` | Read and change one half. |
| `orElse(own, fallback)` | Static. `own` when a task set one, else `fallback`: the fallback rule [`global-models.md`](global-models.md) builds on. |

A chosen provider is honoured exactly. With no key stored for it the request fails with
`NO_KEY`, naming the provider; it never goes through OpenRouter instead.

## ModelProvider

`record ModelProvider(String id, String baseUrl)`: an API a request can go to. The id is one
lowercase segment of letters, digits, `.`, `-` or `_` (`isValidId`), and is what the provider's
API key is stored under. The address must pass `ModelPlatform.isUsableBaseUrl`; a trailing
slash is dropped. `openRouter()` is the gateway, `builtIns()` lists it first and then every
`ModelPlatform.knownDirectBaseUrls()` entry, and `isBuiltIn()` says whether this exact id and
address ship with core. Custom providers are the player's, kept in the global model
([`global-models.md`](global-models.md)).

## ProviderRoute and ProviderRoutes

`ProviderRoute(platform, baseUrl, modelId)` is where one request goes.
`throughOpenRouter(storedModelName)`, `direct(ModelName, baseUrl)` and
`to(ModelProvider, storedModelName)` build one. `to` names the model the way that provider
does: the whole stored name at OpenRouter; the bare id when the name's platform is the provider
(`deepseek/deepseek-v4-flash` at `deepseek` is `deepseek-v4-flash`); and the whole name at any
other provider, since a host like Together names other platforms' models `{platform}/{model}`.
`isThroughOpenRouter()`, `chatUrl()` (`{base}/chat/completions`) and `embeddingsUrl()`
(`{base}/embeddings`) read it.

`ProviderRoutes.route(storedModelName)` returns a `CompletableFuture<ProviderRoute>` for a
request that chose no provider; it never fails, and anything it cannot resolve goes through the
gateway. `route(ModelChoice)` is for a request that did: it goes to that provider and, when no
address is known for it, fails with a `REQUEST` error from `unknownProvider(id)`, which says to
add it as a custom provider. By default it knows `ModelProvider.builtIns()`;
`GlobalModels.routes(...)` also knows the player's custom providers.
`ProviderRoutes.throughOpenRouter()` sends every unchosen request through the gateway.

## ApiKeySource

| Method | Meaning |
|---|---|
| `next()` | The gateway key to use next, or empty when the player has none active. |
| `next(platform)` | The key for that platform. By default it answers only for `openrouter`, so a source holding one list of gateway keys never sends one to another host. A source that stores keys per platform overrides it. |
| `reject(key)` | The provider rejected this key. Deactivate it rather than delete it, so the player can see what happened. |

The rotation rules that call `reject` are on [`model-client.md`](model-client.md).
