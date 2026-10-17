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
| [`../../wiki/api/model-routing.md`](../../wiki/api/model-routing.md) | `ModelName`, `ModelPlatform`, `ModelChoice`, `ModelProvider`, `ProviderRoutes` and `ApiKeySource`: where a request goes and with which key. |
| [`../../wiki/api/http.md`](../../wiki/api/http.md) | The HTTP seam and `JdkHttpTransport`'s limits. |
| [`../../wiki/api/failures.md`](../../wiki/api/failures.md) | `AgentException` and `ModelException`: what a failed future means and what to do. |
| [`../../wiki/api/test-fixtures.md`](../../wiki/api/test-fixtures.md) | `StubTransport` and `StubKeySource`, for a consumer's tests. |

## Tools

| File | Purpose |
|---|---|
| [`../../wiki/api/tool-registry.md`](../../wiki/api/tool-registry.md) | `ToolRegistry`: registering a tool, its categories, and the handle that removes it. |
| [`../../wiki/api/tool-spec.md`](../../wiki/api/tool-spec.md) | `ToolSpec` and `ToolSchema`: what the model is told about a tool and its arguments. |
| [`../../wiki/api/tool-arguments.md`](../../wiki/api/tool-arguments.md) | `ToolArguments` and `ToolResult`: reading what the model sent and answering it. |
| [`../../wiki/api/agent.md`](../../wiki/api/agent.md) | `Agents.agent`, `Agent`, `AgentRequest`, `AgentAnswer` and `ToolAgent`: asking the agent a question it answers with the tools it needs. |
| [`../../wiki/api/settings.md`](../../wiki/api/settings.md) | `AgentSettings` and `Agents.update`: the worker limit and tool rounds the player sets, and their file. |
| [`../../wiki/api/tool-execution.md`](../../wiki/api/tool-execution.md) | `Tool`, `AsyncTool`, `ToolExecutor` and `WorkerPool`: how calls run, in what order, and on how many workers. |

## In the game

| File | Purpose |
|---|---|
| [`../../wiki/api/mod-runtime.md`](../../wiki/api/mod-runtime.md) | `CoreRuntime`, the physical sides, and the two halves a loader starts. |
| [`../../wiki/api/key-file.md`](../../wiki/api/key-file.md) | The shared `mcagents.json`, where it is resolved, and `SharedTokenStore`. |
| [`../../wiki/api/token-commands.md`](../../wiki/api/token-commands.md) | `ModTokenCommands` and the server's permission checks. |
| [`../../wiki/api/hologram.md`](../../wiki/api/hologram.md) | `Hologram` and `Holograms`: opening and closing a hologram, and what the player does with it. |
| [`../../wiki/api/buttons.md`](../../wiki/api/buttons.md) | `AgentButton`, `Agents.buttons()`, the agents menu G shows, and the G key. |
| [`../../wiki/api/hologram-panels.md`](../../wiki/api/hologram-panels.md) | The panel kinds a hologram is made of: `ButtonGrid`, `SelectList`, `PickList`, `Transcript`, `PromptInput` and `Form`, and typing into a prompt or a form. |
