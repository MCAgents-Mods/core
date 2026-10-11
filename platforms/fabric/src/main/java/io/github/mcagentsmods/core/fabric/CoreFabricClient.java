package io.github.mcagentsmods.core.fabric;

import io.github.mcagentsmods.core.shared.CoreRuntime;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric's client entry point. Fabric loads it on a client only, so the client
 * lifecycle classes it names are never linked on a dedicated server.
 */
public final class CoreFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CoreRuntime.start(PhysicalSide.CLIENT, FabricLoader.getInstance().getGameDir());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CoreRuntime.stop());
    }
}
