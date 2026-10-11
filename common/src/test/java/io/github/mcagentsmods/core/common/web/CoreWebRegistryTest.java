package io.github.mcagentsmods.core.common.web;

import io.github.mcagentsmods.core.api.web.NavLink;
import io.github.mcagentsmods.core.api.web.WebResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Who may register what, before anything is listening. */
class CoreWebRegistryTest {

    @Test
    void acceptsModulesBeforeAnythingIsListening() {
        CoreWebRegistry registry = new CoreWebRegistry();
        registry.register(new SampleModule("chat"));
        registry.register(new SampleModule("maps_2"));

        assertEquals(List.of("chat", "maps_2"), registry.moduleIds());
        assertTrue(registry.current().isEmpty(), "registering opens no port");
    }

    @Test
    void refusesAnIdThatIsNotOneSegmentOfLowercase() {
        CoreWebRegistry registry = new CoreWebRegistry();
        for (String id : List.of("", "Chat", "chat-mod", "chat/x", "..", "a".repeat(33))) {
            assertThrows(IllegalArgumentException.class,
                    () -> registry.register(new SampleModule(id)), id);
        }
    }

    @Test
    void keepsTheShellsPathsForCore() {
        CoreWebRegistry registry = new CoreWebRegistry();
        for (String id : List.of("core", "api", "assets")) {
            assertThrows(IllegalArgumentException.class,
                    () -> registry.register(new SampleModule(id)), id);
        }
    }

    @Test
    void letsNoModuleReplaceAnothersPages() {
        CoreWebRegistry registry = new CoreWebRegistry();
        registry.register(new SampleModule("chat"));

        assertThrows(IllegalStateException.class,
                () -> registry.register(new SampleModule("chat")));
        assertTrue(registry.unregister("chat"));
        assertFalse(registry.unregister("chat"));
    }

    @Test
    void refusesAResourceRootThatCouldReachOutsideTheMod() {
        CoreWebRegistry registry = new CoreWebRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new SampleModule("chat") {
                    @Override
                    public String resourceRoot() {
                        return "/";
                    }
                }));
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new SampleModule("maps") {
                    @Override
                    public String resourceRoot() {
                        return "maps/../";
                    }
                }));
    }

    @Test
    void keepsANavigationLinkInsideItsModule() {
        assertThrows(IllegalArgumentException.class, () -> new NavLink("Away", "/core/"));
        assertThrows(IllegalArgumentException.class, () -> new NavLink("Away", "../core/"));
        assertThrows(IllegalArgumentException.class,
                () -> new NavLink("Away", "javascript:alert(1)"));
        assertEquals("knowledge", new NavLink("Knowledge", "knowledge/").section());
    }

    @Test
    void writesStringsThatCannotEscapeTheirJson() {
        assertEquals("\"a\\\"b\\\\c\\n\\u003c/script\\u003e\"",
                WebResponse.quote("a\"b\\c\n</script>"));
        assertEquals("{\"error\":\"Request refused.\"}",
                new String(WebResponse.error(400, " ").body()));
    }
}
