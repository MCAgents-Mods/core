package io.github.mcagentsmods.core.api.llm;

import io.github.mcagentsmods.core.api.model.ModelPlatform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a key may be sent, and what a log line may show of it. */
class LlmCredentialsTest {

    @Test
    void refusesToSendAKeyInTheClearToAnotherMachine() {
        assertThrows(IllegalArgumentException.class,
                () -> LlmCredentials.of(LlmVendor.OPENAI, "sk-test", "http://gateway.example.com/v1"));
        assertThrows(IllegalArgumentException.class,
                () -> LlmCredentials.of(LlmVendor.OPENAI, "sk-test", "http://localhost.example.com/v1"));
        assertDoesNotThrow(() -> LlmCredentials.of(LlmVendor.OPENAI, "sk-test", "http://localhost:11434/v1"));
        assertDoesNotThrow(() -> LlmCredentials.of(LlmVendor.OPENAI, "sk-test", "https://gateway.example.com/v1/"));
    }

    @Test
    void neverPrintsTheKey() {
        assertFalse(LlmCredentials.of(LlmVendor.OPENROUTER, "sk-or-v1-neverprinted")
                .toString().contains("neverprinted"));
    }

    @Test
    void refusesAKeyThatCouldNotBeSentWithoutRepeatingIt() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> LlmCredentials.of(LlmVendor.OPENROUTER, "sk-or-v1-secret\nX-Evil: 1"));
        assertFalse(refused.getMessage().contains("secret"));
        assertThrows(IllegalArgumentException.class,
                () -> LlmCredentials.of(LlmVendor.OPENROUTER, "sk-or-v1 secret"));
        assertThrows(IllegalArgumentException.class,
                () -> LlmCredentials.of(LlmVendor.OPENROUTER, "sk-or-v1-\u201csecret\u201d"));
        assertTrue(LlmCredentials.isWellFormedKey("sk-or-v1-0123abcd"));
        assertFalse(LlmCredentials.isWellFormedKey(null));
    }

    @Test
    void vendorAddressesComeFromTheOnePlatformTable() {
        assertEquals(ModelPlatform.knownBaseUrl("openai").orElseThrow(), LlmVendor.OPENAI.defaultBaseUrl());
        assertEquals(ModelPlatform.OPENROUTER_BASE_URL, LlmVendor.OPENROUTER.defaultBaseUrl());
    }
}
