# Test Fixtures

`mcagents-core-common` publishes two fakes for a consumer's tests: `StubTransport`, an
`HttpTransport` that answers from a script, and `StubKeySource`, an `ApiKeySource` that
records which keys were rejected. With them, the [model client](model-client.md)'s retries,
key rotation and JSON handling run without a network or an API key.

```groovy
dependencies {
    testImplementation testFixtures('io.github.mcagentsmods:mcagents-core-common:{version}')
}
```

From 0.5.1 on. 0.5.0 published the fixtures under a capability that notation does not ask
for. The fixtures are never bundled into a loader jar.

```java
StubTransport transport = new StubTransport()
        .reply(401, "{}")
        .reply(200, "{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}");
StubKeySource keys = new StubKeySource("dead-key", "good-key");

ChatCompletion answer = new OpenRouterClient(transport, keys)
        .complete(new ChatRequest("openai/gpt-4o-mini", List.of(PromptMessage.user("hello"))))
        .join();

assertEquals(Set.of("dead-key"), keys.rejected());
assertEquals(2, transport.callCount());
```

Both are in `io.github.mcagentsmods.core.common.testing`.

## StubTransport

| Method | Meaning |
|---|---|
| `reply(status, body)` | Queues an answer. |
| `fail(Throwable)` | Queues a transport failure: a dropped connection, a DNS failure, a timeout. |
| `refuse(RuntimeException)` | Queues a refusal thrown by `send` itself, before anything is sent. |
| `calls()`, `lastCall()`, `callCount()` | What the client sent, in order. |
| `isClosed()` | Whether the client closed it. |

A request beyond the script fails with an `AssertionError`, so a client that sends more than
the test expected fails the test.

## StubKeySource

| Method | Meaning |
|---|---|
| `StubKeySource(String... keys)` | Gateway keys, handed out in order, skipping rejected ones. |
| `forPlatform(platform, key)` | Adds a key that answers only for that platform. A platform with none answers empty and never borrows another's. |
| `rejected()` | Every key the client rejected. |
