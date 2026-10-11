package io.github.mcagentsmods.core.api.web;

import java.util.Objects;

/**
 * One link in the console's header.
 *
 * @param label   what the link says
 * @param path    where it goes, relative to the module's root: {@code chat/} for
 *                {@code /{id}/chat/}. No leading slash, no query string, no scheme
 * @param section the value a page names in its {@code data-section} to mark this link
 *                active; defaults to the path's first segment
 */
public record NavLink(String label, String path, String section) {

    public NavLink {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(path, "path");
        if (label.isBlank()) {
            throw new IllegalArgumentException("A navigation link needs a label");
        }
        if (path.startsWith("/") || path.contains("..") || path.contains(":")
                || path.contains("?") || path.contains("#") || path.contains("\\")) {
            throw new IllegalArgumentException(
                    "A navigation path is relative to its module, with no query or scheme");
        }
        if (section == null || section.isBlank()) {
            int slash = path.indexOf('/');
            section = slash < 0 ? path : path.substring(0, slash);
        }
    }

    /** A link whose section is its path's first segment. */
    public NavLink(String label, String path) {
        this(label, path, null);
    }
}
