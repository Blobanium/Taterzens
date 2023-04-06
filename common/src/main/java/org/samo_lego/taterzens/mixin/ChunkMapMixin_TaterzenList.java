package org.samo_lego.taterzens.mixin;

import org.samo_lego.taterzens.npc.TaterzenNPC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.samo_lego.taterzens.Taterzens.TATERZEN_NPCS;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ChunkMapMixin_TaterzenList {

    /**
     * Sets Taterzen to {@link org.samo_lego.taterzens.Taterzens#TATERZEN_NPCS NPC list}.
     * @param entity entity being loaded
     */
    @Inject(method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;loadEntity(Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
    private void onEntityAdded(Entity entity, CallbackInfo ci) {
        if (entity instanceof TaterzenNPC && !TATERZEN_NPCS.containsKey(entity.getUuid())) {
            TATERZEN_NPCS.put(entity.getUuid(), (TaterzenNPC) entity);
        }
    }

    /**
     * Unloads Taterzen from {@link org.samo_lego.taterzens.Taterzens#TATERZEN_NPCS NPC list}.
     * @param entity entity being unloaded
     */
    @Inject(method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;unloadEntity(Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
    private void onEntityRemoved(Entity entity, CallbackInfo ci) {
        if (entity instanceof TaterzenNPC) {
            TATERZEN_NPCS.remove(entity.getUuid());
        }
    }
}
