# Model Exchange

The request and answer types of the [model client](model-client.md), in
`io.github.mcagentsmods.core.api.model`: a `ChatRequest` of `PromptMessage`s and
`ToolDefinition`s goes in, a `ChatCompletion` comes out, and a completion that wants tools
carries `ToolCall`s. These are not the provider's types on
[`chat-exchange.md`](chat-exchange.md), which share some names.

## A tool round

```java
ChatRequest request = new ChatRequest(model,
        List.of(PromptMessage.system(rules).asCacheBreakpoint(), PromptMessage.user(question)),
        List.of(new ToolDefinition("get_time", "The time in the world", "{\"type\":\"object\"}")));

client.complete(request).thenCompose(completion -> {
    if (!completion.wantsTools()) {
        return CompletableFuture.completedFuture(completion);
    }
    List<PromptMessage> next = new ArrayList<>(request.messages());
    next.add(PromptMessage.assistantToolCalls(completion.text(), completion.toolCalls()));
    for (ToolCall call : completion.toolCalls()) {
        next.add(PromptMessage.toolResult(call.id(), run(call)));
    }
    return client.complete(request.continuedWith(next));
});
```

## Types

| Type | What it is |
|---|---|
| `ChatRequest(model, messages, tools)` | One request. `ChatRequest(model, messages)` offers no tools. `continuedWith(messages)` keeps the model and tools for the next round of a tool exchange. |
| `PromptMessage(role, text, cacheBreakpoint, toolCalls, toolCallId)` | One message on the wire. Build with `system`, `user`, `assistant`, `assistantToolCalls(text, calls)` and `toolResult(callId, text)`. `asCacheBreakpoint()` asks the provider to cache the prefix ending here. `isToolExchange()` says whether it carries calls or a result. |
| `ToolDefinition(name, description, parametersSchema)` | A tool offered to the model; `parametersSchema` is the JSON Schema of its arguments, as text. The name is unique among the tools offered at once, and must be 1 to 64 letters, digits, `-` or `_` (`isAcceptable(name)` checks), because a provider rejects the whole request over one bad name. |
| `ToolCall(id, name, arguments)` | The model asking for a tool. `arguments` stays the raw JSON text. |
| `ChatCompletion(text, model, usage, toolCalls)` | The answer. `wantsTools()` is true when it asks for tools instead of answering. `model` is the model that answered, which may differ from the one asked for. |
| `TokenUsage(promptTokens, completionTokens, cachedTokens)` | What it cost. `totalTokens()` adds the first two; `hadCacheHit()` and `cacheHitRate()` say whether caching worked. `UNKNOWN` is all zeros. |

`PromptMessage.role` is the shared `ChatRole` from `api.chat`, including `TOOL`, which only a
`PromptMessage` can carry.
