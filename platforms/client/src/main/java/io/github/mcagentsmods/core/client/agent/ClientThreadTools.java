package io.github.mcagentsmods.core.client.agent;

import io.github.mcagentsmods.core.tool.AsyncTool;
import io.github.mcagentsmods.core.tool.Tool;
import io.github.mcagentsmods.core.tool.ToolResult;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Turns a tool that reads or changes the game into one that runs on the game's thread.
 *
 * <p>Tools run on worker threads, and most of Minecraft may only be touched from the client
 * thread. Register a game tool through {@link #onClientThread} and its body runs there, between
 * frames, while the call keeps its worker slot until it is done:
 *
 * <pre>{@code
 * Agents.tools().registerAsync(spec, ClientThreadTools.onClientThread(arguments -> {
 *     LocalPlayer player = Minecraft.getInstance().player;
 *     return player == null
 *             ? ToolResult.error("the player is not in a world")
 *             : ToolResult.ok(player.blockPosition().toShortString());
 * }));
 * }</pre>
 *
 * <p>Keep the body short: the game does not draw a frame while it runs.
 */
public final class ClientThreadTools {

    private ClientThreadTools() {
        throw new AssertionError("No instances.");
    }

    /** {@code body}, run on the client thread when the tool is called. */
    public static AsyncTool onClientThread(Tool body) {
        Objects.requireNonNull(body, "body");
        return arguments -> {
            CompletableFuture<ToolResult> result = new CompletableFuture<>();
            Minecraft.getInstance().execute(() -> {
                try {
                    result.complete(body.call(arguments));
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            return result;
        };
    }
}
