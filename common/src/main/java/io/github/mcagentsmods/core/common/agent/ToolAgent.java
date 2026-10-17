package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.agent.Agent;
import io.github.mcagentsmods.core.api.agent.AgentAnswer;
import io.github.mcagentsmods.core.api.agent.AgentRequest;
import io.github.mcagentsmods.core.tool.ToolCategory;
import io.github.mcagentsmods.core.tool.ToolExecutor;
import io.github.mcagentsmods.core.tool.ToolInvocation;
import io.github.mcagentsmods.core.tool.ToolLookup;
import io.github.mcagentsmods.core.tool.ToolOutcome;
import io.github.mcagentsmods.core.tool.ToolRegistry;
import io.github.mcagentsmods.core.tool.ToolSpec;
import io.github.mcagentsmods.core.api.model.ChatCompletion;
import io.github.mcagentsmods.core.api.model.ChatRequest;
import io.github.mcagentsmods.core.api.model.ModelClient;
import io.github.mcagentsmods.core.api.model.PromptMessage;
import io.github.mcagentsmods.core.api.model.TokenUsage;
import io.github.mcagentsmods.core.api.model.ToolCall;
import io.github.mcagentsmods.core.api.model.ToolDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

/**
 * The agent: picks categories, then tools, then answers with them, through core's model client.
 *
 * <h2>The three steps</h2>
 *
 * <ol>
 *   <li><b>Categories.</b> The prompt and every category's id and description. The model picks
 *       one or more. Skipped when there is only one category to pick from.</li>
 *   <li><b>Tools.</b> The prompt and the name and description of each tool in the picked
 *       categories — never their schemas. The model picks the tools it needs.</li>
 *   <li><b>Answer.</b> The request's conversation, offering the picked tools in full. When the
 *       model calls them they run on the {@link ToolExecutor}, their results go back, and the
 *       model is asked again — for as many rounds as the request allows, 1 unless the player
 *       set more. After the last round the model is told to answer from what it has.</li>
 * </ol>
 *
 * <p>With no tools registered, or none picked, the answer step offers no tools and the question
 * costs one request, as it would without tools.
 *
 * <p>The registry is read once per question, as a snapshot, and used for every step: the tools
 * the model was shown are the tools its calls run against, even if a mod removes one meanwhile.
 * A call to a tool that was not picked is answered with an error, not run.
 */
public final class ToolAgent implements Agent {

    /** Said to the model after its last round, so it answers rather than asking again. */
    static final String LAST_ROUND = "You have used every round of tool calls you may make for "
            + "this request. Do not call any more tools. Answer the player now, using the tool "
            + "results above.";

    /** Said to the player when the model never stopped asking for tools and wrote nothing. */
    static final String STOPPED_SHORT = "I kept needing more tools and did not reach an answer. "
            + "Ask again, or narrow the question.";

    private final ModelClient client;
    private final ToolRegistry registry;
    private final ToolExecutor executor;
    private final IntSupplier playerToolRounds;

    /**
     * @param client           where every request goes
     * @param registry         the tools to choose from
     * @param executor         what runs the tools the model calls
     * @param playerToolRounds the player's setting, read per question, for a request that does
     *                         not set its own
     */
    public ToolAgent(ModelClient client, ToolRegistry registry, ToolExecutor executor,
                     IntSupplier playerToolRounds) {
        this.client = Objects.requireNonNull(client, "client");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.playerToolRounds = Objects.requireNonNull(playerToolRounds, "playerToolRounds");
    }

    @Override
    public CompletableFuture<AgentAnswer> answer(AgentRequest request) {
        Objects.requireNonNull(request, "request");
        Question question = new Question(request, registry.snapshot(), rounds(request));
        return pickCategories(question)
                .thenCompose(categories -> pickTools(question, categories))
                .thenCompose(tools -> answerWith(question, tools));
    }

    private int rounds(AgentRequest request) {
        int rounds = request.toolRounds() == AgentRequest.PLAYER_SETTING
                ? playerToolRounds.getAsInt()
                : request.toolRounds();
        return Math.max(1, Math.min(AgentRequest.MAX_TOOL_ROUNDS, rounds));
    }

    // Step 1 ----------------------------------------------------------------------------------

    private CompletableFuture<List<String>> pickCategories(Question question) {
        List<ToolCategory> categories = question.snapshot.categories().stream()
                .filter(category -> !question.request.isLimitedToCategories()
                        || question.request.categories().contains(category.id()))
                .toList();
        List<String> ids = categories.stream().map(ToolCategory::id).toList();
        if (ids.size() <= 1) {
            question.categories = ids;
            return CompletableFuture.completedFuture(ids);
        }
        ChatRequest step = new ChatRequest(question.request.model(),
                Selection.categoryMessages(categories, question.request.prompt()),
                List.of(Selection.categoryFunction(categories)));
        return complete(question, step).thenApply(completion -> {
            question.categories = Selection.picked(
                    completion, Selection.SELECT_CATEGORIES, "categories", ids);
            return question.categories;
        });
    }

    // Step 2 ----------------------------------------------------------------------------------

    private CompletableFuture<List<ToolSpec>> pickTools(Question question, List<String> categories) {
        List<ToolSpec> offered = categories.isEmpty() ? List.of() : question.snapshot.toolsIn(categories);
        if (offered.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        ChatRequest step = new ChatRequest(question.request.model(),
                Selection.toolMessages(offered, question.request.prompt()),
                List.of(Selection.toolFunction(offered)));
        List<String> names = offered.stream().map(ToolSpec::name).toList();
        return complete(question, step).thenApply(completion -> question.snapshot.specs(
                Selection.picked(completion, Selection.SELECT_TOOLS, "tools", names)));
    }

    // Step 3 ----------------------------------------------------------------------------------

    private CompletableFuture<AgentAnswer> answerWith(Question question, List<ToolSpec> tools) {
        question.tools = tools.stream().map(ToolSpec::name).toList();
        List<ToolDefinition> definitions = tools.stream()
                .map(spec -> new ToolDefinition(spec.name(), spec.description(), spec.parameters().json()))
                .toList();
        Set<String> picked = Set.copyOf(question.tools);
        // Only the picked tools run: a call to anything else is answered as unknown.
        ToolLookup lookup = name -> picked.contains(name) ? question.snapshot.find(name) : Optional.empty();
        ChatRequest first = new ChatRequest(
                question.request.model(), question.request.conversation(), definitions);
        return round(question, first, lookup);
    }

    private CompletableFuture<AgentAnswer> round(Question question, ChatRequest request,
                                                 ToolLookup lookup) {
        return complete(question, request).thenCompose(completion -> {
            if (!completion.wantsTools()) {
                return CompletableFuture.completedFuture(
                        question.answer(completion.text(), completion.model(), false));
            }
            if (question.roundsRun >= question.rounds) {
                // Asked again after being told to answer: keep what it wrote, if anything.
                String text = completion.text().isBlank() ? STOPPED_SHORT : completion.text();
                return CompletableFuture.completedFuture(question.answer(text, completion.model(), true));
            }
            return executor.run(invocations(completion.toolCalls()), lookup).thenCompose(outcomes -> {
                question.roundsRun++;
                question.results.addAll(outcomes);
                List<PromptMessage> messages = new ArrayList<>(request.messages());
                messages.add(PromptMessage.assistantToolCalls(completion.text(), completion.toolCalls()));
                for (ToolOutcome outcome : outcomes) {
                    messages.add(PromptMessage.toolResult(outcome.callId(), outcome.text()));
                }
                if (question.roundsRun >= question.rounds) {
                    // The tools stay offered, because some providers refuse tool results
                    // without the definitions they answer, but the model is told to stop.
                    messages.add(PromptMessage.user(LAST_ROUND));
                }
                return round(question, request.continuedWith(messages), lookup);
            });
        });
    }

    private CompletableFuture<ChatCompletion> complete(Question question, ChatRequest request) {
        // Every step goes to the provider the question was asked of, whichever step built it.
        return client.complete(request.withProvider(question.request.provider())).thenApply(completion -> {
            question.add(completion.usage());
            return completion;
        });
    }

    /** One question's state as it moves through the steps. Touched by one step at a time. */
    private static final class Question {

        private final AgentRequest request;
        private final ToolRegistry.Snapshot snapshot;
        private final int rounds;
        private final List<ToolOutcome> results = new ArrayList<>();
        private List<String> categories = List.of();
        private List<String> tools = List.of();
        private int roundsRun;
        private int promptTokens;
        private int completionTokens;
        private int cachedTokens;

        private Question(AgentRequest request, ToolRegistry.Snapshot snapshot, int rounds) {
            this.request = request;
            this.snapshot = snapshot;
            this.rounds = rounds;
        }

        private synchronized void add(TokenUsage usage) {
            promptTokens += usage.promptTokens();
            completionTokens += usage.completionTokens();
            cachedTokens += usage.cachedTokens();
        }

        private synchronized AgentAnswer answer(String text, String model, boolean stoppedShort) {
            return new AgentAnswer(text, model,
                    new TokenUsage(promptTokens, completionTokens, cachedTokens),
                    categories, tools, results, roundsRun, stoppedShort);
        }
    }

    /** The model's calls, as the executor takes them. */
    private static List<ToolInvocation> invocations(List<ToolCall> calls) {
        return calls.stream()
                .map(call -> new ToolInvocation(call.id(), call.name(), call.arguments()))
                .toList();
    }
}
