package io.github.mcagentsmods.core.fabric;

import io.github.mcagentsmods.core.client.agent.AgentsKeys;
import io.github.mcagentsmods.core.client.agent.AgentsRuntime;
import io.github.mcagentsmods.core.client.hologram.HologramClient;
import io.github.mcagentsmods.core.shared.CoreRuntime;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientHotbarScrollEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/**
 * Fabric's client entry point. Fabric loads it on a client only, so the client
 * lifecycle classes it names are never linked on a dedicated server.
 */
public final class CoreFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // First, before anything that can fail: Fabric refuses a key mapping once
        // the game's options exist.
        KeyMapping.Category category = KeyMapping.Category.register(HologramClient.KEY_CATEGORY);
        KeyMappingHelper.registerKeyMapping(HologramClient.createMoveKey(category));
        KeyMappingHelper.registerKeyMapping(AgentsKeys.create(category));

        CoreRuntime.start(PhysicalSide.CLIENT, FabricLoader.getInstance().getGameDir());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CoreRuntime.stop());
        AgentsRuntime.start(FabricLoader.getInstance().getGameDir());

        ClientTickEvents.END_CLIENT_TICK.register(HologramClient::onEndTick);
        ClientTickEvents.END_CLIENT_TICK.register(AgentsKeys::onEndTick);
        LevelRenderEvents.COLLECT_SUBMITS.register(context ->
                HologramClient.submit(context.levelState(), context.submitNodeCollector()));
        // Returning true cancels the attack, so the block behind a hologram is not hit.
        ClientPreAttackCallback.EVENT.register((client, player, clicks) -> HologramClient.onAttack());
        ClientHotbarScrollEvents.ALLOW.register((inventory, from, to, horizontal, vertical) ->
                !HologramClient.onScroll(vertical != 0 ? vertical : horizontal));
    }
}
