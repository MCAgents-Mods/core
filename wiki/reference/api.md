# API Reference

Every public type in `core-api` and `core-common`, and what each one is
for. There are two ways in:
[`MCAgentsProvider`](#mcagentsprovider), one registered client per vendor, and
the [routed model client](#the-routed-model-client), for tools, embeddings,
prompt caching, and retries. Pages for the local web console go through
[the web console](#the-web-console).

For a task-oriented walkthrough instead, see
[`../guides/llm-providers.md`](../guides/llm-providers.md).

## Coordinates

```groovy
dependencies {
    compileOnly 'io.github.mcagentsmods:core-api:0.1.0'
    compileOnly 'io.github.mcagentsmods:core-common:0.1.0'
}
```

Published to GitHub Packages at `https://maven.pkg.github.com/MCAgents-Mods/core`.

## MCAgentsProvider

`io.github.mcagentsmods.core.common.MCAgentsProvider` — the single entry point. It
is the only public class in `common`; everything behind it is package-private
and cannot be reached from outside.

### Creating one

| Method | Returns | What it does |
|---|---|---|
| `MCAgentsProvider()` | — | Empty provider. Installs itself as `instance`. |
| `create()` | `MCAgentsProvider` | Same, as a static call. |
| `create(LlmCredentials...)` | `MCAgentsProvider` | Registers each vendor in order. |
| `openRouter(String apiKey)` | `MCAgentsProvider` | Serving OpenRouter alone. |
| `openAi(String apiKey)` | `MCAgentsProvider` | Serving OpenAI alone. |
| `deepSeek(String apiKey)` | `MCAgentsProvider` | Serving DeepSeek alone. |
| `anthropic(String apiKey)` | `MCAgentsProvider` | Serving Anthropic alone. |

`public static volatile MCAgentsProvider instance` holds the most recently
constructed provider. It is never cleared, so a provider you closed stays there
and fails calls with a clear message rather than becoming a `null` in someone
else's code.

### Registering vendors

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

Registering opens no connection, so a bad key is only discovered on the first
real call — or on `ping`.

### Credentials, and who owns them

Two ways to register a vendor, and the difference matters.

| Method | Returns | What it does |
|---|---|---|
| `register(LlmCredentials)` | `void` | One fixed credential. Nothing rotates; a failure goes straight back to the caller. |
| `registerStore(LlmVendor, TokenStore)` | `TokenState` | Hands core **where the credentials live**. Core owns the rest. |
| `registerStore(LlmVendor, TokenStore, LlmCredentials)` | `TokenState` | Same, reached through a non-default endpoint. The template's key is ignored; its base URL, timeout, and headers are used with every credential from the store. |
| `tokenState(LlmVendor)` | `TokenState` | `READY`, `NOT_SET`, or `EXPIRED`. |
| `reloadTokens(LlmVendor)` | `TokenState` | Re-read one vendor's store. |
| `reloadTokens()` | `int` | Re-read every store; returns how many vendors were reloaded. |

`TokenStore` itself is four methods: `load`, `add`, `evict`, `reload`, plus
`describe`. `add` and `evict` are what the mod's `/agents` client command drives.

With a store registered, core handles the whole credential lifecycle so a
consumer never sees a key again:

* a **rejected** credential (401/403) is retried on the next one, and the dead
  one is **deleted from the store**;
* a **rate limited** credential (429) is retried on the next one and **kept** —
  it is healthy, merely busy;
* **anything else** is not retried and touches no credential, because nothing
  was learned about it.

That distinction is the reason this lives in core rather than in each consumer.
Evicting a rate-limited key destroys something the user paid for, and nothing
inside a game can undo it — so it is worth writing once, correctly.

`NOT_SET` and `EXPIRED` are separate answers deliberately: both mean "no usable
credential", but one asks the owner to add a key and the other tells them their
keys stopped working.

Rotation rebuilds credentials from the vendor's stored connection settings, so a
deployment behind a proxy or a self-hosted gateway is never silently moved onto
the public endpoint.

### Calling a model

| Method | Returns | What it does |
|---|---|---|
| `chat(LlmVendor, ChatRequest)` | `CompletableFuture<ChatResponse>` | One exchange. |
| `ask(LlmVendor, String model, String prompt)` | `CompletableFuture<String>` | One prompt, reply text only. |
| `listModels(LlmVendor)` | `CompletableFuture<List<ModelInfo>>` | What that vendor currently offers. Not cached. |
| `ping(LlmVendor)` | `CompletableFuture<Boolean>` | Reachable and key accepted. Resolves `false` instead of failing, including for an unregistered vendor. |
| `close()` | `void` | Closes every client and clears the registry. Idempotent, and not reusable afterwards. |

Named per vendor, if you prefer them over passing the enum:

| Vendor | Prompt only | Full exchange |
|---|---|---|
| OpenRouter | `askOpenRouter(model, prompt)` | `chatOpenRouter(request)` |
| OpenAI | `askOpenAi(model, prompt)` | `chatOpenAi(request)` |
| DeepSeek | `askDeepSeek(model, prompt)` | `chatDeepSeek(request)` |
| Anthropic | `askAnthropic(model, prompt)` | `chatAnthropic(request)` |

## The exchange model

`io.github.mcagentsmods.core.api.chat`

### ChatRequest

Immutable, and carries everything the vendor needs. Build one with
`ChatRequest.builder(model)` or, for the trivial case,
`ChatRequest.of(model, prompt)`.

| Component | Type | Meaning |
|---|---|---|
| `model()` | `String` | The vendor's model identifier. Never blank. |
| `messages()` | `List<ChatMessage>` | The conversation, oldest first. Never empty, always an unmodifiable copy. |
| `systemPrompt()` | `String` | Framing instructions, or empty. Kept out of `messages` because vendors disagree about where it belongs on the wire. |
| `maxTokens()` | `int` | Token bound, or `-1` for the vendor's default. |
| `temperature()` | `double` | `0.0`–`2.0`, or `NaN` for the vendor's default. |
| `topP()` | `double` | Above `0.0` and at most `1.0`, or `NaN` for the vendor's default. |
| `stopSequences()` | `List<String>` | Strings that end generation. Unmodifiable copy. |

Optional settings use a sentinel rather than a boxed type, so nothing has to be
unwrapped. `hasMaxTokens()`, `hasTemperature()`, `hasTopP()`, and
`hasSystemPrompt()` tell you whether one was set.

Builder methods: `system`, `user`, `assistant`, `message`, `messages`,
`maxTokens`, `temperature`, `topP`, `stop`, `build`.

The constructor validates: a blank model, an empty message list, or an
out-of-range sampling setting throws `IllegalArgumentException` immediately.

### ChatResponse

| Component | Type | Meaning |
|---|---|---|
| `id()` | `String` | The vendor's identifier for the exchange, or empty. |
| `model()` | `String` | The model that actually answered — not always the one asked for, since OpenRouter routes. |
| `content()` | `String` | The reply text. Never `null`. |
| `finishReason()` | `String` | Why generation stopped, in the vendor's own words. A `String` rather than an enum because the vocabulary differs per vendor and grows without notice. |
| `usage()` | `TokenUsage` | What it cost. |

`isTruncated()` reports whether generation hit the token limit rather than
finishing. `asMessage()` returns the reply as an assistant turn, ready to be
replayed in the next request.

### ChatMessage and ChatRole

`ChatMessage(ChatRole role, String content)`, with `ChatMessage.system(…)`,
`.user(…)`, and `.assistant(…)` factories. `ChatRole` is `SYSTEM`, `USER`,
`ASSISTANT`, or `TOOL`; each carries a fixed wire `code()`, and `fromCode`
resolves one case-insensitively.

`TOOL` belongs to the routed model client's `PromptMessage`, which names the
call a result answers. A `ChatMessage` has nowhere to carry that id, so it
refuses `TOOL` with `IllegalArgumentException`.

### TokenUsage

`promptTokens()`, `completionTokens()`, `totalTokens()` — each `-1` when the
vendor did not report it. `TokenUsage.UNKNOWN` is what you get when nothing was
reported at all, so a response's usage is never `null`. `isKnown()` checks the
total.

`totalTokens()` is not always the sum of the other two: cached and reasoning
tokens can be billed differently.

## The vendor layer

`io.github.mcagentsmods.core.api.llm`

### LlmVendor

`OPENROUTER`, `OPENAI`, `DEEPSEEK`, `ANTHROPIC`.

| Method | Meaning |
|---|---|
| `code()` | The stable name used in config files — `"openrouter"`, `"openai"`, `"deepseek"`, `"anthropic"`. Fixed per constant, so renaming a constant cannot invalidate a deployment's config. |
| `defaultBaseUrl()` | The service's endpoint, without a trailing slash. |
| `usesOpenAiDialect()` | `true` for the first three, `false` for Anthropic. |
| `fromCode(String)` | Resolves a config value, ignoring case and whitespace. |

### LlmCredentials

| Component | Type | Meaning |
|---|---|---|
| `vendor()` | `LlmVendor` | Which service these reach. |
| `apiKey()` | `String` | The secret. Never blank. |
| `baseUrl()` | `String` | The endpoint. Trailing slashes are stripped. |
| `timeout()` | `Duration` | Bounds one HTTP exchange, not a retry sequence — this path does not retry; the [routed model client](#the-routed-model-client) does. Defaults to 60s (`DEFAULT_TIMEOUT`). Carried on the credential template, so it survives every rotation. |
| `headers()` | `Map<String,String>` | Extra headers on every request. Unmodifiable copy. |

Build with `LlmCredentials.of(vendor, apiKey)` or
`of(vendor, apiKey, baseUrl)`, then `withTimeout(…)` and `withHeader(…)` for the
rest.

**`toString()` redacts the API key.** The record's generated one would print it,
and Minecraft server logs get pasted in public.

### ModelInfo

`id()`, `name()`, `contextLength()` (`-1` when unreported, see
`hasContextLength()`), and `vendor()`. Only the fields all four vendors agree on
are carried — pricing and modality flags are vendor-specific and change without
notice.

### LlmClient and AbstractLlmClient

The per-vendor transport contract and its shared bookkeeping. You will not
implement or hold either one — `MCAgentsProvider` owns every client and the
implementations are package-private. They are documented here because they are
public types in the published `api` jar, not because there is anything to do
with them.

## The routed model client

A second way to call a model, for a mod that needs tool calling, embeddings,
prompt caching, retries, or per-request routing. It is the client
`MCAgents-Mods/chat` was built on, moved here so every mod shares it.

### ModelClient and OpenRouterClient

`io.github.mcagentsmods.core.api.model.ModelClient` is the contract;
`io.github.mcagentsmods.core.common.model.OpenRouterClient` implements it.

| Method | Returns | Meaning |
|---|---|---|
| `complete(ChatRequest)` | `CompletableFuture<ChatCompletion>` | Sends a conversation and returns the answer, or the tools the model wants run first. |
| `embed(String model, String text)` | `CompletableFuture<float[]>` | Embeds text for retrieval. |
| `close()` | | Closes the transport and stops pending retries. |

A client is built from three seams, so nothing in it knows where keys or
settings live:

| Constructor argument | Type | What it supplies |
|---|---|---|
| `transport` | `HttpTransport` | Sends a request. `JdkHttpTransport` is the real one: 10s connect timeout, 120s request timeout. |
| `keys` | `ApiKeySource` | The key to use next, per platform, and where to report one the provider rejected. |
| `routes` | `ProviderRoutes` | Where a stored model name goes. Defaults to `ProviderRoutes.throughOpenRouter()`. |
| `attribution` | `OpenRouterClient.Attribution` | The `HTTP-Referer` and `X-Title` OpenRouter is sent. Defaults to `Attribution.CORE`; `Attribution.NONE` sends neither. A value with a control character is refused. |

What it does so callers do not have to:

* **Key rotation.** A 401, 403, or 402 rejects the key through
  `ApiKeySource.reject` and tries the next, up to five keys per request.
* **Retries.** A rate limit, a server error, or a transport failure is retried
  up to three attempts in total, with exponential backoff and jitter. A bad
  request and a malformed answer are never retried — the second has already
  been billed.
* **Routing.** Through OpenRouter the whole `{platform}/{model}` name is sent,
  with the gateway's own extensions (`cache_control`, `usage.include`) and the
  attribution headers. Directly to a platform the bare model id is sent, with
  none of those, because the platform rejects them.

### The request and the answer

`io.github.mcagentsmods.core.api.model`

| Type | What it is |
|---|---|
| `ChatRequest(model, messages, tools)` | One request. `continuedWith(messages)` keeps the model and tools for the next round of a tool exchange. |
| `PromptMessage(role, text, cacheBreakpoint, toolCalls, toolCallId)` | One message on the wire. `system`, `user`, `assistant`, `assistantToolCalls`, and `toolResult` build one; `asCacheBreakpoint()` asks the provider to cache the prefix ending here. |
| `ToolDefinition(name, description, parametersSchema)` | A tool offered to the model. The name must be 1–64 letters, digits, `-`, or `_`, because a provider rejects the whole request over one bad name. |
| `ToolCall(id, name, arguments)` | The model asking for a tool. `arguments` stays the raw JSON text. |
| `ChatCompletion(text, model, usage, toolCalls)` | The answer. `wantsTools()` is true when it asks for tools instead. |
| `TokenUsage(promptTokens, completionTokens, cachedTokens)` | What it cost. `hadCacheHit()` and `cacheHitRate()` say whether caching worked. |

### Naming and routing

| Type | What it is |
|---|---|
| `ModelName` | A stored model id in the shape `{platform}/{model}`, lowercase. `of`, `parse`, and `normalise` build and read one. |
| `ModelPlatform` | Platform ids, OpenRouter's base URL, the known direct base URLs, and `isUsableBaseUrl` — `https`, or a loopback `http` address. |
| `ProviderRoute` | Where one request goes: a platform, a base URL, and the model id to send. |
| `ProviderRoutes` | Resolves a stored name to a route. It never fails; anything unknown goes through OpenRouter. |
| `ApiKeySource` | `next()`, `next(platform)`, and `reject(key)`. |

### When it fails

`ModelException` — unchecked, and the cause of every failed future this client
hands back. Its `kind()` says what to do:

| Kind | Meaning | Retried by the client |
|---|---|---|
| `AUTHENTICATION` | Every key tried was rejected. | Rotated, not retried. |
| `RATE_LIMIT` | Rate limited. | Yes. |
| `SERVER` | The provider failed. | Yes. |
| `TRANSPORT` | Nothing answered. | Yes. |
| `REQUEST` | The request was wrong. | No. |
| `MALFORMED_RESPONSE` | The answer could not be read. | No — it was already billed. |
| `NO_KEY` | No usable key for the platform. | No. |

### The transport

`io.github.mcagentsmods.core.api.http` holds `HttpTransport`, `HttpCall`, and
`HttpReply`, so a mod can drive any HTTP client through the same seam.
`JdkHttpTransport` is in `io.github.mcagentsmods.core.common.http`.

### Test fixtures

`core-common` publishes test fixtures in
`io.github.mcagentsmods.core.common.testing`: `StubTransport` answers from a
script and records each call, and `StubKeySource` holds keys per platform and
records which were rejected. Depend on them with
`testImplementation testFixtures('io.github.mcagentsmods:core-common:{version}')`.

## The web console

One loopback server that every mod registers its pages into. How to write a
module and its pages: [`../guides/web-pages.md`](../guides/web-pages.md).

### WebRegistry and CoreWebRegistry

`io.github.mcagentsmods.core.api.web.WebRegistry`, implemented by
`io.github.mcagentsmods.core.common.web.CoreWebRegistry`. A mod uses
`CoreWebRegistry.shared()`, the one instance in the game.

| Method | Meaning |
|---|---|
| `register(WebModule)` | Adds a module. Throws `IllegalArgumentException` for an invalid or reserved id or a resource root with a leading slash, no trailing slash, or `..`; `IllegalStateException` for an id already registered. |
| `unregister(String id)` | Removes one. `false` when nothing had that id. |
| `moduleIds()` | Every registered id, in registration order. |
| `open()` | The running console, starting one if none runs. Throws `WebConsoleException` when no port can be opened. |
| `current()` | The running console, if any. |
| `close()` | Stops the running console and keeps the registrations. Core calls it at shutdown. |

### WebModule

| Method | Meaning |
|---|---|
| `id()` | `[a-z0-9_]{1,32}`, and not `core`, `api` or `assets`. The prefix of every path the module owns. |
| `title()` | The module's name on the console's front page and in the header. |
| `navigation()` | `List<NavLink>` — the header's links, in order. |
| `page(String path)` | The resource for `/{id}/{path}`, or empty for a 404. |
| `resourceRoot()` | The classpath folder pages and assets are read from, such as `mcagents/web/`. |
| `resourceLoader()` | The class loader they are read through. The module's own by default. |
| `handle(WebRequest)` | Answers `/api/{id}/…`. Defaults to a 404. |

`NavLink(label, path, section)` — `path` is relative to the module and may not
start with `/`, contain `..`, a scheme, a query or a fragment. `section`
defaults to the path's first segment.

`WebRequest(method, path, query, body)` — `is(method, path)` and
`parameter(name)` read it.

`WebResponse(status, contentType, body)` — `ok(json)`, `json(status, json)`,
`error(status, message)`, and `quote(text)` for a JSON string literal.

### WebConsole

One run of the server.

| Method | Meaning |
|---|---|
| `url()` | The front page, `/core/`, with the token. |
| `url(String moduleId)` | That module's first navigation link, with the token. |
| `port()` | The loopback port. |
| `isRunning()` | Whether it still listens. |
| `close()` | Stops it, giving requests in flight two seconds. Safe to call twice. |
| `stopped()` | A future that completes when it has stopped. |

A closed console is not reopened: `open()` starts a new one, on a new port, with
a new token.

What every answer carries: no `Access-Control-Allow-Origin`, `Cache-Control:
no-store`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`,
`X-Frame-Options: DENY`, `Cross-Origin-Resource-Policy: same-origin`, and a
`Content-Security-Policy` that allows only the console's own scripts, styles,
images and requests.

## Failure

`io.github.mcagentsmods.core.api.AgentException` — unchecked, and the cause of every
failed future `MCAgentsProvider` and an `LlmClient` hand back. The routed model
client fails with `ModelException` instead — see
[When it fails](#when-it-fails).

| Method | Meaning |
|---|---|
| `vendor()` | Which service the failing call was aimed at. |
| `statusCode()` | The HTTP status, or `AgentException.NO_STATUS` (`-1`) when the call never got an answer. |
| `isAuthFailure()` | HTTP 401 or 403. |
| `isRateLimited()` | HTTP 429 — the one failure where retrying later is reasonable. |

The message includes the text the vendor put in its error body, so an HTTP 400
says what was actually wrong rather than just its number.

Argument validation is the exception to the failed-future rule: a `null` or
malformed argument throws right away, because that is a programming error rather
than a remote failure.
