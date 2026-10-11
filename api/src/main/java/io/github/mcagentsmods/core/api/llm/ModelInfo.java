package io.github.mcagentsmods.core.api.llm;

import java.util.Objects;

/**
 * One model a vendor offers, as that vendor described it.
 *
 * <p>Only the fields all four vendors agree on are carried. Pricing, modality
 * flags, and per provider routing details are deliberately left out: they are
 * vendor specific, they change without notice, and a shared record that tried
 * to hold them would be wrong for three vendors out of four.</p>
 *
 * @param id The identifier to put in a {@link io.github.mcagentsmods.core.api.chat.ChatRequest},
 *           never blank.
 * @param name A human readable name, or the same value as {@link #id()} when
 *             the vendor supplied none.
 * @param contextLength The model's context window in tokens, or {@code -1} when
 *                      the vendor did not report it.
 * @param vendor The service that listed this model, never {@code null}.
 */
public record ModelInfo(String id, String name, int contextLength, LlmVendor vendor) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException When any component is {@code null}.
     * @throws IllegalArgumentException When the id is blank.
     */
    public ModelInfo {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(vendor, "vendor cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }

    /**
     * Reports whether the vendor stated a context window.
     *
     * @return {@code true} when {@link #contextLength()} is a real figure
     *         rather than {@code -1}.
     */
    public boolean hasContextLength() {
        return contextLength > 0;
    }
}
