package io.github.mcagentsmods.core.forge;

import io.github.mcagentsmods.core.shared.CoreRuntime;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge's entry point. Forge reports the running distribution before any mod
 * class loads, so the side is never guessed.
 */
@Mod(CoreRuntime.MOD_ID)
public final class CoreForgeMod {

    public CoreForgeMod() {
        PhysicalSide side = FMLEnvironment.dist.isClient() ? PhysicalSide.CLIENT : PhysicalSide.DEDICATED_SERVER;
        if (side == PhysicalSide.CLIENT) {
            // First, because Forge refuses a key once the game's options exist; and a
            // class of its own, so a dedicated server never links the client types it names.
            CoreForgeClient.start();
        }
        CoreRuntime.start(side, FMLPaths.GAMEDIR.get());
        GameShuttingDownEvent.BUS.addListener(event -> CoreRuntime.stop());
    }
}
