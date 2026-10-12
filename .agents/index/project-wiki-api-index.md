---
name: project-wiki-api-index
description: Index of wiki/api/ — the public API of every core module, one page per scope, so a reader opens only the API it is about to use.
---

# Project Wiki API Index

**Scope:** `wiki/api/`
**Parent:** [project-wiki-index](project-wiki-index.md)

Plain markdown, no frontmatter, written for a person or an agent about to call core. One
scope per page; a page that would need an unrelated heading is split.

## Start here

| File | Purpose |
|---|---|
| [`../../wiki/api/dependency.md`](../../wiki/api/dependency.md) | Which artifact to depend on, how, and which page covers each one. |

## Calling a model

| File | Purpose |
|---|---|
| [`../../wiki/api/provider.md`](../../wiki/api/provider.md) | `MCAgentsProvider`: one client per vendor, registered once, for plain chat. |
| [`../../wiki/api/chat-exchange.md`](../../wiki/api/chat-exchange.md) | The provider's request and reply types: `ChatRequest`, `ChatResponse`, `ChatMessage`, `TokenUsage`. |
| [`../../wiki/api/vendors.md`](../../wiki/api/vendors.md) | `LlmVendor`, `LlmCredentials` and `ModelInfo`. |
| [`../../wiki/api/tokens.md`](../../wiki/api/tokens.md) | `TokenStore` and `TokenState`: handing core a list of keys to rotate and evict. |
| [`../../wiki/api/model-client.md`](../../wiki/api/model-client.md) | `ModelClient` and `OpenRouterClient`: tools, embeddings, caching, retries, key rotation. |
| [`../../wiki/api/model-exchange.md`](../../wiki/api/model-exchange.md) | The model client's request, message, tool and answer types. |
| [`../../wiki/api/model-routing.md`](../../wiki/api/model-routing.md) | `ModelName`, `ModelPlatform`, `ProviderRoutes` and `ApiKeySource`: where a request goes and with which key. |
| [`../../wiki/api/http.md`](../../wiki/api/http.md) | The HTTP seam and `JdkHttpTransport`'s limits. |
| [`../../wiki/api/failures.md`](../../wiki/api/failures.md) | `AgentException` and `ModelException`: what a failed future means and what to do. |
| [`../../wiki/api/test-fixtures.md`](../../wiki/api/test-fixtures.md) | `StubTransport` and `StubKeySource`, for a consumer's tests. |

## In the game

| File | Purpose |
|---|---|
| [`../../wiki/api/mod-runtime.md`](../../wiki/api/mod-runtime.md) | `CoreRuntime`, the physical sides, and the two halves a loader starts. |
| [`../../wiki/api/key-file.md`](../../wiki/api/key-file.md) | The shared `mcagents.json`, where it is resolved, and `SharedTokenStore`. |
| [`../../wiki/api/token-commands.md`](../../wiki/api/token-commands.md) | `ModTokenCommands` and the server's permission checks. |
| [`../../wiki/api/web-console.md`](../../wiki/api/web-console.md) | The loopback web console mods register pages into. |
