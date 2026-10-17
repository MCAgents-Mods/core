package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * What a hologram shows: one or more panels that stand side by side in front of the player,
 * left to right, and are placed, moved and closed together.
 *
 * <p>The panels are data: a mod builds a hologram, hands it to core, and core draws it until
 * the player closes it or walks away. Several holograms can stand at once; the id tells them
 * apart, so a mod that shows several of a kind gives each its own id.
 *
 * @param id      which hologram this is: 1 to 64 lowercase letters, digits, dots, dashes or
 *                underscores, conventionally {@code {modid}.{name}}; one hologram per id is
 *                open at a time
 * @param panels  the panels, left to right; at least one and at most {@link #MAX_PANELS}
 * @param onClose run once when the hologram closes, whatever closes it: its X, the mod, the
 *                player walking away, or a hologram with the same id opening in its place. It
 *                runs on the client thread and must not block; one that throws is logged
 */
public record Hologram(String id, List<HologramPanel> panels, Runnable onClose) {

    /** The most panels one hologram stands; more would run off the sides of the screen. */
    public static final int MAX_PANELS = 3;

    private static final Pattern ID = Pattern.compile("[a-z0-9._-]{1,64}");

    public Hologram {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(panels, "panels");
        Objects.requireNonNull(onClose, "onClose");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("The hologram id '" + id
                    + "' must be 1 to 64 lowercase letters, digits, dots, dashes or underscores");
        }
        panels = List.copyOf(panels);
        if (panels.isEmpty() || panels.size() > MAX_PANELS) {
            throw new IllegalArgumentException("A hologram has 1 to " + MAX_PANELS
                    + " panels, not " + panels.size());
        }
    }

    /**
     * A hologram that does nothing when it closes.
     *
     * @param id     which hologram this is
     * @param panels the panels, left to right
     */
    public Hologram(String id, List<HologramPanel> panels) {
        this(id, panels, () -> { });
    }

    /**
     * @param onClose what to run once when the hologram closes
     * @return this hologram, running it
     */
    public Hologram withOnClose(Runnable onClose) {
        return new Hologram(id, panels, onClose);
    }

    /**
     * A hologram of the given panels, left to right.
     *
     * @param id     who the hologram belongs to
     * @param panels the panels
     * @return the hologram
     */
    public static Hologram of(String id, HologramPanel... panels) {
        return new Hologram(id, List.of(panels));
    }
}
