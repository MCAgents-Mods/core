package io.github.mcagentsmods.core.neoforge;

import io.github.mcagentsmods.core.server.AgentsCommand;
import io.github.mcagentsmods.core.shared.CoreRuntime;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * NeoForge's entry point. NeoForge tells the constructor which distribution is
 * running, so the side is never guessed.
 */
@Mod(CoreRuntime.MOD_ID)
public final class CoreNeoForgeMod {

    public CoreNeoForgeMod(Dist dist) {
        PhysicalSide side = dist.isClient() ? PhysicalSide.CLIENT : PhysicalSide.DEDICATED_SERVER;
        CoreRuntime.start(side, FMLPaths.GAMEDIR.get());
        NeoForge.EVENT_BUS.addListener(GameShuttingDownEvent.class, event -> CoreRuntime.stop());
        if (side == PhysicalSide.DEDICATED_SERVER) {
            // Dedicated servers only: in single player the player owns the keys and manages
            // them on the Settings hologram.
            NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                    event -> AgentsCommand.register(event.getDispatcher()));
        }
    }
}
