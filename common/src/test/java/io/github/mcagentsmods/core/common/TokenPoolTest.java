package io.github.mcagentsmods.core.common;

import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.api.token.TokenStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Rotation when several requests fail at once — the case where acting on "the current key"
 * instead of the key that failed deletes a good one.
 */
class TokenPoolTest {

    /** Holds keys in memory and records every eviction. */
    private static final class MemoryStore implements TokenStore {
        final List<String> keys = new ArrayList<>();
        final List<String> evicted = new ArrayList<>();

        MemoryStore(String... values) {
            keys.addAll(List.of(values));
        }

        @Override public List<String> load(String vendorCode) { return List.copyOf(keys); }
        @Override public boolean add(String vendorCode, String token) { return keys.add(token); }
        @Override public void evict(String vendorCode, String token) {
            evicted.add(token);
            keys.remove(token);
        }
        @Override public void reload() { }
        @Override public String describe() { return "memory"; }
    }

    @Test
    void twoFailuresOnOneDeadKeyRemoveOnlyThatKey() {
        MemoryStore store = new MemoryStore("dead", "good");
        TokenPool pool = new TokenPool(LlmVendor.OPENROUTER, store);

        assertEquals(Optional.of("good"), pool.reject("dead"));
        // The second request in flight on the dead key reports it too.
        assertEquals(Optional.of("good"), pool.reject("dead"));

        assertEquals(List.of("dead"), store.evicted);
        assertEquals(List.of("good"), store.keys, "the working key survives on disk");
    }

    @Test
    void twoRateLimitsOnOneKeyRotateOnce() {
        TokenPool pool = new TokenPool(LlmVendor.OPENROUTER, new MemoryStore("a", "b"));

        assertEquals(Optional.of("b"), pool.rotate("a"));
        assertEquals(Optional.of("b"), pool.rotate("a"), "not back onto the busy key");
    }
}
