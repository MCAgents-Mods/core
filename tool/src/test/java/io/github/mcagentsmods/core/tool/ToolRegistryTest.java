package io.github.mcagentsmods.core.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    private static ToolSpec spec(String name, String... categories) {
        return ToolSpec.builder(name).description("Does " + name).categories(List.of(categories)).build();
    }

    private static final Tool OK = arguments -> ToolResult.ok("done");

    @Test
    void findsARegisteredToolByName() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("look", "world"), OK);

        assertTrue(registry.contains("look"));
        assertEquals("look", registry.find("look").orElseThrow().name());
        assertTrue(registry.find("missing").isEmpty());
        assertTrue(registry.find(null).isEmpty());
    }

    @Test
    void refusesADuplicateName() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("look", "world"), OK);

        ToolRegistrationException refused = assertThrows(ToolRegistrationException.class,
                () -> registry.register(spec("look", "other"), OK));
        assertTrue(refused.getMessage().contains("already registered"));
        assertEquals(Set.of("world"), registry.find("look").orElseThrow().spec().categories());
    }

    @Test
    void refusesNamesAProviderWouldReject() {
        assertThrows(ToolRegistrationException.class, () -> spec("has space", "world"));
        assertThrows(ToolRegistrationException.class, () -> spec("dotted.name", "world"));
        assertThrows(ToolRegistrationException.class, () -> spec("x".repeat(65), "world"));
        assertThrows(ToolRegistrationException.class, () -> spec("", "world"));
        assertThrows(ToolRegistrationException.class, () -> spec("look", "bad category"));
    }

    @Test
    void refusesAToolWithNoDescriptionOrNoCategory() {
        assertThrows(ToolRegistrationException.class,
                () -> ToolSpec.builder("look").category("world").build());
        assertThrows(ToolRegistrationException.class,
                () -> ToolSpec.builder("look").description("   ").category("world").build());
        assertThrows(ToolRegistrationException.class,
                () -> ToolSpec.builder("look").description("Looks").build());
    }

    @Test
    void aToolInSeveralCategoriesIsListedOnceWhenSeveralArePicked() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("look", "world", "player"), OK);
        registry.register(spec("walk", "player"), OK);
        registry.register(spec("time", "clock"), OK);

        List<String> names = registry.toolsIn(List.of("world", "player")).stream().map(ToolSpec::name).toList();

        assertEquals(List.of("look", "walk"), names);
    }

    @Test
    void listsOnlyCategoriesThatHoldATool() {
        ToolRegistry registry = new ToolRegistry();
        registry.declareCategory("empty", "Nothing in it");
        registry.register(spec("look", "world"), OK);
        registry.register(spec("time", "clock"), OK);

        List<String> ids = registry.categories().stream().map(ToolCategory::id).toList();

        assertEquals(List.of("clock", "world"), ids);
        assertTrue(registry.category("empty").isPresent());
    }

    @Test
    void theFirstDescriptionOfACategoryStays() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("look", "world"), OK);
        assertFalse(registry.category("world").orElseThrow().isDescribed());

        registry.declareCategory("world", "Blocks and entities");
        registry.declareCategory("world", "Something else");

        assertEquals("Blocks and entities", registry.categories().get(0).description());
    }

    @Test
    void closingARegistrationRemovesOnlyThatTool() {
        ToolRegistry registry = new ToolRegistry();
        ToolRegistration first = registry.register(spec("look", "world"), OK);
        first.close();
        assertFalse(registry.contains("look"));

        ToolRegistration second = registry.register(spec("look", "world"), OK);
        // The old handle no longer owns the name, so it cannot take the new tool away.
        first.close();
        assertTrue(registry.contains("look"));
        second.close();
        assertTrue(registry.isEmpty());
    }

    @Test
    void aSnapshotDoesNotChangeWithTheRegistry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(spec("look", "world"), OK);
        ToolRegistry.Snapshot snapshot = registry.snapshot();

        registry.register(spec("walk", "player"), OK);
        registry.unregister("look");

        assertTrue(snapshot.find("look").isPresent());
        assertTrue(snapshot.find("walk").isEmpty());
        assertEquals(List.of("world"), snapshot.categories().stream().map(ToolCategory::id).toList());
    }

    @Test
    void onlyOneOfManyThreadsRegisteringTheSameNameWins() throws Exception {
        ToolRegistry registry = new ToolRegistry();
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger won = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> attempts = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                attempts.add(executor.submit(() -> {
                    start.await();
                    try {
                        registry.register(spec("look", "world"), OK);
                        won.incrementAndGet();
                    } catch (ToolRegistrationException e) {
                        refused.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> attempt : attempts) {
                attempt.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, won.get());
        assertEquals(threads - 1, refused.get());
    }

    @Test
    void theSharedRegistryIsOneInstance() {
        assertSame(ToolRegistry.shared(), ToolRegistry.shared());
    }

    @Test
    void keepsCategoriesInTheOrderGiven() {
        ToolSpec spec = spec("look", "world", "player", "alpha");
        assertEquals(List.of("world", "player", "alpha"), List.copyOf(spec.categories()));
    }
}
