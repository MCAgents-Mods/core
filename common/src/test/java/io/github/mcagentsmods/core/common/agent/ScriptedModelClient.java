package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.model.ChatCompletion;
import io.github.mcagentsmods.core.api.model.ChatRequest;
import io.github.mcagentsmods.core.api.model.ModelClient;
import io.github.mcagentsmods.core.api.model.TokenUsage;
import io.github.mcagentsmods.core.api.model.ToolCall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** A model that answers from a script and remembers every request it was sent. */
class ScriptedModelClient implements ModelClient {

    final List<ChatRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private final Deque<ChatCompletion> script = new ArrayDeque<>();

    ScriptedModelClient then(ChatCompletion completion) {
        script.add(completion);
        return this;
    }

    ScriptedModelClient thenSays(String text) {
        return then(new ChatCompletion(text, "test/model", new TokenUsage(10, 5)));
    }

    ScriptedModelClient thenCalls(ToolCall... calls) {
        return then(new ChatCompletion("", "test/model", new TokenUsage(10, 5), List.of(calls)));
    }

    static ToolCall call(String id, String name, String arguments) {
        return new ToolCall(id, name, arguments);
    }

    @Override
    public synchronized CompletableFuture<ChatCompletion> complete(ChatRequest request) {
        requests.add(request);
        ChatCompletion next = script.poll();
        if (next == null) {
            return CompletableFuture.failedFuture(new AssertionError("The script ran out at request "
                    + requests.size()));
        }
        // Answered on another thread, as a real client would.
        return CompletableFuture.supplyAsync(() -> next);
    }

    @Override
    public CompletableFuture<float[]> embed(String model, String text) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public void close() {
    }
}
