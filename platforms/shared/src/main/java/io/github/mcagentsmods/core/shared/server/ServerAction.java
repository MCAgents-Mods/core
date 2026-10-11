package io.github.mcagentsmods.core.shared.server;

import io.github.mcagentsmods.core.shared.environment.ServerOnly;

/**
 * What a caller is trying to do to the server's credentials.
 *
 * <p>Enumerated rather than passed as a string so that adding an operation
 * forces a decision about who may perform it: a new constant makes the switch
 * in {@link ServerCommandAuthority} fail to compile until someone answers the
 * question. A string parameter would have defaulted the answer to "whatever the
 * last branch did".</p>
 */
@ServerOnly
public enum ServerAction {

    /**
     * Read which platforms have a usable credential, with masked handles.
     *
     * <p>Restricted like the rest. The handles reveal no key, but the list is
     * still the server owner's configuration, and which platforms an owner pays
     * for is not a player's business.</p>
     */
    VIEW_STATUS("view credential status"),

    /**
     * Store a new credential.
     */
    ADD_TOKEN("add a credential"),

    /**
     * Delete a stored credential.
     */
    REMOVE_TOKEN("remove a credential"),

    /**
     * Re-read the credential file and reinstall every platform.
     */
    RELOAD("reload credentials");

    /**
     * How this action is named in a refusal, phrased to complete the sentence
     * "You are not allowed to …".
     */
    private final String description;

    /**
     * Binds an action to how it is described.
     *
     * @param description The phrase used in a refusal.
     */
    ServerAction(String description) {
        this.description = description;
    }

    /**
     * Returns how this action is described in a refusal.
     *
     * @return The phrase, for example {@code "add a credential"}.
     */
    public String description() {
        return description;
    }
}
