package io.github.mcagentsmods.core.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/**
 * The client for Anthropic's Messages API.
 *
 * <p>Anthropic is the one supported vendor that does not speak the OpenAI
 * dialect, and it differs in four ways that all have to be handled here:
 * framing instructions travel as a top level {@code system} field rather than a
 * message, a token bound is mandatory, the key goes in {@code x-api-key}
 * instead of an {@code Authorization} header, and every request must state an
 * API version.</p>
 *
 * <p>Package-private on purpose: nothing outside {@code common} constructs a
 * client.</p>
 */
final class AnthropicClient extends HttpLlmClient {

    /**
     * The endpoint one exchange is sent to.
     */
    private static final String MESSAGES_PATH = "/messages";

    /**
     * The endpoint the model catalog is read from.
     */
    private static final String MODELS_PATH = "/models";

    /**
     * The API version every request declares.
     *
     * <p>Anthropic dates its API rather than numbering it, and pins behavior to
     * the date sent. Naming one here means a change on their side cannot alter
     * how the core behaves without a deliberate bump.</p>
     */
    private static final String API_VERSION = "2023-06-01";

    /**
     * The token bound applied when the caller set none.
     *
     * <p>Anthropic rejects a request without {@code max_tokens}, so unlike the
     * other three vendors there is no "leave it to the service" option — a
     * number has to be chosen, and this is it.</p>
     */
    private static final int DEFAULT_MAX_TOKENS = 4096;

    /**
     * Builds a client for Anthropic.
     *
     * @param credentials The key, endpoint, and timeout to use.
     */
    AnthropicClient(LlmCredentials credentials) {
        super(credentials);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the API version alongside the key, since Anthropic rejects a
     * request that omits it.</p>
     */
    @Override
    protected void authorize(HttpRequest.Builder builder) {
        builder.header("x-api-key", credentials.apiKey());
        builder.header("anthropic-version", API_VERSION);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Any {@link ChatRole#SYSTEM} message in the request is lifted out of
     * the message list and merged into the top level {@code system} field,
     * because the Messages API does not accept that role inline.</p>
     */
    @Override
    public CompletableFuture<ChatResponse> chat(ChatRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return post(MESSAGES_PATH, body(request)).thenApply(this::toResponse);
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
     * Renders a request into the Messages API wire format.
     *
     * @param request What to ask.
     * @return The request document.
     */
    private JsonObject body(ChatRequest request) {
        JsonObject document = new JsonObject();
        document.addProperty("model", request.model());
        document.addProperty("max_tokens", request.hasMaxTokens() ? request.maxTokens() : DEFAULT_MAX_TOKENS);

        String system = systemPrompt(request);
        if (!system.isEmpty()) {
            document.addProperty("system", system);
        }

        JsonArray messages = new JsonArray();
        for (ChatMessage turn : request.messages()) {
            if (turn.role() == ChatRole.SYSTEM) {
                // Already merged into the top level system field above.
                continue;
            }
            JsonObject message = new JsonObject();
            message.addProperty("role", turn.role().code());
            message.addProperty("content", turn.content());
            messages.add(message);
        }
        document.add("messages", messages);

        if (request.hasTemperature()) {
            document.addProperty("temperature", request.temperature());
        }
        if (request.hasTopP()) {
            document.addProperty("top_p", request.topP());
        }
        if (!request.stopSequences().isEmpty()) {
            JsonArray stop = new JsonArray();
            request.stopSequences().forEach(stop::add);
            document.add("stop_sequences", stop);
        }
        return document;
    }

    /**
     * Collects every piece of framing instruction into the single string the
     * Messages API accepts.
     *
     * <p>The request's own system prompt comes first, then any
     * {@link ChatRole#SYSTEM} messages in the order they appear, separated by
     * blank lines. Merging rather than dropping means a caller that built a
     * request for another vendor gets the same behavior here.</p>
     *
     * @param request The request being rendered.
     * @return The merged instructions, empty when there are none.
     */
    private String systemPrompt(ChatRequest request) {
        StringJoiner joiner = new StringJoiner("\n\n");
        if (request.hasSystemPrompt()) {
            joiner.add(request.systemPrompt());
        }
        for (ChatMessage turn : request.messages()) {
            if (turn.role() == ChatRole.SYSTEM && !turn.content().isBlank()) {
                joiner.add(turn.content());
            }
        }
        return joiner.toString();
    }

    /**
     * Reads a Messages API answer into the core's response model.
     *
     * <p>The reply arrives as a list of content blocks. Only text blocks are
     * kept, concatenated in order; other block types carry no text and would
     * contribute nothing.</p>
     *
     * @param document The vendor's answer.
     * @return The reply.
     */
    private ChatResponse toResponse(JsonObject document) {
        StringBuilder text = new StringBuilder();
        JsonElement content = document.get("content");
        if (content != null && content.isJsonArray()) {
            for (JsonElement element : content.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject block = element.getAsJsonObject();
                if ("text".equals(optString(block, "type", ""))) {
                    text.append(optString(block, "text", ""));
                }
            }
        }

        return new ChatResponse(
                optString(document, "id", ""),
                optString(document, "model", ""),
                text.toString(),
                optString(document, "stop_reason", ""),
                toUsage(optObject(document, "usage")));
    }

    /**
     * Reads the usage block.
     *
     * <p>Anthropic reports input and output separately and never a total, so
     * the total is summed here — but only when both halves were reported, since
     * summing an unreported {@code -1} would invent a number.</p>
     *
     * @param usage The usage object, or {@code null} when absent.
     * @return The token counts, or {@link TokenUsage#UNKNOWN}.
     */
    private TokenUsage toUsage(JsonObject usage) {
        if (usage == null) {
            return TokenUsage.UNKNOWN;
        }
        int input = optInt(usage, "input_tokens", -1);
        int output = optInt(usage, "output_tokens", -1);
        int total = input >= 0 && output >= 0 ? input + output : -1;
        return new TokenUsage(input, output, total);
    }

    /**
     * Reads a model catalog document.
     *
     * <p>Anthropic reports a display name but no context window, so
     * {@link ModelInfo#contextLength()} is always unreported here.</p>
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
            models.add(new ModelInfo(id, optString(entry, "display_name", id), -1, vendor()));
        }
        return List.copyOf(models);
    }
}
