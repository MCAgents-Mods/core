package io.github.mcagentsmods.core.api.model;

/**
 * A model request failed.
 *
 * <p>The {@link Kind} is what callers branch on, because the right response differs sharply
 * between them: a rejected key rotates, a rate limit retries, a bad request does not.
 */
public class ModelException extends RuntimeException {

    /** Why the request failed, in the terms that decide what to do about it. */
    public enum Kind {
        /** The key was rejected. Deactivate it and try the next one. */
        AUTHENTICATION,
        /** Rate limited or out of credit. Back off and retry. */
        RATE_LIMIT,
        /** The provider failed. Back off and retry. */
        SERVER,
        /** The request was wrong — a bad model id, a malformed body. Retrying will not help. */
        REQUEST,
        /** The network failed, or nothing answered. Retrying may help. */
        TRANSPORT,
        /**
         * The provider answered successfully but the body could not be used.
         *
         * <p>Deliberately not retryable: the request already succeeded, so the player has
         * already been billed for it, and sending it again would bill them again for the
         * same broken answer.
         */
        MALFORMED_RESPONSE,
        /** The player has no usable key stored. */
        NO_KEY,
        /**
         * The client was closed before the request finished — the game is shutting down,
         * or the mod replaced its client. Retrying on the same client cannot help.
         */
        CLOSED;

        /**
         * What an HTTP error status says about a request.
         *
         * <p>Only 401 says a key is dead, and 402 that it is out of credit. A 403 is
         * deliberately not one of them: providers answer 403 for a prompt their moderation
         * flagged, for a region they do not serve, and for a permission the key lacks — none
         * of which a different key fixes, and treating it as a dead key would let one flagged
         * prompt deactivate every key the player has.
         *
         * @param status an HTTP status outside the 2xx range
         * @return the kind of failure it is
         */
        public static Kind forStatus(int status) {
            return switch (status) {
                case 401, 402 -> AUTHENTICATION;
                case 429 -> RATE_LIMIT;
                default -> status >= 500 ? SERVER : REQUEST;
            };
        }
    }

    private final Kind kind;

    public ModelException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ModelException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Whether retrying the same request could plausibly succeed. */
    public boolean isRetryable() {
        return kind == Kind.RATE_LIMIT || kind == Kind.SERVER || kind == Kind.TRANSPORT;
    }
}
