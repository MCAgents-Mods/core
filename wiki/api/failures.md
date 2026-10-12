# Failures

Core fails a future with one of two unchecked exceptions, one per way in:
[`MCAgentsProvider`](provider.md) fails with `AgentException`, and the
[model client](model-client.md) fails with `ModelException`. Read the failure off the cause of
the failed future:

```java
client.complete(request).exceptionally(error -> {
    Throwable cause = error instanceof CompletionException ? error.getCause() : error;
    if (cause instanceof ModelException failure && failure.kind() == ModelException.Kind.NO_KEY) {
        tellPlayer("Add an API key first.");
    }
    return null;
});
```

A `null` or malformed argument is the exception to the failed-future rule: it throws at once,
because it is a programming error rather than a remote failure.

## AgentException

`io.github.mcagentsmods.core.api.AgentException`, from the provider and its vendor clients.

| Method | Meaning |
|---|---|
| `vendor()` | Which service the failing call was aimed at. |
| `statusCode()` | The HTTP status, or `AgentException.NO_STATUS` (`-1`) when no answer came. |
| `isAuthFailure()` | HTTP 401. A 403 refuses the request, not the key. |
| `isRateLimited()` | HTTP 429, the one failure where retrying later is reasonable. |

The message includes the text the vendor put in its error body, so a 400 says what was wrong
rather than only its number.

## ModelException

`io.github.mcagentsmods.core.api.model.ModelException`, from the model client. `kind()` says
what to do; `isRetryable()` is true for `RATE_LIMIT`, `SERVER` and `TRANSPORT`.

| Kind | Meaning | The client already |
|---|---|---|
| `AUTHENTICATION` | Every key tried was rejected (401), or out of credit (402). | Rotated through up to five keys. |
| `RATE_LIMIT` | Rate limited (429). | Retried, three attempts in total. |
| `SERVER` | The provider failed (5xx). | Retried. |
| `TRANSPORT` | The network failed, or nothing answered. | Retried. |
| `REQUEST` | The request was wrong: a bad model id, a malformed body, or a 403. | Nothing; retrying cannot help. |
| `MALFORMED_RESPONSE` | The answer came back but could not be read. | Nothing; it was already billed, and a retry bills again. |
| `NO_KEY` | No usable key for the platform. | Nothing. |
| `CLOSED` | The client was closed before the request finished. | Nothing; the same client cannot help. |

`ModelException.Kind.forStatus(status)` is the mapping from an HTTP error status: 401 and 402
are `AUTHENTICATION`, 429 is `RATE_LIMIT`, 5xx is `SERVER`, anything else is `REQUEST`.
