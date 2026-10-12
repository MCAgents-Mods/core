# Chat Exchange

The request and reply types [`MCAgentsProvider`](provider.md) sends and returns: a
`ChatRequest` in, a `ChatResponse` out, built from `ChatMessage`s. They live in
`io.github.mcagentsmods.core.api.chat`. The [model client](model-client.md) has its own
request and reply types, on [`model-exchange.md`](model-exchange.md); the two are not
interchangeable.

```java
ChatRequest request = ChatRequest.builder("openai/gpt-4o-mini")
        .system("Answer in one sentence.")
        .user("What is a creeper?")
        .maxTokens(200)
        .build();

agents.chat(LlmVendor.OPENROUTER, request).thenAccept(response -> {
    String text = response.content();
    ChatMessage next = response.asMessage(); // replay it in the next request
});
```

## ChatRequest

Immutable. Build one with `ChatRequest.builder(model)`, or `ChatRequest.of(model, prompt)` for
one user turn.

| Component | Type | Meaning |
|---|---|---|
| `model()` | `String` | The vendor's model identifier. Never blank. |
| `messages()` | `List<ChatMessage>` | The conversation, oldest first. Never empty; an unmodifiable copy. |
| `systemPrompt()` | `String` | Framing instructions, or empty. Kept out of `messages` because vendors disagree about where it goes on the wire. |
| `maxTokens()` | `int` | Token bound, or `-1` for the vendor's default. |
| `temperature()` | `double` | `0.0` to `2.0`, or `NaN` for the vendor's default. |
| `topP()` | `double` | Above `0.0` and at most `1.0`, or `NaN` for the vendor's default. |
| `stopSequences()` | `List<String>` | Strings that end generation. An unmodifiable copy. |

Optional settings use a sentinel rather than a boxed type. `hasMaxTokens()`,
`hasTemperature()`, `hasTopP()` and `hasSystemPrompt()` say whether one was set.

Builder methods: `system`, `user`, `assistant`, `message`, `messages`, `maxTokens`,
`temperature`, `topP`, `stop`, `build`.

The constructor validates at once: a blank model, an empty message list, or an out-of-range
sampling setting throws `IllegalArgumentException`.

## ChatResponse

| Component | Type | Meaning |
|---|---|---|
| `id()` | `String` | The vendor's identifier for the exchange, or empty. |
| `model()` | `String` | The model that answered. Not always the one asked for, since OpenRouter routes. |
| `content()` | `String` | The reply text. Never `null`. |
| `finishReason()` | `String` | Why generation stopped, in the vendor's own words. A `String` because the vocabulary differs per vendor and grows without notice. |
| `usage()` | `TokenUsage` | What it cost. |

`isTruncated()` says whether generation hit the token limit rather than finishing.
`asMessage()` returns the reply as an assistant turn, ready to replay in the next request.

## ChatMessage and ChatRole

`ChatMessage(ChatRole role, String content)`, with `ChatMessage.system(…)`, `.user(…)` and
`.assistant(…)` factories.

`ChatRole` is `SYSTEM`, `USER`, `ASSISTANT` or `TOOL`. Each carries a fixed wire `code()`, and
`fromCode` resolves one case-insensitively. A `ChatMessage` refuses `TOOL` with
`IllegalArgumentException`: a tool result must name the call it answers, and only the model
client's `PromptMessage` has a field for that.

## TokenUsage

`promptTokens()`, `completionTokens()`, `totalTokens()`, each `-1` when the vendor did not
report it. `TokenUsage.UNKNOWN` stands in when nothing was reported, so a response's usage is
never `null`. `isKnown()` checks the total.

`totalTokens()` is not always the sum of the other two: cached and reasoning tokens can be
billed differently.
