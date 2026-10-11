package io.github.mcagentsmods.core.api.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code {platform}/{model}} naming convention, and where each platform answers.
 */
class ModelNamingTest {

    @Test
    void buildsANameFromThePlatformAndTheModel() {
        assertEquals("openai/text-embedding-3-small",
                ModelName.of("OpenAI", "Text-Embedding-3-Small").qualified());
    }

    @Test
    void refusesAModelIdThatAlreadyCarriesItsPlatform() {
        // Doubling it silently would produce openai/openai/gpt-4o-mini, which is a second
        // name for one model and compares equal to nothing.
        assertThrows(IllegalArgumentException.class,
                () -> ModelName.of("openai", "openai/gpt-4o-mini"));
    }

    @Test
    void refusesAPlatformThatIsNotOneSegment() {
        assertThrows(IllegalArgumentException.class, () -> ModelName.of("open/ai", "gpt-4o-mini"));
        assertThrows(IllegalArgumentException.class, () -> ModelName.of(" ", "gpt-4o-mini"));
    }

    @Test
    void splitsAtTheFirstSlashOnly() {
        // A model id may carry slashes or a suffix of its own; the platform never does.
        ModelName name = ModelName.parse("deepseek/deepseek-v4-flash:free").orElseThrow();

        assertEquals("deepseek", name.platform());
        assertEquals("deepseek-v4-flash:free", name.model());
    }

    @Test
    void lowercasesWhatItReadsBack() {
        assertEquals("openai/gpt-4o-mini",
                ModelName.parse("OpenAI/GPT-4o-Mini").orElseThrow().qualified());
    }

    @Test
    void reportsAnIdWithNoPlatformAsUnqualified() {
        assertEquals(Optional.empty(), ModelName.parse("gpt-4o-mini"));
        assertEquals(Optional.empty(), ModelName.parse("/gpt-4o-mini"));
        assertEquals(Optional.empty(), ModelName.parse("openai/"));
        assertFalse(ModelName.isQualified("gpt-4o-mini"));
    }

    @Test
    void keepsAnUnqualifiedIdRatherThanInventingAPlatform() {
        // openrouter/gpt-4o-mini is not a shorter spelling of gpt-4o-mini, it is a model
        // nobody serves. An unqualified id is kept as typed and routed through the gateway.
        assertEquals("gpt-4o-mini", ModelName.normalise("  GPT-4o-Mini "));
    }

    @Test
    void treatsABlankPlatformAsTheGateway() {
        assertEquals(ModelPlatform.OPENROUTER, ModelPlatform.normalise(null));
        assertEquals(ModelPlatform.OPENROUTER, ModelPlatform.normalise("  "));
        assertEquals("openai", ModelPlatform.normalise("  OpenAI "));
    }

    @Test
    void sendsAKeyOnlyToAnEncryptedOrLoopbackAddress() {
        assertTrue(ModelPlatform.isUsableBaseUrl("https://api.openai.com/v1"));
        assertTrue(ModelPlatform.isUsableBaseUrl("http://localhost:11434/v1"));
        assertTrue(ModelPlatform.isUsableBaseUrl("http://127.0.0.1:1234/v1"));
        // A key on the wire in the clear, to somewhere that is not this machine.
        assertFalse(ModelPlatform.isUsableBaseUrl("http://example.invalid/v1"));
        assertFalse(ModelPlatform.isUsableBaseUrl(""));
        assertFalse(ModelPlatform.isUsableBaseUrl(null));
    }

    @Test
    void knowsWhereTheCommonPlatformsAnswer() {
        assertTrue(ModelPlatform.knownBaseUrl("openai").isPresent());
        assertTrue(ModelPlatform.knownBaseUrl("nobody-ships-this").isEmpty());
        // The gateway is the fallback, not a direct route, so it is not in the map.
        assertTrue(ModelPlatform.knownBaseUrl(ModelPlatform.OPENROUTER).isEmpty());
        ModelPlatform.knownDirectBaseUrls().values()
                .forEach(url -> assertTrue(ModelPlatform.isUsableBaseUrl(url), url));
    }

    @Test
    void namesTheSettingThatOverridesAPlatformsAddress() {
        assertEquals("api.base-url.openai", ModelPlatform.baseUrlSettingKey("OpenAI"));
    }

    @Test
    void routesAQualifiedNameEitherWay() {
        ModelName name = ModelName.parse("openai/gpt-4o-mini").orElseThrow();

        ProviderRoute gateway = ProviderRoute.throughOpenRouter(name.qualified());
        ProviderRoute direct = ProviderRoute.direct(name, "https://api.openai.com/v1/");

        assertTrue(gateway.isThroughOpenRouter());
        assertEquals("openai/gpt-4o-mini", gateway.modelId());
        assertEquals("gpt-4o-mini", direct.modelId());
        assertEquals("https://api.openai.com/v1/chat/completions", direct.chatUrl());
        assertThrows(IllegalArgumentException.class,
                () -> ProviderRoute.direct(name, "http://example.invalid/v1"));
    }
}
