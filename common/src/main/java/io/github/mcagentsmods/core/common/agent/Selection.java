package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.tool.ToolArgumentException;
import io.github.mcagentsmods.core.tool.ToolArguments;
import io.github.mcagentsmods.core.tool.ToolCategory;
import io.github.mcagentsmods.core.tool.ToolSchema;
import io.github.mcagentsmods.core.tool.ToolSpec;
import io.github.mcagentsmods.core.api.model.ChatCompletion;
import io.github.mcagentsmods.core.api.model.PromptMessage;
import io.github.mcagentsmods.core.api.model.ToolCall;
import io.github.mcagentsmods.core.api.model.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The first two steps: asking the model which categories, then which tools, a prompt needs.
 *
 * <p>Each step offers the model one function to answer with — {@code select_categories} or
 * {@code select_tools} — whose argument may only hold the names on offer, so a well-behaved model
 * cannot pick something that is not there. A model that answers in prose instead is read for
 * the names it mentions. Whatever the model sends, only names that were offered survive.
 *
 * <p>The list of names is the system message and the prompt is the user message, so the part
 * that stays the same between questions comes first, where a provider can cache it.
 */
final class Selection {

    static final String SELECT_CATEGORIES = "select_categories";
    static final String SELECT_TOOLS = "select_tools";

    private static final String CATEGORY_INSTRUCTIONS = """
            You route a player's request to groups of tools. Below are the tool categories, \
            one per line as "id: what its tools do". Pick every category whose tools could \
            help with the request, and only those; pick several when the request needs \
            several. Answer by calling %s with their ids. If no category could help, call it \
            with an empty list.

            Categories:
            """.formatted(SELECT_CATEGORIES);

    private static final String TOOL_INSTRUCTIONS = """
            You choose the tools needed to answer a player's request. Below are the tools you \
            may use, one per line as "name: what it does". Pick every tool you will need to \
            call, and only those. Answer by calling %s with their names. If no tool is needed, \
            call it with an empty list.

            Tools:
            """.formatted(SELECT_TOOLS);

    private Selection() {
        throw new AssertionError("No instances.");
    }

    /** Step 1: the categories, as a request's messages. */
    static List<PromptMessage> categoryMessages(List<ToolCategory> categories, String prompt) {
        StringBuilder system = new StringBuilder(CATEGORY_INSTRUCTIONS);
        for (ToolCategory category : categories) {
            system.append("- ").append(category.id());
            if (category.isDescribed()) {
                system.append(": ").append(category.description());
            }
            system.append('\n');
        }
        return messages(system, prompt);
    }

    /** Step 1: the one function the model answers with. */
    static ToolDefinition categoryFunction(List<ToolCategory> categories) {
        return function(SELECT_CATEGORIES, "Selects the tool categories the request needs.",
                "categories", "The ids of the categories to use",
                categories.stream().map(ToolCategory::id).toList());
    }

    /** Step 2: the tools in the picked categories, by name and description only. */
    static List<PromptMessage> toolMessages(List<ToolSpec> tools, String prompt) {
        StringBuilder system = new StringBuilder(TOOL_INSTRUCTIONS);
        for (ToolSpec tool : tools) {
            system.append("- ").append(tool.name()).append(": ").append(tool.description()).append('\n');
        }
        return messages(system, prompt);
    }

    /** Step 2: the one function the model answers with. */
    static ToolDefinition toolFunction(List<ToolSpec> tools) {
        return function(SELECT_TOOLS, "Selects the tools the request needs.",
                "tools", "The names of the tools to use",
                tools.stream().map(ToolSpec::name).toList());
    }

    /**
     * The names the model picked, from its call to {@code function} or else from its text,
     * keeping only those in {@code offered}, in the order picked.
     */
    static List<String> picked(ChatCompletion completion, String function, String argument,
                               List<String> offered) {
        Set<String> allowed = Set.copyOf(offered);
        Set<String> picked = new LinkedHashSet<>();
        boolean answeredByCall = false;
        for (ToolCall call : completion.toolCalls()) {
            if (!call.name().equals(function)) {
                continue;
            }
            answeredByCall = true;
            try {
                for (Object name : ToolArguments.parse(call.arguments()).list(argument)) {
                    if (name instanceof String text && allowed.contains(text.strip())) {
                        picked.add(text.strip());
                    }
                }
            } catch (ToolArgumentException e) {
                // A malformed pick picks nothing; the text below may still name something.
                answeredByCall = false;
            }
        }
        if (!answeredByCall) {
            picked.addAll(mentioned(completion.text(), offered));
        }
        return List.copyOf(picked);
    }

    /** The names in {@code offered} that appear in {@code text} as whole words, in offered order. */
    static List<String> mentioned(String text, List<String> offered) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        for (String name : offered) {
            Pattern word = Pattern.compile(
                    "(?<![A-Za-z0-9_-])" + Pattern.quote(name) + "(?![A-Za-z0-9_-])");
            if (word.matcher(text).find()) {
                found.add(name);
            }
        }
        return found;
    }

    /** The list, which stays the same between questions, first; then the prompt, which does not. */
    private static List<PromptMessage> messages(StringBuilder system, String prompt) {
        return List.of(
                PromptMessage.system(system.toString().strip()).asCacheBreakpoint(),
                PromptMessage.user(prompt));
    }

    private static ToolDefinition function(String name, String description, String argument,
                                           String argumentDescription, List<String> allowed) {
        ToolSchema schema = ToolSchema.builder()
                .choices(argument, argumentDescription, true, allowed)
                .build();
        return new ToolDefinition(name, description, schema.json());
    }
}
