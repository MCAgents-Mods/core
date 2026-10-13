package io.github.mcagentsmods.core.forge;

import io.github.mcagentsmods.core.client.hologram.HologramClient;
import net.minecraftforge.client.event.InputEvent;

/**
 * Listens for the attack key on Forge for Minecraft 26.3. Every Minecraft version core builds
 * for has its own copy of this class, because Forge for 26.2 fires one event for every
 * interaction key.
 */
final class AttackListener {

    private AttackListener() {
    }

    static void listen() {
        // A listener that returns true cancels the attack: the block behind a hologram is
        // not hit.
        InputEvent.InteractionKeyMappingTriggered.Attack.BUS.addListener(event -> {
            if (HologramClient.onAttack()) {
                event.setSwingHand(false);
                return true;
            }
            return false;
        });
    }
}
