# HTTP

The seam the [model client](model-client.md) sends requests through, so a mod can drive any
HTTP client and a test can drive none. The contract is in `io.github.mcagentsmods.core.api.http`;
`JdkHttpTransport` in `io.github.mcagentsmods.core.common.http` is the real one.

```java
HttpTransport transport = new JdkHttpTransport();
transport.send(HttpCall.post(url, Map.of("Authorization", "Bearer " + key), json))
        .thenAccept(reply -> {
            if (reply.isSuccess()) {
                handle(reply.body());
            }
        });
```

## Types

| Type | What it is |
|---|---|
| `HttpTransport` | `send(HttpCall)` returns a `CompletableFuture<HttpReply>`. `close()` releases it; the default does nothing. |
| `HttpCall(method, url, headers, body)` | One request. `HttpCall.post(url, headers, body)` builds a POST. `toString()` names the headers and never prints their values. |
| `HttpReply(status, body)` | One answer. `isSuccess()` is true for a 2xx status. |

## JdkHttpTransport

Built on the JDK's `HttpClient`, with no settings to pass.

| Limit | Value | Why |
|---|---|---|
| Connect timeout | 10 s | |
| Request timeout | 120 s; the whole exchange, body included, is abandoned after 130 s | A future must always complete. |
| Largest answer read | 8 MiB | A runaway body cannot fill the game's memory. |
| Redirects | Never followed | The JDK client would re-send `Authorization` to wherever one points. |

## Keys in headers

A key must be printable ASCII with no space (`LlmCredentials.isWellFormedKey`). Credentials, the
shared key file and the token commands refuse anything else without repeating it, so the HTTP
client never gets a header it would refuse with a message quoting the key.
