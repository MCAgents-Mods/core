package io.github.mcagentsmods.core.fabric;

import io.github.mcagentsmods.core.server.AgentsCommand;
import io.github.mcagentsmods.core.shared.CoreRuntime;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric's common entry point. Starts core on a dedicated server; on a client
 * it does nothing, because {@link CoreFabricClient} starts the client half and
 * this runs on both.
 */
public final class CoreFabricMod implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.getEnvironmentType() != EnvType.SERVER) {
            return;
        }
        CoreRuntime.start(PhysicalSide.DEDICATED_SERVER, loader.getGameDir());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CoreRuntime.stop());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) ->
                AgentsCommand.register(dispatcher));
    }
}
