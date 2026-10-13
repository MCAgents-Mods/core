package io.github.mcagentsmods.core.neoforge;

import io.github.mcagentsmods.core.client.hologram.HologramClient;
import io.github.mcagentsmods.core.shared.CoreRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge's client half: the hologram's key, tick, drawing, click and wheel. Declared for the
 * client distribution only, so a dedicated server never constructs it or links what it names.
 */
@Mod(value = CoreRuntime.MOD_ID, dist = Dist.CLIENT)
public final class CoreNeoForgeClient {

    public CoreNeoForgeClient(IEventBus modBus) {
        // NeoForge offers key registration only on this start-up event, before the
        // game's options exist.
        modBus.addListener(RegisterKeyMappingsEvent.class, CoreNeoForgeClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class,
                event -> HologramClient.onEndTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class,
                event -> HologramClient.submit(event.getLevelRenderState(),
                        event.getSubmitNodeCollector()));
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class,
                CoreNeoForgeClient::onInteraction);
        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, event -> {
            if (HologramClient.onScroll(event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        });
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        // The event's own category registration, not KeyMapping.Category.register,
        // which NeoForge deprecates for mods.
        KeyMapping.Category category = new KeyMapping.Category(HologramClient.KEY_CATEGORY);
        event.registerCategory(category);
        event.register(HologramClient.createMoveKey(category));
    }

    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        // Cancelling keeps the block behind a hologram from being hit or mined.
        if (event.isAttack() && HologramClient.onAttack()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
