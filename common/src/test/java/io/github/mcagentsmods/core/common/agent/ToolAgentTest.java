package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.agent.AgentAnswer;
import io.github.mcagentsmods.core.api.agent.AgentRequest;
import io.github.mcagentsmods.core.tool.ExecutionMode;
import io.github.mcagentsmods.core.tool.ToolExecutor;
import io.github.mcagentsmods.core.tool.ToolRegistry;
import io.github.mcagentsmods.core.tool.ToolResult;
import io.github.mcagentsmods.core.tool.ToolSchema;
import io.github.mcagentsmods.core.tool.ToolSpec;
import io.github.mcagentsmods.core.tool.WorkerPool;
import io.github.mcagentsmods.core.api.chat.ChatRole;
import io.github.mcagentsmods.core.api.model.ChatCompletion;
import io.github.mcagentsmods.core.api.model.ChatRequest;
import io.github.mcagentsmods.core.api.model.PromptMessage;
import io.github.mcagentsmods.core.api.model.ToolDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.mcagentsmods.core.common.agent.ScriptedModelClient.call;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolAgentTest {

    private static final String MODEL = "test/model";

    private final WorkerPool pool = new WorkerPool(2);
    private final ToolRegistry registry = new ToolRegistry();
    private final ScriptedModelClient model = new ScriptedModelClient();
    private final AtomicInteger playerRounds = new AtomicInteger(1);
    private ToolAgent agent;

    @BeforeEach
    void buildAgent() {
        agent = new ToolAgent(model, registry, new ToolExecutor(pool), playerRounds::get);
    }

    @AfterEach
    void closePool() {
        pool.close();
    }

    private void tool(String name, String... categories) {
        registry.register(ToolSpec.builder(name)
                .description("Describes " + name)
                .categories(List.of(categories))
                .parameters(ToolSchema.builder().string("about", "What to describe", false).build())
                .mode(ExecutionMode.CONCURRENT)
                .build(), arguments -> ToolResult.ok(name + " says " + arguments.string("about", "hi")));
    }

    private AgentAnswer ask(AgentRequest request) throws Exception {
        return agent.answer(request).get(10, TimeUnit.SECONDS);
    }

    private AgentAnswer ask(String prompt) throws Exception {
        return ask(AgentRequest.of(MODEL, prompt));
    }

    private static List<String> offered(ChatRequest request) {
        return request.tools().stream().map(ToolDefinition::name).toList();
    }

    @Test
    void withNoToolsItAsksOnceAndOffersNothing() throws Exception {
        model.thenSays("Hello.");

        AgentAnswer answer = ask("Hi");

        assertEquals("Hello.", answer.text());
        assertEquals(1, model.requests.size());
        assertTrue(model.requests.get(0).tools().isEmpty());
        assertFalse(answer.usedTools());
    }

    @Test
    void walksTheThreeStepsAndAnswersWithTheToolResult() throws Exception {
        registry.declareCategory("world", "Blocks and biomes");
        registry.declareCategory("clock", "Time of day");
        tool("biome", "world");
        tool("block", "world");
        tool("time", "clock");
        model.thenCalls(call("c1", "select_categories", "{\"categories\":[\"world\"]}"))
                .thenCalls(call("c2", "select_tools", "{\"tools\":[\"biome\"]}"))
                .thenCalls(call("c3", "biome", "{\"about\":\"here\"}"))
                .thenSays("You are in a forest.");

        AgentAnswer answer = ask("Where am I?");

        assertEquals("You are in a forest.", answer.text());
        assertEquals(4, model.requests.size());

        // Step 1: every category, described, and the one function to answer with.
        ChatRequest step1 = model.requests.get(0);
        assertEquals(List.of("select_categories"), offered(step1));
        String categories = step1.messages().get(0).text();
        assertTrue(categories.contains("- clock: Time of day"));
        assertTrue(categories.contains("- world: Blocks and biomes"));
        assertEquals("Where am I?", step1.messages().get(1).text());

        // Step 2: only the tools in the picked category, by name and description only.
        ChatRequest step2 = model.requests.get(1);
        assertEquals(List.of("select_tools"), offered(step2));
        String tools = step2.messages().get(0).text();
        assertTrue(tools.contains("- biome: Describes biome"));
        assertTrue(tools.contains("- block: Describes block"));
        assertFalse(tools.contains("time"));
        assertFalse(tools.contains("\"type\""));

        // Step 3: the picked tool in full, then its result.
        ChatRequest answerStep = model.requests.get(2);
        assertEquals(List.of("biome"), offered(answerStep));
        assertTrue(answerStep.tools().get(0).parametersSchema().contains("\"about\""));
        List<PromptMessage> followUp = model.requests.get(3).messages();
        PromptMessage result = followUp.stream()
                .filter(message -> message.role() == ChatRole.TOOL)
                .findFirst()
                .orElseThrow();
        assertEquals("c3", result.toolCallId());
        assertEquals("biome says here", result.text());

        assertEquals(List.of("world"), answer.categories());
        assertEquals(List.of("biome"), answer.tools());
        assertEquals(1, answer.toolRounds());
        assertEquals(1, answer.toolResults().size());
        assertEquals(40, answer.usage().promptTokens());
        assertEquals(20, answer.usage().completionTokens());
    }

    @Test
    void picksSeveralCategoriesAtOnceAndListsAToolInBothOnlyOnce() throws Exception {
        tool("look", "world", "player");
        tool("walk", "player");
        tool("time", "clock");
        model.thenCalls(call("c1", "select_categories", "{\"categories\":[\"world\",\"player\"]}"))
                .thenCalls(call("c2", "select_tools", "{\"tools\":[]}"))
                .thenSays("Nothing needed.");

        AgentAnswer answer = ask("Look around and walk");

        String tools = model.requests.get(1).messages().get(0).text();
        assertEquals(1, tools.split("- look:", -1).length - 1);
        assertTrue(tools.contains("- walk:"));
        assertEquals(List.of("world", "player"), answer.categories());
        // No tool picked: the answer step offers none.
        assertTrue(model.requests.get(2).tools().isEmpty());
        assertEquals("Nothing needed.", answer.text());
    }

    @Test
    void skipsTheCategoryStepWhenThereIsOnlyOne() throws Exception {
        tool("biome", "world");
        tool("block", "world");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[\"block\"]}"))
                .thenSays("Stone.");

        AgentAnswer answer = ask("What block?");

        assertEquals(2, model.requests.size());
        assertEquals(List.of("select_tools"), offered(model.requests.get(0)));
        assertEquals(List.of("block"), offered(model.requests.get(1)));
        assertEquals("Stone.", answer.text());
    }

    @Test
    void readsAPickWrittenAsTextAndIgnoresNamesNotOffered() throws Exception {
        tool("biome", "world");
        tool("time", "clock");
        tool("weather", "sky");
        model.thenSays("I would use world and sky, maybe skylight or the nether.")
                .thenCalls(call("c2", "select_tools", "{\"tools\":[\"weather\",\"made_up\"]}"))
                .thenSays("Raining.");

        AgentAnswer answer = ask("Is it raining?");

        assertEquals(List.of("sky", "world"), answer.categories());
        assertEquals(List.of("weather"), answer.tools());
    }

    @Test
    void stopsAfterOneRoundByDefaultAndTellsTheModelToAnswer() throws Exception {
        tool("biome", "world");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[\"biome\"]}"))
                .thenCalls(call("c2", "biome", "{}"))
                .thenCalls(call("c3", "biome", "{}"))
                .thenSays("unused");

        AgentAnswer answer = ask("Where?");

        assertEquals(3, model.requests.size());
        List<PromptMessage> last = model.requests.get(2).messages();
        assertEquals(ToolAgent.LAST_ROUND, last.get(last.size() - 1).text());
        assertTrue(answer.stoppedShort());
        assertEquals(ToolAgent.STOPPED_SHORT, answer.text());
        assertEquals(1, answer.toolRounds());
    }

    @Test
    void runsAsManyRoundsAsThePlayerSet() throws Exception {
        playerRounds.set(3);
        tool("biome", "world");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[\"biome\"]}"))
                .thenCalls(call("r1", "biome", "{\"about\":\"one\"}"))
                .thenCalls(call("r2", "biome", "{\"about\":\"two\"}"))
                .thenSays("Done in two.");

        AgentAnswer answer = ask("Where?");

        assertEquals("Done in two.", answer.text());
        assertEquals(2, answer.toolRounds());
        assertFalse(answer.stoppedShort());
        assertEquals(List.of("biome says one", "biome says two"),
                answer.toolResults().stream().map(outcome -> outcome.text()).toList());
    }

    @Test
    void aRequestsOwnRoundsWinOverThePlayers() throws Exception {
        tool("biome", "world");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[\"biome\"]}"))
                .thenCalls(call("r1", "biome", "{}"))
                .thenCalls(call("r2", "biome", "{}"))
                .thenSays("Two rounds.");

        AgentAnswer answer = ask(AgentRequest.of(MODEL, "Where?").withToolRounds(2));

        assertEquals("Two rounds.", answer.text());
        assertEquals(2, answer.toolRounds());
    }

    @Test
    void aCallToAToolThatWasNotPickedIsAnsweredWithAnError() throws Exception {
        tool("biome", "world");
        tool("block", "world");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[\"biome\"]}"))
                .thenCalls(call("c2", "block", "{}"))
                .thenSays("Sorry.");

        AgentAnswer answer = ask("What block?");

        assertTrue(answer.toolResults().get(0).isError());
        assertTrue(answer.toolResults().get(0).text().contains("no tool called 'block'"));
    }

    @Test
    void aRequestLimitedToCategoriesSeesOnlyThose() throws Exception {
        tool("biome", "world");
        tool("time", "clock");
        tool("weather", "sky");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[\"time\"]}"))
                .thenSays("Noon.");

        AgentAnswer answer = ask(AgentRequest.of(MODEL, "Time?").withCategories(List.of("clock")));

        // One category left, so step 1 is skipped.
        assertEquals(List.of("select_tools"), offered(model.requests.get(0)));
        assertEquals(List.of("clock"), answer.categories());
        assertEquals("Noon.", answer.text());
    }

    @Test
    void answersFromTheConversationTheRequestCarries() throws Exception {
        tool("biome", "world");
        model.thenCalls(call("c1", "select_tools", "{\"tools\":[]}"))
                .thenSays("As before.");
        List<PromptMessage> conversation = List.of(
                PromptMessage.system("You are a guide."),
                PromptMessage.user("Earlier question"),
                PromptMessage.assistant("Earlier answer"),
                PromptMessage.user("And now?"));

        ask(AgentRequest.of(MODEL, "And now?").withConversation(conversation));

        assertEquals(conversation, model.requests.get(1).messages());
        assertEquals("And now?", model.requests.get(0).messages().get(1).text());
    }

    @Test
    void aToolRemovedMidQuestionStillRunsForThatQuestion() throws Exception {
        tool("biome", "world");
        ScriptedModelClient removing = new ScriptedModelClient() {
            @Override
            public synchronized CompletableFuture<ChatCompletion> complete(ChatRequest request) {
                registry.unregister("biome");
                return super.complete(request);
            }
        };
        removing.thenCalls(call("c1", "select_tools", "{\"tools\":[\"biome\"]}"))
                .thenCalls(call("c2", "biome", "{}"))
                .thenSays("Forest.");
        ToolAgent snapshotAgent = new ToolAgent(removing, registry, new ToolExecutor(pool), () -> 1);

        AgentAnswer answer = snapshotAgent.answer(AgentRequest.of(MODEL, "Where?"))
                .get(10, TimeUnit.SECONDS);

        assertEquals("biome says hi", answer.toolResults().get(0).text());
    }
}
