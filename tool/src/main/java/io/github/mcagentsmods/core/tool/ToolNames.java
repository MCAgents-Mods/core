package io.github.mcagentsmods.core.tool;

import java.util.regex.Pattern;

/**
 * What a tool name or a category id may be.
 *
 * <p>The same rule OpenAI-compatible providers apply to a function name. It is checked when a
 * tool is registered rather than when it is sent, because a provider refuses the <em>whole
 * request</em> over one bad name: a single malformed tool would take every other tool down
 * with it, and the error would name none of them.
 */
public final class ToolNames {

    private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private ToolNames() {
        throw new AssertionError("No instances.");
    }

    /** Whether {@code name} could be a tool name or a category id. */
    public static boolean isValid(String name) {
        return name != null && ACCEPTED.matcher(name).matches();
    }

    /**
     * Refuses a name a provider would not accept.
     *
     * @param what what the name is for, for the message: "tool" or "category"
     */
    static void requireValid(String name, String what) {
        if (!isValid(name)) {
            throw new ToolRegistrationException("The " + what + " name '" + name
                    + "' is not one a model provider accepts: use 1 to 64 letters, digits, "
                    + "dashes or underscores");
        }
    }
}
