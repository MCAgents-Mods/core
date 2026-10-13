package io.github.mcagentsmods.core.forge.mixin;

import io.github.mcagentsmods.core.client.hologram.HologramClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the open hologram on Forge, which has no event for drawing in the level: at
 * the start of {@code submitFeatures}, where Fabric's and NeoForge's own events hand out the
 * same collector.
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {

    @Inject(method = "submitFeatures", at = @At("HEAD"))
    private void mcagents_core$submitHolograms(LevelRenderState state, SubmitNodeCollector collector,
                                               boolean translucent, CallbackInfo info) {
        HologramClient.submit(state, collector);
    }
}
