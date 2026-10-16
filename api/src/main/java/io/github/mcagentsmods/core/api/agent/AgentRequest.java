package io.github.mcagentsmods.core.api.agent;

import io.github.mcagentsmods.core.api.model.PromptMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One question for the agent.
 *
 * <p>The {@link #prompt()} is what the agent picks categories and tools by. The
 * {@link #conversation()} is what it answers from: for a single question it is just the prompt,
 * and a mod with history of its own — a chat, with a persona and earlier turns — passes the
 * whole conversation instead, ending with the question.
 *
 * @param model        the model id to ask, as core stores it
 * @param prompt       the player's request, as the category and tool steps read it
 * @param conversation the messages the answer is written from, oldest first
 * @param categories   the categories the agent may use; empty means every category
 * @param toolRounds   how many times the model may ask for tools before it must answer, from 1
 *                     to {@link #MAX_TOOL_ROUNDS}, or {@link #PLAYER_SETTING} for the number the
 *                     player set in game
 */
public record AgentRequest(
        String model,
        String prompt,
        List<PromptMessage> conversation,
        Set<String> categories,
        int toolRounds) {

    /** Use the number of tool rounds the player set in game. */
    public static final int PLAYER_SETTING = 0;

    /** The most tool rounds one question may take. */
    public static final int MAX_TOOL_ROUNDS = 8;

    public AgentRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(prompt, "prompt");
        if (model.isBlank()) {
            throw new IllegalArgumentException("An agent request needs a model");
        }
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("An agent request needs a prompt");
        }
        conversation = conversation == null || conversation.isEmpty()
                ? List.of(PromptMessage.user(prompt))
                : List.copyOf(conversation);
        categories = categories == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(categories));
        if (toolRounds < PLAYER_SETTING || toolRounds > MAX_TOOL_ROUNDS) {
            throw new IllegalArgumentException("Tool rounds must be between 1 and " + MAX_TOOL_ROUNDS
                    + ", or PLAYER_SETTING, not " + toolRounds);
        }
    }

    /** A single question, answered from the prompt alone, with every category and the player's rounds. */
    public static AgentRequest of(String model, String prompt) {
        return new AgentRequest(model, prompt, List.of(), Set.of(), PLAYER_SETTING);
    }

    /** This request answered from {@code messages}, which should end with the question. */
    public AgentRequest withConversation(List<PromptMessage> messages) {
        return new AgentRequest(model, prompt, messages, categories, toolRounds);
    }

    /** This request limited to the tools in {@code ids}. */
    public AgentRequest withCategories(Collection<String> ids) {
        return new AgentRequest(model, prompt, conversation, new LinkedHashSet<>(ids), toolRounds);
    }

    /** This request with its own number of tool rounds rather than the player's. */
    public AgentRequest withToolRounds(int rounds) {
        return new AgentRequest(model, prompt, conversation, categories, rounds);
    }

    /** Whether the request is limited to some categories. */
    public boolean isLimitedToCategories() {
        return !categories.isEmpty();
    }
}
