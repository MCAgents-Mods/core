package io.github.mcagentsmods.core.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.mcagentsmods.core.api.AgentException;
import io.github.mcagentsmods.core.api.chat.ChatMessage;
import io.github.mcagentsmods.core.api.chat.ChatRequest;
import io.github.mcagentsmods.core.api.chat.ChatResponse;
import io.github.mcagentsmods.core.api.chat.ChatRole;
import io.github.mcagentsmods.core.api.chat.TokenUsage;
import io.github.mcagentsmods.core.api.llm.LlmCredentials;
import io.github.mcagentsmods.core.api.llm.ModelInfo;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The client for every vendor speaking the OpenAI chat completions dialect —
 * OpenRouter, OpenAI, and DeepSeek.
 *
 * <p>One class serves all three because they differ only in base URL and model
 * naming, both of which come from
 * {@link io.github.mcagentsmods.core.api.llm.LlmVendor} and
 * {@link LlmCredentials}. Writing three near identical clients would mean three
 * places to fix the next time the dialect changes.</p>
 *
 * <p>Package-private on purpose: nothing outside {@code common} constructs a
 * client.</p>
 */
final class OpenAiCompatibleClient extends HttpLlmClient {

    /**
     * The endpoint one exchange is sent to.
     */
    private static final String CHAT_PATH = "/chat/completions";

    /**
     * The endpoint the model catalog is read from.
     */
    private static final String MODELS_PATH = "/models";

    /**
     * Builds a client for an OpenAI compatible vendor.
     *
     * @param credentials The key, endpoint, and timeout to use. Its vendor must
     *                    be one that reports
     *                    {@link io.github.mcagentsmods.core.api.llm.LlmVendor#usesOpenAiDialect()}.
     */
    OpenAiCompatibleClient(LlmCredentials credentials) {
        super(credentials);
    }

    /**
     * {@inheritDoc}
     *
     * <p>All three vendors take the key as a bearer token.</p>
     */
    @Override
    protected void authorize(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + credentials.apiKey());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Framing instructions are sent as a leading {@code system} message,
     * which is where this dialect expects them.</p>
     */
    @Override
    public CompletableFuture<ChatResponse> chat(ChatRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return post(CHAT_PATH, body(request)).thenApply(this::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<List<ModelInfo>> listModels() {
        return get(MODELS_PATH).thenApply(this::toModels);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the model catalog, which is the cheapest call that still proves
     * the endpoint is reachable and the key is accepted.</p>
     */
    @Override
    public CompletableFuture<Boolean> ping() {
        return get(MODELS_PATH).handle((document, failure) -> failure == null);
    }

    /**
     * Renders a request into the dialect's wire format.
     *
     * @param request What to ask.
     * @return The request document.
     */
    private JsonObject body(ChatRequest request) {
        JsonObject document = new JsonObject();
        document.addProperty("model", request.model());

        JsonArray messages = new JsonArray();
        if (request.hasSystemPrompt()) {
            messages.add(message(ChatRole.SYSTEM.code(), request.systemPrompt()));
        }
        for (ChatMessage turn : request.messages()) {
            messages.add(message(turn.role().code(), turn.content()));
        }
        document.add("messages", messages);

        if (request.hasMaxTokens()) {
            document.addProperty("max_tokens", request.maxTokens());
        }
        if (request.hasTemperature()) {
            document.addProperty("temperature", request.temperature());
        }
        if (request.hasTopP()) {
            document.addProperty("top_p", request.topP());
        }
        if (!request.stopSequences().isEmpty()) {
            JsonArray stop = new JsonArray();
            request.stopSequences().forEach(stop::add);
            document.add("stop", stop);
        }
        return document;
    }

    /**
     * Builds one message object.
     *
     * @param role The wire name of the author's role.
     * @param content The text of the turn.
     * @return The message object.
     */
    private JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    /**
     * Reads a completion document into the core's response model.
     *
     * @param document The vendor's answer.
     * @return The reply.
     * @throws AgentException When the answer carries no choice, which means the
     *                        vendor accepted the call and returned nothing
     *                        usable.
     */
    private ChatResponse toResponse(JsonObject document) {
        JsonElement choices = document.get("choices");
        if (choices == null || !choices.isJsonArray() || choices.getAsJsonArray().isEmpty()) {
            throw new AgentException(vendor(), vendor().code() + " returned no completion choices");
        }

        JsonObject choice = choices.getAsJsonArray().get(0).getAsJsonObject();
        JsonObject message = optObject(choice, "message");

        return new ChatResponse(
                optString(document, "id", ""),
                optString(document, "model", ""),
                message == null ? "" : optString(message, "content", ""),
                optString(choice, "finish_reason", ""),
                toUsage(optObject(document, "usage")));
    }

    /**
     * Reads the usage block, which several providers omit.
     *
     * @param usage The usage object, or {@code null} when absent.
     * @return The token counts, or {@link TokenUsage#UNKNOWN}.
     */
    private TokenUsage toUsage(JsonObject usage) {
        if (usage == null) {
            return TokenUsage.UNKNOWN;
        }
        return new TokenUsage(
                optInt(usage, "prompt_tokens", -1),
                optInt(usage, "completion_tokens", -1),
                optInt(usage, "total_tokens", -1));
    }

    /**
     * Reads a model catalog document.
     *
     * <p>Entries without an {@code id} are skipped rather than failing the
     * whole call: one malformed row should not cost a server owner the entire
     * catalog.</p>
     *
     * @param document The vendor's answer.
     * @return The models it listed.
     */
    private List<ModelInfo> toModels(JsonObject document) {
        JsonElement data = document.get("data");
        if (data == null || !data.isJsonArray()) {
            return List.of();
        }

        List<ModelInfo> models = new ArrayList<>();
        for (JsonElement element : data.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String id = optString(entry, "id", "");
            if (id.isBlank()) {
                continue;
            }
            // OpenRouter reports a display name and a context window; OpenAI
            // and DeepSeek report neither, so both fall back.
            models.add(new ModelInfo(
                    id,
                    optString(entry, "name", id),
                    optInt(entry, "context_length", -1),
                    vendor()));
        }
        return List.copyOf(models);
    }
}
