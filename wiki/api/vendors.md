# Vendors

The four services [`MCAgentsProvider`](provider.md) can call, the credentials that reach one,
and what a vendor says about its models. All in `io.github.mcagentsmods.core.api.llm`.

```java
LlmCredentials credentials = LlmCredentials.of(LlmVendor.OPENAI, key)
        .withTimeout(Duration.ofSeconds(30))
        .withHeader("OpenAI-Organization", org);
agents.register(credentials);
```

## LlmVendor

`OPENROUTER`, `OPENAI`, `DEEPSEEK`, `ANTHROPIC`.

| Method | Meaning |
|---|---|
| `code()` | The stable name used in config files and the shared key file: `openrouter`, `openai`, `deepseek`, `anthropic`. Fixed per constant, so renaming a constant cannot break a deployment's config. |
| `defaultBaseUrl()` | The service's endpoint, without a trailing slash. |
| `usesOpenAiDialect()` | `true` for the first three, `false` for Anthropic. |
| `fromCode(String)` | Resolves a config value, ignoring case and surrounding whitespace. |

## LlmCredentials

| Component | Type | Meaning |
|---|---|---|
| `vendor()` | `LlmVendor` | Which service these reach. |
| `apiKey()` | `String` | The secret. Never blank. |
| `baseUrl()` | `String` | The endpoint. Trailing slashes are stripped. |
| `timeout()` | `Duration` | Bounds one HTTP exchange. This path does not retry; the [model client](model-client.md) does. Defaults to `DEFAULT_TIMEOUT`, 60 seconds. |
| `headers()` | `Map<String,String>` | Extra headers on every request. An unmodifiable copy. |

Build with `LlmCredentials.of(vendor, apiKey)` or `of(vendor, apiKey, baseUrl)`, then
`withTimeout(…)` and `withHeader(…)`.

`isWellFormedKey(String)` says whether a key is printable ASCII with no space. Credentials, the
shared key file and the token commands all refuse anything else, without repeating it.

**`toString()` redacts the API key.** The record's generated one would print it, and game logs
get pasted in public.

## ModelInfo

What `listModels` returns: `id()`, `name()`, `contextLength()` (`-1` when unreported; see
`hasContextLength()`) and `vendor()`. Only the fields all four vendors agree on are carried;
pricing and modality flags are vendor-specific and change without notice.

## LlmClient and AbstractLlmClient

The per-vendor transport contract and its shared bookkeeping. You never implement or hold
either: `MCAgentsProvider` owns every client, and the implementations are package-private.
They are public only because they are types in the published `api` jar.
