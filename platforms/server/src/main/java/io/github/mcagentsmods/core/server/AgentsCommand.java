package io.github.mcagentsmods.core.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.shared.CoreRuntime;
import io.github.mcagentsmods.core.shared.environment.SideEntrypoint;
import io.github.mcagentsmods.core.shared.server.CommandCaller;
import io.github.mcagentsmods.core.shared.server.ServerEntrypoint;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

import java.util.List;
import java.util.function.Function;

/**
 * {@code /agents} on a dedicated server: the server owner's credentials, managed in game.
 *
 * <pre>
 * /agents                                  credential status
 * /agents status                           credential status
 * /agents reload                           re-read the credential file
 * /agents {platform} token add {token}     store a key
 * /agents {platform} token remove {handle} remove a key, by the handle tab completion shows
 * </pre>
 *
 * <p>The keys are the server owner's money, so the command is guarded twice. Brigadier's
 * {@code requires} hides the whole tree from anyone below {@link CommandCaller#OWNER_LEVEL}, so
 * a player cannot see it, complete it or run it; and every call still goes through
 * {@link ServerEntrypoint}, whose authority refuses the same callers, so a route that skips
 * Brigadier skips nothing. Every reply is sent to the caller alone, never broadcast to other
 * operators, and never contains a key.</p>
 */
public final class AgentsCommand {

    /** The command's name. */
    public static final String NAME = "agents";

    private AgentsCommand() {
    }

    /**
     * Adds {@code /agents} to a server's command tree.
     *
     * @param dispatcher The server's dispatcher, from the loader's command registration event.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(NAME)
                .requires(source -> caller(source).hasLevel(CommandCaller.OWNER_LEVEL))
                .executes(AgentsCommand::status)
                .then(Commands.literal("status").executes(AgentsCommand::status))
                .then(Commands.literal("reload").executes(context ->
                        reply(context, server -> List.of(server.reload(caller(context.getSource()))))));

        for (LlmVendor vendor : LlmVendor.values()) {
            root.then(Commands.literal(vendor.code()).then(Commands.literal("token")
                    .then(Commands.literal("add")
                            .then(Commands.argument("token", StringArgumentType.greedyString())
                                    .executes(context -> reply(context, server -> List.of(server.addToken(
                                            caller(context.getSource()), vendor.code(),
                                            StringArgumentType.getString(context, "token")))))))
                    .then(Commands.literal("remove")
                            .then(Commands.argument("handle", StringArgumentType.greedyString())
                                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                            removable(context.getSource(), vendor), builder))
                                    .executes(context -> reply(context, server -> List.of(server.removeToken(
                                            caller(context.getSource()), vendor.code(),
                                            StringArgumentType.getString(context, "handle")))))))));
        }
        dispatcher.register(root);
    }

    /**
     * Who is running a command, as the server knows them: the player it authenticated, or the
     * console or command block, at the permission level the server assigned.
     *
     * @param source The command's source.
     * @return The caller to check.
     */
    static CommandCaller caller(CommandSourceStack source) {
        int level = level(source.permissions());
        String name = source.getTextName() == null || source.getTextName().isBlank()
                ? "Unknown"
                : source.getTextName();
        ServerPlayer player = source.getPlayer();
        return player == null
                ? new CommandCaller(null, name, level)
                : CommandCaller.player(player.getUUID(), name, level);
    }

    /**
     * Reads a permission set as Minecraft's 0 to 4 level, the scale {@link CommandCaller} uses.
     *
     * @param permissions What the server granted the source.
     * @return The highest command level the set holds.
     */
    static int level(PermissionSet permissions) {
        if (permissions.hasPermission(Permissions.COMMANDS_OWNER)) {
            return 4;
        }
        if (permissions.hasPermission(Permissions.COMMANDS_ADMIN)) {
            return 3;
        }
        if (permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return 2;
        }
        if (permissions.hasPermission(Permissions.COMMANDS_MODERATOR)) {
            return 1;
        }
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        return reply(context, server -> server.status(caller(context.getSource())));
    }

    /** The masked handles of a platform's stored keys, for {@code remove}'s suggestions. */
    private static List<String> removable(CommandSourceStack source, LlmVendor vendor) {
        ServerEntrypoint server = server();
        return server == null ? List.of() : server.removableHandles(caller(source), vendor.code());
    }

    /**
     * Runs a command against the server entry point and sends its lines to the caller alone.
     *
     * @return {@code 1} when it ran, {@code 0} when core's server half is not running.
     */
    private static int reply(CommandContext<CommandSourceStack> context,
                             Function<ServerEntrypoint, List<String>> action) {
        CommandSourceStack source = context.getSource();
        ServerEntrypoint server = server();
        if (server == null) {
            source.sendFailure(Component.literal("MCAgents is not ready."));
            return 0;
        }
        for (String line : action.apply(server)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /** Core's running server half, or null when it is not running. */
    private static ServerEntrypoint server() {
        SideEntrypoint running = CoreRuntime.running();
        return running instanceof ServerEntrypoint server ? server : null;
    }
}
