# Model Routing

How the [model client](model-client.md) decides where a request goes and which key it sends.
A stored model name is `{platform}/{model}`, lowercase. `ProviderRoutes` turns it into a
`ProviderRoute`: through OpenRouter, or straight to the platform's own API. `ApiKeySource`
then supplies the key for that platform. Anything unknown goes through OpenRouter, so routing
never fails. All types are in `io.github.mcagentsmods.core.api.model`.

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

## ProviderRoute and ProviderRoutes

`ProviderRoute(platform, baseUrl, modelId)` is where one request goes.
`throughOpenRouter(storedModelName)` and `direct(ModelName, baseUrl)` build one;
`isThroughOpenRouter()`, `chatUrl()` (`{base}/chat/completions`) and `embeddingsUrl()`
(`{base}/embeddings`) read it.

`ProviderRoutes` is one method, `route(storedModelName)`, returning a
`CompletableFuture<ProviderRoute>`. `ProviderRoutes.throughOpenRouter()` sends everything
through the gateway. A mod that stores per-platform keys and URLs implements its own, and
falls back to the gateway for anything it cannot resolve.

## ApiKeySource

| Method | Meaning |
|---|---|
| `next()` | The gateway key to use next, or empty when the player has none active. |
| `next(platform)` | The key for that platform. By default it answers only for `openrouter`, so a source holding one list of gateway keys never sends one to another host. A source that stores keys per platform overrides it. |
| `reject(key)` | The provider rejected this key. Deactivate it rather than delete it, so the player can see what happened. |

The rotation rules that call `reject` are on [`model-client.md`](model-client.md).
