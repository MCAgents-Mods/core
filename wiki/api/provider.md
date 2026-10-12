# Provider

`MCAgentsProvider` is one object that holds a client per vendor (OpenRouter, OpenAI, DeepSeek,
Anthropic) and sends each call to the right one. Create it once, register the vendors you
have keys for, then call `chat` or `ask`. For tool calling, embeddings, prompt caching or
retries, use the [model client](model-client.md) instead.

```java
MCAgentsProvider agents = MCAgentsProvider.create();
agents.registerOpenRouter(key);

agents.ask(LlmVendor.OPENROUTER, "openai/gpt-4o-mini", "Say hello")
        .thenAccept(reply -> LOGGER.info(reply));
```

`io.github.mcagentsmods.core.common.MCAgentsProvider` implements the contract
`io.github.mcagentsmods.core.api.AgentProvider`. The vendor clients behind it are
package-private; a consumer never holds one.

## Create one

| Method | Returns | What it does |
|---|---|---|
| `MCAgentsProvider()` | — | Empty provider. Installs itself as `instance`. |
| `create()` | `MCAgentsProvider` | Same, as a static call. |
| `create(LlmCredentials...)` | `MCAgentsProvider` | Registers each vendor in order. |
| `openRouter(String apiKey)` | `MCAgentsProvider` | Serving OpenRouter alone. |
| `openAi(String apiKey)` | `MCAgentsProvider` | Serving OpenAI alone. |
| `deepSeek(String apiKey)` | `MCAgentsProvider` | Serving DeepSeek alone. |
| `anthropic(String apiKey)` | `MCAgentsProvider` | Serving Anthropic alone. |

`public static volatile MCAgentsProvider instance` holds the most recently constructed
provider. It is never cleared, so a provider you closed stays there and fails calls with a
clear message rather than becoming a `null` in someone else's code. The token commands
register the shared key file into this instance; see [`token-commands.md`](token-commands.md).

## Register vendors

| Method | Returns | What it does |
|---|---|---|
| `register(LlmCredentials)` | `void` | Adds a vendor. Replacing one closes the client it replaced, which is how a key is rotated without a restart. |
| `registerOpenRouter(String apiKey)` | `void` | `register` with OpenRouter's default endpoint. |
| `registerOpenAi(String apiKey)` | `void` | Same, for OpenAI. |
| `registerDeepSeek(String apiKey)` | `void` | Same, for DeepSeek. |
| `registerAnthropic(String apiKey)` | `void` | Same, for Anthropic. |
| `unregister(LlmVendor)` | `boolean` | Removes a vendor and closes its client. `false` when nothing was registered. |
| `isRegistered(LlmVendor)` | `boolean` | Whether that vendor can be called. |
| `registeredVendors()` | `Set<LlmVendor>` | Unmodifiable snapshot. |

Registering opens no connection, so a bad key is only discovered on the first real call, or
on `ping`. To let core own a list of keys and rotate through them instead of one fixed key,
register a store: [`tokens.md`](tokens.md). The credential type is on
[`vendors.md`](vendors.md).

## Call a model

| Method | Returns | What it does |
|---|---|---|
| `chat(LlmVendor, ChatRequest)` | `CompletableFuture<ChatResponse>` | One exchange. |
| `ask(LlmVendor, String model, String prompt)` | `CompletableFuture<String>` | One prompt, reply text only. |
| `listModels(LlmVendor)` | `CompletableFuture<List<ModelInfo>>` | What that vendor offers now. Not cached. |
| `ping(LlmVendor)` | `CompletableFuture<Boolean>` | Reachable and key accepted. Resolves `false` instead of failing, also for an unregistered vendor. |
| `close()` | `void` | Closes every client and clears the registry. Idempotent; the provider is not reusable afterwards. |

The request and reply types are on [`chat-exchange.md`](chat-exchange.md).

Named per vendor, if you prefer them to passing the enum:

| Vendor | Prompt only | Full exchange |
|---|---|---|
| OpenRouter | `askOpenRouter(model, prompt)` | `chatOpenRouter(request)` |
| OpenAI | `askOpenAi(model, prompt)` | `chatOpenAi(request)` |
| DeepSeek | `askDeepSeek(model, prompt)` | `chatDeepSeek(request)` |
| Anthropic | `askAnthropic(model, prompt)` | `chatAnthropic(request)` |

## Threads and memory

Every remote call returns a `CompletableFuture` and works off the calling thread; it may
complete on any thread, so a callback that touches game state hops back onto the game thread
first. Registration and lookup are safe from any thread.

The provider keeps no conversation, cache or per-player state. History is the caller's to
keep and replay: `ChatResponse.asMessage()` turns a reply into the next request's assistant
turn.

A failed future fails with `AgentException`: [`failures.md`](failures.md).
