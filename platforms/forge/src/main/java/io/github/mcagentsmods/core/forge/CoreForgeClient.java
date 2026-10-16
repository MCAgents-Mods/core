package io.github.mcagentsmods.core.forge;

import io.github.mcagentsmods.core.client.hologram.HologramClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Forge's client half: the hologram's key, tick, click and wheel. Only {@link CoreForgeMod}
 * calls it, and only on the client. Forge has no event for drawing in the level, so the
 * drawing comes from {@code mixin.LevelRendererMixin} instead.
 */
final class CoreForgeClient {

    private CoreForgeClient() {
    }

    static void start() {
        // Forge's key event takes the key alone, so the Controls category is
        // registered the vanilla way. Both happen before the game's options exist,
        // which Forge requires.
        KeyMapping key = HologramClient.createMoveKey(
                KeyMapping.Category.register(HologramClient.KEY_CATEGORY));
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(key));
        TickEvent.ClientTickEvent.Post.BUS.addListener(
                event -> HologramClient.onEndTick(Minecraft.getInstance()));
        // The attack key's event changes between Minecraft releases, so AttackListener
        // sits in the version folder. A listener that returns true cancels the event: the block behind
        // a hologram is not hit, and the hotbar slot does not change.
        AttackListener.listen();
        InputEvent.MouseScrollingEvent.BUS.addListener(event -> {
            return HologramClient.onScroll(event.getDeltaY());
        });
    }
}
