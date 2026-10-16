package io.github.mcagentsmods.core.forge;

import io.github.mcagentsmods.core.client.hologram.HologramClient;
import net.minecraftforge.client.event.InputEvent;

/**
 * Listens for the attack key on Forge for Minecraft 26.3. It sits in the version folder
 * because Forge's interaction key events change between Minecraft releases: before 26.3,
 * one event fired for every interaction key.
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
