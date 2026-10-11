# API Reference

Every public type in `mcagents-api` and `mcagents-common`, and what each one is
for. If you only read one section, read
[`MCAgentsProvider`](#mcagentsprovider) — it is the whole surface.

For a task-oriented walkthrough instead, see
[`../guides/llm-providers.md`](../guides/llm-providers.md).

## Coordinates

```groovy
dependencies {
    compileOnly 'io.github.mcagents:mcagents-api:0.1.0'
    compileOnly 'io.github.mcagents:mcagents-common:0.1.0'
}
```

Published to GitHub Packages at `https://maven.pkg.github.com/MCAgents/core`.

## MCAgentsProvider

`io.github.mcagents.core.common.MCAgentsProvider` — the single entry point. It
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

`io.github.mcagents.core.api.chat`

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
`.user(…)`, and `.assistant(…)` factories. `ChatRole` is `SYSTEM`, `USER`, or
`ASSISTANT`; each carries a fixed wire `code()`, and `fromCode` resolves one
case-insensitively.

### TokenUsage

`promptTokens()`, `completionTokens()`, `totalTokens()` — each `-1` when the
vendor did not report it. `TokenUsage.UNKNOWN` is what you get when nothing was
reported at all, so a response's usage is never `null`. `isKnown()` checks the
total.

`totalTokens()` is not always the sum of the other two: cached and reasoning
tokens can be billed differently.

## The vendor layer

`io.github.mcagents.core.api.llm`

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
| `timeout()` | `Duration` | Bounds one HTTP exchange, not a retry sequence — the core does not retry. Defaults to 60s (`DEFAULT_TIMEOUT`). Carried on the credential template, so it survives every rotation. |
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

## Failure

`io.github.mcagents.core.api.AgentException` — unchecked, and the cause of every
failed future the core hands back.

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
