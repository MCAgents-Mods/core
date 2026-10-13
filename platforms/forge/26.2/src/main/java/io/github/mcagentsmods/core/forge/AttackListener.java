package io.github.mcagentsmods.core.forge;

import io.github.mcagentsmods.core.client.hologram.HologramClient;
import net.minecraftforge.client.event.InputEvent;

/**
 * Listens for the attack key on Forge for Minecraft 26.2. Every Minecraft version core builds
 * for has its own copy of this class, because Forge for 26.3 gives the attack key an event of
 * its own.
 */
final class AttackListener {

    private AttackListener() {
    }

    static void listen() {
        // 26.2 fires one event for every interaction key, so only the attack is taken.
        // A listener that returns true cancels it: the block behind a hologram is not hit.
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(event -> {
            if (event.isAttack() && HologramClient.onAttack()) {
                event.setSwingHand(false);
                return true;
            }
            return false;
        });
    }
}
