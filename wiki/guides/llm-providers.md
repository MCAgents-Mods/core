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

The core does not retry. `LlmCredentials.timeout()` bounds one exchange, and
that is the whole of its patience — a retry policy belongs to the mod that
knows whether the work is worth repeating.

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

## Shut down

Close the provider when your mod shuts down:

```java
agents.close();
```

Closing shuts every client down without waiting for in-flight requests, so it
will not stall the game thread. Requests still running fail; the provider is
not reusable afterwards.
