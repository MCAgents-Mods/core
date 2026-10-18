package io.github.mcagentsmods.core.forge;

import io.github.mcagentsmods.core.client.agent.AgentsKeys;
import io.github.mcagentsmods.core.client.agent.AgentsRuntime;
import io.github.mcagentsmods.core.client.hologram.HologramClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge's client half: the hologram's key, tick, click and wheel, and the agents menu's G key
 * and settings. Only {@link CoreForgeMod} calls it, and only on the client. Forge has no event
 * for drawing in the level, so the drawing comes from {@code mixin.LevelRendererMixin} instead.
 */
final class CoreForgeClient {

    private CoreForgeClient() {
    }

    static void start() {
        // Forge's key event takes the key alone, so the Controls category is
        // registered the vanilla way. Both happen before the game's options exist,
        // which Forge requires.
        KeyMapping.Category category = KeyMapping.Category.register(HologramClient.KEY_CATEGORY);
        KeyMapping move = HologramClient.createMoveKey(category);
        KeyMapping agents = AgentsKeys.create(category);
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(move);
            event.register(agents);
        });
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            HologramClient.onEndTick(Minecraft.getInstance());
            AgentsKeys.onEndTick(Minecraft.getInstance());
        });
        // The attack key's event changes between Minecraft releases, so AttackListener
        // is a class of its own. A listener that returns true cancels the event: the block
        // behind a hologram is not hit, and the hotbar slot does not change.
        AttackListener.listen();
        InputEvent.MouseScrollingEvent.BUS.addListener(event -> {
            return HologramClient.onScroll(event.getDeltaY());
        });

        AgentsRuntime.start(FMLPaths.GAMEDIR.get());
    }
}
