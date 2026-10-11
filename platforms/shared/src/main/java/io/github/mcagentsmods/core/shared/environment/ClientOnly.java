package io.github.mcagentsmods.core.shared.environment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks something that exists on a client and nowhere else.
 *
 * <p>This is the loader-neutral spelling of Fabric's {@code @Environment(CLIENT)}
 * and NeoForge's {@code @OnlyIn(Dist.CLIENT)}. Neither of those can be used
 * here: {@code platforms:shared} compiles without a loader toolchain so it
 * stays plain Java and stays testable.</p>
 *
 * <p><strong>The annotation is documentation, not enforcement.</strong> What
 * actually keeps client code off a dedicated server is the package layout —
 * {@code shared.client} is never imported from outside itself, which
 * {@code SideSeparationTest} checks — and the {@link SideGuard} check at each
 * entry point. Marking a class does not make it safe; putting it in the client
 * package and never linking it does.</p>
 *
 * <p>Retained at runtime so a test can assert that the client half is marked,
 * which is the cheapest way to keep the convention from rotting.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface ClientOnly {
}
