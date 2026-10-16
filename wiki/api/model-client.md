# Model Client

`OpenRouterClient` is the client for a mod that needs tool calling, embeddings, prompt
caching, retries or per-request routing. Give it a transport, a key source and, optionally,
routes; it then rotates keys, retries what is worth retrying, and sends each request to
OpenRouter or straight to the platform. It is what core's own [agent](agent.md) and
`MCAgents-Mods/chat` call. For one fixed key per vendor and plain chat,
[`MCAgentsProvider`](provider.md) is simpler.

```java
ModelClient client = new OpenRouterClient(new JdkHttpTransport(), keys);

client.complete(new ChatRequest("openai/gpt-4o-mini", List.of(PromptMessage.user("Hello"))))
        .thenAccept(completion -> LOGGER.info(completion.text()));
```

`io.github.mcagentsmods.core.api.model.ModelClient` is the contract;
`io.github.mcagentsmods.core.common.model.OpenRouterClient` implements it.

## ModelClient

| Method | Returns | Meaning |
|---|---|---|
| `complete(ChatRequest)` | `CompletableFuture<ChatCompletion>` | Sends a conversation and returns the answer, or the tools the model wants run first. |
| `embed(String model, String text)` | `CompletableFuture<float[]>` | Embeds text for retrieval. |
| `close()` | `void` | Closes the transport and the retry scheduler, and fails every request waiting on a backoff as `CLOSED`. Safe to call twice. |

The request, message, tool and answer types are on [`model-exchange.md`](model-exchange.md).

## Constructing OpenRouterClient

| Constructor | Use it when |
|---|---|
| `OpenRouterClient(HttpTransport, ApiKeySource)` | Everything goes through OpenRouter, attributed to core. |
| `OpenRouterClient(HttpTransport, ApiKeySource, ProviderRoutes)` | Some models go straight to their platform. |
| `OpenRouterClient(HttpTransport, ApiKeySource, ProviderRoutes, Attribution)` | Your mod names itself to OpenRouter, or sends no attribution. |

| Argument | Type | What it supplies |
|---|---|---|
| `transport` | `HttpTransport` | Sends a request. `JdkHttpTransport` is the real one; see [`http.md`](http.md). |
| `keys` | `ApiKeySource` | The key to use next, per platform, and where to report a rejected one; see [`model-routing.md`](model-routing.md). |
| `routes` | `ProviderRoutes` | Where a stored model name goes. Defaults to `ProviderRoutes.throughOpenRouter()`. |
| `attribution` | `OpenRouterClient.Attribution` | The `HTTP-Referer` and `X-Title` headers OpenRouter is sent. Defaults to `Attribution.CORE` (`https://github.com/MCAgents-Mods/core`, `MCAgents`); `Attribution.NONE` sends neither. A value with a control character is refused. |

## What it does for you

| Behaviour | Detail |
|---|---|
| Key rotation | A 401 or 402 rejects the key through `ApiKeySource.reject` and tries the next, up to five keys per request. A 403 refuses the request, not the key, and fails as `REQUEST`. |
| Retries | A rate limit, a server error or a transport failure is retried, three attempts in total, with exponential backoff from 500 ms and jitter. A bad request and a malformed answer are never retried; the second was already billed. |
| Routing | Through OpenRouter the whole `{platform}/{model}` name is sent, with the gateway's extensions (`cache_control`, `usage.include`) and the attribution headers. Straight to a platform the bare model id is sent, with none of those, because the platform rejects them. |
| Shutdown | Backoffs wait on one daemon thread, so the game can exit with a retry pending. `close()` fails every request still waiting. |

A failed future fails with `ModelException`, whose kind says what to do next:
[`failures.md`](failures.md).
