package io.github.mcagentsmods.core.api.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Everything needed to ask a language model one question.
 *
 * <p>A request is self contained and immutable: it names the model, carries the
 * full message list, and fixes the sampling settings. Nothing is remembered
 * between requests, so a caller that wants a conversation sends the whole
 * history every time.</p>
 *
 * <p>The three sampling settings are optional and use a sentinel rather than a
 * boxed type, so a caller never has to unwrap them: {@link #maxTokens()} of
 * {@code -1} and a {@link Double#NaN} {@link #temperature()} or {@link #topP()}
 * all mean "leave it to the vendor's default". Build one with
 * {@link #builder(String)}.</p>
 *
 * @param model The vendor's model identifier, for example
 *              {@code "anthropic/claude-opus-4"} on OpenRouter or
 *              {@code "gpt-4o-mini"} on OpenAI. Never blank.
 * @param messages The conversation to send, oldest turn first. Never empty, and
 *                 always an unmodifiable copy of what the caller supplied.
 * @param systemPrompt Framing instructions applied to the whole exchange, or an
 *                     empty string for none. Kept separate from
 *                     {@link #messages()} because vendors disagree about where
 *                     it belongs on the wire — the client places it correctly.
 * @param maxTokens The upper bound on tokens to generate, or {@code -1} for the
 *                  vendor's default. Anthropic requires a bound, so its client
 *                  substitutes one when this is left unset.
 * @param temperature Sampling temperature between {@code 0.0} and {@code 2.0},
 *                    or {@link Double#NaN} for the vendor's default. Lower is
 *                    more deterministic.
 * @param topP Nucleus sampling cutoff above {@code 0.0} and at most
 *             {@code 1.0}, or {@link Double#NaN} for the vendor's default.
 * @param stopSequences Strings that end generation when produced. Never
 *                      {@code null}, always an unmodifiable copy, and empty for
 *                      none.
 */
public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        String systemPrompt,
        int maxTokens,
        double temperature,
        double topP,
        List<String> stopSequences) {

    /**
     * The value {@link #maxTokens()} takes when the caller set no bound.
     */
    public static final int DEFAULT_MAX_TOKENS = -1;

    /**
     * Validates the components and defensively copies the two lists.
     *
     * @throws NullPointerException When any component is {@code null}.
     * @throws IllegalArgumentException When the model is blank, the message
     *                                  list is empty, or a sampling setting is
     *                                  outside its documented range.
     */
    public ChatRequest {
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(messages, "messages cannot be null");
        Objects.requireNonNull(systemPrompt, "systemPrompt cannot be null");
        Objects.requireNonNull(stopSequences, "stopSequences cannot be null");

        if (model.isBlank()) {
            throw new IllegalArgumentException("model cannot be blank");
        }
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be empty");
        }
        if (maxTokens != DEFAULT_MAX_TOKENS && maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be at least 1, or -1 for the vendor default");
        }
        if (!Double.isNaN(temperature) && (temperature < 0.0 || temperature > 2.0)) {
            throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
        }
        if (!Double.isNaN(topP) && (topP <= 0.0 || topP > 1.0)) {
            throw new IllegalArgumentException("topP must be greater than 0.0 and at most 1.0");
        }

        messages = List.copyOf(messages);
        stopSequences = List.copyOf(stopSequences);
    }

    /**
     * Builds the simplest possible request: one user turn, every sampling
     * setting left to the vendor.
     *
     * @param model The model identifier to send to.
     * @param prompt The single user turn.
     * @return The new request.
     */
    public static ChatRequest of(String model, String prompt) {
        return builder(model).user(prompt).build();
    }

    /**
     * Starts building a request for a model.
     *
     * @param model The model identifier to send to.
     * @return A fresh builder, with no messages and no sampling settings.
     */
    public static Builder builder(String model) {
        return new Builder(model);
    }

    /**
     * Reports whether a bound on generated tokens was set.
     *
     * @return {@code true} when {@link #maxTokens()} is a real bound rather
     *         than {@link #DEFAULT_MAX_TOKENS}.
     */
    public boolean hasMaxTokens() {
        return maxTokens != DEFAULT_MAX_TOKENS;
    }

    /**
     * Reports whether a sampling temperature was set.
     *
     * @return {@code true} when {@link #temperature()} is a real value rather
     *         than {@link Double#NaN}.
     */
    public boolean hasTemperature() {
        return !Double.isNaN(temperature);
    }

    /**
     * Reports whether a nucleus sampling cutoff was set.
     *
     * @return {@code true} when {@link #topP()} is a real value rather than
     *         {@link Double#NaN}.
     */
    public boolean hasTopP() {
        return !Double.isNaN(topP);
    }

    /**
     * Reports whether framing instructions were supplied.
     *
     * @return {@code true} when {@link #systemPrompt()} holds something other
     *         than whitespace.
     */
    public boolean hasSystemPrompt() {
        return !systemPrompt.isBlank();
    }

    /**
     * Fluent builder for {@link ChatRequest}.
     *
     * <p>A builder is mutable and is not safe to share between threads. Build
     * the request on one thread and hand the finished, immutable record
     * around.</p>
     */
    public static final class Builder {

        /**
         * The model identifier the finished request will name. Fixed when the
         * builder is created.
         */
        private final String model;

        /**
         * The turns accumulated so far, in the order they were added. Copied
         * into the record on {@link #build()}.
         */
        private final List<ChatMessage> messages = new ArrayList<>();

        /**
         * The stop sequences accumulated so far. Copied into the record on
         * {@link #build()}.
         */
        private final List<String> stopSequences = new ArrayList<>();

        /**
         * The framing instructions, empty until {@link #system(String)} is
         * called.
         */
        private String systemPrompt = "";

        /**
         * The token bound, {@link ChatRequest#DEFAULT_MAX_TOKENS} until set.
         */
        private int maxTokens = DEFAULT_MAX_TOKENS;

        /**
         * The sampling temperature, {@link Double#NaN} until set.
         */
        private double temperature = Double.NaN;

        /**
         * The nucleus sampling cutoff, {@link Double#NaN} until set.
         */
        private double topP = Double.NaN;

        /**
         * Creates a builder for a model.
         *
         * @param model The model identifier to send to.
         */
        private Builder(String model) {
            this.model = model;
        }

        /**
         * Sets the framing instructions for the whole exchange, replacing any
         * previously set.
         *
         * @param prompt The instructions.
         * @return This builder.
         */
        public Builder system(String prompt) {
            this.systemPrompt = Objects.requireNonNull(prompt, "prompt cannot be null");
            return this;
        }

        /**
         * Appends a {@link ChatRole#USER} turn.
         *
         * @param content The text to send.
         * @return This builder.
         */
        public Builder user(String content) {
            messages.add(ChatMessage.user(content));
            return this;
        }

        /**
         * Appends a {@link ChatRole#ASSISTANT} turn, replaying an earlier reply
         * as context.
         *
         * @param content The text the model produced.
         * @return This builder.
         */
        public Builder assistant(String content) {
            messages.add(ChatMessage.assistant(content));
            return this;
        }

        /**
         * Appends an already built message of any role.
         *
         * @param message The turn to append.
         * @return This builder.
         */
        public Builder message(ChatMessage message) {
            messages.add(Objects.requireNonNull(message, "message cannot be null"));
            return this;
        }

        /**
         * Appends every message in a conversation, oldest turn first.
         *
         * @param history The turns to append.
         * @return This builder.
         */
        public Builder messages(List<ChatMessage> history) {
            Objects.requireNonNull(history, "history cannot be null");
            history.forEach(this::message);
            return this;
        }

        /**
         * Sets the upper bound on tokens to generate.
         *
         * @param tokens The bound, or {@link ChatRequest#DEFAULT_MAX_TOKENS} to
         *               leave it to the vendor.
         * @return This builder.
         */
        public Builder maxTokens(int tokens) {
            this.maxTokens = tokens;
            return this;
        }

        /**
         * Sets the sampling temperature.
         *
         * @param value A value between {@code 0.0} and {@code 2.0}, or
         *              {@link Double#NaN} to leave it to the vendor.
         * @return This builder.
         */
        public Builder temperature(double value) {
            this.temperature = value;
            return this;
        }

        /**
         * Sets the nucleus sampling cutoff.
         *
         * @param value A value above {@code 0.0} and at most {@code 1.0}, or
         *              {@link Double#NaN} to leave it to the vendor.
         * @return This builder.
         */
        public Builder topP(double value) {
            this.topP = value;
            return this;
        }

        /**
         * Appends a string that ends generation when the model produces it.
         *
         * @param sequence The stop sequence.
         * @return This builder.
         */
        public Builder stop(String sequence) {
            stopSequences.add(Objects.requireNonNull(sequence, "sequence cannot be null"));
            return this;
        }

        /**
         * Builds the immutable request.
         *
         * @return The finished request.
         * @throws IllegalArgumentException When no message was added, or a
         *                                  sampling setting is outside its
         *                                  documented range.
         */
        public ChatRequest build() {
            return new ChatRequest(model, messages, systemPrompt, maxTokens, temperature, topP, stopSequences);
        }
    }
}
