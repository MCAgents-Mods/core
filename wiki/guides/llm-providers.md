# Using the Language Model Providers

How to get from an API key to a model's reply, on any platform the core
supports. For the full surface, see [`../reference/api.md`](../reference/api.md).

## Add the dependency

```groovy
repositories {
    maven {
        url = 'https://maven.pkg.github.com/MCAgents-Mods/core'
        credentials {
            username = System.getenv('GITHUB_ACTOR')
            password = System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    compileOnly 'io.github.mcagentsmods:core-api:0.1.0'
    compileOnly 'io.github.mcagentsmods:core-common:0.1.0'
}
```

`compileOnly` because the core mod jar already carries both at runtime.
Shading your own copy would put two of each class on the classpath.

## Register the vendors you have keys for

Do this once, when your mod starts:

```java
MCAgentsProvider agents = MCAgentsProvider.create();
agents.registerOpenRouter(config.getString("openrouter.key"));
agents.registerAnthropic(config.getString("anthropic.key"));
```

For a single vendor, skip a step:

```java
MCAgentsProvider agents = MCAgentsProvider.openRouter(key);
```

Registering opens no connection. A bad key surfaces on the first real call, so
check it deliberately if you want to fail loudly at startup:

```java
agents.ping(LlmVendor.OPENROUTER).thenAccept(ok -> {
    if (!ok) {
        getLogger().warning("OpenRouter rejected the configured key");
    }
});
```

Registering a vendor that is already registered replaces it and closes the old
client. That is how you rotate a key from a reload command without a restart.

## Ask something

The short form gives you the reply text:

```java
agents.askAnthropic("claude-opus-4", "Name this village in three words.")
      .thenAccept(name -> getLogger().info(name));
```

The long form gives you everything — framing instructions, sampling settings,
token counts:

```java
ChatRequest request = ChatRequest.builder("claude-opus-4")
        .system("You are a terse Minecraft villager. Reply in one sentence.")
        .user("A player just sold you 12 emeralds. What do you say?")
        .maxTokens(120)
        .temperature(0.8)
        .build();

agents.chatAnthropic(request).thenAccept(reply -> {
    getLogger().info(reply.content());
    getLogger().info("cost: " + reply.usage().totalTokens() + " tokens");
});
```

## Two rules that will bite you if you skip them

### Nothing is remembered

The provider stores no conversation, no cache, and no per-player state. That is
deliberate: `core` is shared by several mods, and a core that quietly
accumulated history would leak memory in all of them.

So a conversation is yours to keep and replay:

```java
List<ChatMessage> history = new ArrayList<>();
history.add(ChatMessage.user(playerMessage));

ChatRequest request = ChatRequest.builder(model).messages(history).build();

agents.chatOpenAi(request).thenAccept(reply -> {
    history.add(reply.asMessage());   // keep the model's turn for next time
});
```

Bound that list yourself. An unbounded history grows until the model rejects it
for length, and one list per player adds up.

### Nothing blocks, and nothing promises you a thread

Every call returns immediately and does its work off your thread, so calling one
from a tick is safe. **The completion is not.** Nothing guarantees which thread
a future finishes on, so a callback that touches the world, an entity, or an
inventory has to hop back onto the game thread first:

```java
agents.askOpenAi(model, prompt).thenAccept(reply ->
        minecraft.execute(() -> player.displayClientMessage(Component.literal(reply), false)));
```

On a client that means the client thread; on a server, the server thread.
Getting this wrong produces an intermittent crash under load and nothing at all in testing.

## Handling failure

Failures arrive as a failed future whose cause is always an `AgentException`:

```java
agents.askDeepSeek(model, prompt)
      .thenAccept(this::show)
      .exceptionally(failure -> {
          AgentException cause = (AgentException) failure.getCause();
          if (cause.isRateLimited()) {
              getLogger().warning("Rate limited by " + cause.vendor().code() + ", try later");
          } else if (cause.isAuthFailure()) {
              getLogger().severe("Key rejected by " + cause.vendor().code());
          } else {
              getLogger().warning(cause.getMessage());
          }
          return null;
      });
```

The message carries the vendor's own error text, so an HTTP 400 tells you what
was actually wrong.

`MCAgentsProvider` does not retry. `LlmCredentials.timeout()` bounds one
exchange, and that is the whole of its patience. If you want retries, use the
routed model client below, which retries the failures worth retrying.

## What differs per vendor

| Vendor | Model identifiers look like | Worth knowing |
|---|---|---|
| OpenRouter | `anthropic/claude-opus-4` | Namespaced by provider. `ChatResponse.model()` reports whoever actually served the request, which may not be who you asked for. Attribution goes in `HTTP-Referer` and `X-Title` headers — add them with `withHeader`. |
| OpenAI | `gpt-4o-mini` | The reference dialect. The model catalog reports ids only, so `ModelInfo.name()` falls back to the id and `contextLength()` is unreported. |
| DeepSeek | `deepseek-chat` | OpenAI-compatible; same behavior as above. |
| Anthropic | `claude-opus-4` | The one vendor with its own dialect. Your `systemPrompt` and any `SYSTEM` messages are merged into the top-level field its API expects. A token bound is mandatory there, so **4096 is applied when you set none** — set `maxTokens` explicitly if that is not what you want. `ModelInfo.contextLength()` is always unreported. |

None of this changes how you call the provider. The differences are absorbed by
the clients; they are listed here only because they show up in what comes back.

## Point somewhere else

To route through a proxy, a gateway, or a local model server that speaks the
OpenAI dialect:

```java
agents.register(LlmCredentials
        .of(LlmVendor.OPENAI, key, "https://gateway.example.com/v1")
        .withTimeout(Duration.ofSeconds(120))
        .withHeader("X-Tenant", "survival-1"));
```

## Tools, embeddings, and caching: the routed model client

`MCAgentsProvider` sends a conversation and returns text. When a mod needs more
— the model calling tools, embeddings for retrieval, prompt caching, retries,
or keys that rotate on their own — use `OpenRouterClient` instead:

```java
ApiKeySource keys = myKeyStore;                       // your storage, your encryption
ModelClient models = new OpenRouterClient(
        new JdkHttpTransport(), keys, ProviderRoutes.throughOpenRouter(),
        new OpenRouterClient.Attribution("https://github.com/you/your-mod", "YourMod"));

ChatRequest request = new ChatRequest("openai/gpt-4o-mini", List.of(
        PromptMessage.system(persona).asCacheBreakpoint(),
        PromptMessage.user(question)));

models.complete(request).thenAccept(answer -> {
    if (answer.wantsTools()) {
        // run answer.toolCalls(), then send request.continuedWith(…) with
        // PromptMessage.assistantToolCalls(…) and one PromptMessage.toolResult(…) per call
    } else {
        minecraft.execute(() -> show(answer.text()));
    }
});
```

Core never stores keys. Your mod implements `ApiKeySource`: `next(platform)`
hands over the key to use, and `reject(key)` is called when a provider refuses
one with a 401 or 402, so you can mark it inactive. A 403 is a refused request,
not a dead key. If you implement only `next()`, those keys are used for
OpenRouter and nothing else. The client tries up to five keys per request.

Model names follow `{platform}/{model}`, lowercase — `ModelName` builds and
reads them. A `ProviderRoutes` decides whether a request goes through
OpenRouter or straight to the platform; the default sends everything through
OpenRouter. A direct base URL must be `https`, or `http` on a loopback address,
so a key is never sent in the clear to another machine.

Put everything stable at the front of the message list and mark the end of it
with `asCacheBreakpoint()`. `answer.usage().cacheHitRate()` tells you whether
the provider reused it.

Failures arrive as a `ModelException` whose `kind()` says what happened. Rate
limits, server errors, and dropped connections are retried up to three times
with backoff before you see them.

Close the client when your mod shuts down. That aborts requests in flight and
fails every request waiting to retry with `ModelException.Kind.CLOSED`, so no
future is left hanging. A switch over `Kind` needs a case for it.

## Shut down

Close the provider when your mod shuts down:

```java
agents.close();
```

Closing shuts every client down without waiting for in-flight requests, so it
will not stall the game thread. Requests still running fail; the provider is
not reusable afterwards.
