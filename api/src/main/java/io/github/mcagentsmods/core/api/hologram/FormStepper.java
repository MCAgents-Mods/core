package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A number on a {@link Form} with a minus and a plus button: the label on the left, the
 * number between the two buttons on the right. The mod keeps the number within its limits.
 *
 * @param label what the number is: a translation key, or the text itself
 * @param value the number now, read every frame
 * @param step  called with {@code -1} or {@code +1} when the player presses minus or plus, on
 *              the client thread
 */
public record FormStepper(String label, IntSupplier value, IntConsumer step) implements FormRow {

    public FormStepper {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(step, "step");
    }
}
