# Tokens

Register a `TokenStore` for a vendor and core owns that vendor's keys from then on: it loads
them, uses the next one when a key is rejected or rate limited, and deletes a rejected key from
the store. A consumer never handles a key again. Use `register(LlmCredentials)` on
[`provider.md`](provider.md) instead when there is exactly one fixed key.

```java
TokenState state = agents.registerStore(LlmVendor.OPENROUTER, store);
if (state == TokenState.NOT_SET) {
    // ask the player to add a key
}
```

## Register a store

On `MCAgentsProvider`:

| Method | Returns | What it does |
|---|---|---|
| `registerStore(LlmVendor, TokenStore)` | `TokenState` | Hands core where the vendor's keys live. |
| `registerStore(LlmVendor, TokenStore, LlmCredentials)` | `TokenState` | Same, through a non-default endpoint. The template's key is ignored; its base URL, timeout and headers go with every key from the store. |
| `tokenState(LlmVendor)` | `TokenState` | `READY`, `NOT_SET` or `EXPIRED`. |
| `reloadTokens(LlmVendor)` | `TokenState` | Re-reads one vendor's store. |
| `reloadTokens()` | `int` | Re-reads every store; returns how many vendors were reloaded. |

## TokenState

`io.github.mcagentsmods.core.api.token.TokenState`

| Value | Meaning | What to tell the player |
|---|---|---|
| `READY` | At least one usable key. | Nothing. |
| `NOT_SET` | The store holds no key. | Add one. |
| `EXPIRED` | Every key was rejected and removed. | Their keys stopped working. |

Both `NOT_SET` and `EXPIRED` mean "no usable key"; they are separate because they ask the
player for different things.

## TokenStore

`io.github.mcagentsmods.core.api.token.TokenStore` is where keys live. Core calls it; a mod
implements it, or uses the shared key file every MCAgents mod reads
([`key-file.md`](key-file.md)).

| Method | What core expects |
|---|---|
| `load(String vendorCode)` | The vendor's keys, oldest first, which is the order core tries them. Empty, never `null`, when there are none. |
| `add(String vendorCode, String token)` | Store a key. `false` when it was already there or could not be written. Never logs or echoes the key. |
| `evict(String vendorCode, String token)` | Delete a key: core calls it for a rejected key, and the [token commands](token-commands.md) for one removed by hand. A missing key is a no-op. |
| `reload()` | Re-read the backing storage, discarding what is in memory, so an evicted key cannot come back. |
| `describe()` | Where the keys live, for a message. Never a key. |

`vendorCode` is `LlmVendor.code()`: `openrouter`, `openai`, `deepseek` or `anthropic`.

## What core does with a failed key

| The vendor answered | Core | The key |
|---|---|---|
| 401, rejected | Retries the call on the next key. | **Deleted** from the store. |
| 429, rate limited | Retries the call on the next key. | **Kept**: it is healthy, only busy. |
| 403 | Fails the call. | Kept. Providers send 403 for a refused prompt or a region, not a bad key. |
| Anything else | Fails the call. | Untouched: nothing was learned about it. |

Deleting a rate-limited key would destroy something the player paid for, which is why this
lives in core rather than in each mod.

Rotation rebuilds each credential from the vendor's stored connection settings, so a
deployment behind a proxy or a self-hosted gateway stays on its endpoint.
