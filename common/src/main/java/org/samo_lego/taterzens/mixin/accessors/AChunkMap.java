package org.samo_lego.taterzens.mixin.accessors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface AChunkMap {
    @Accessor("entityTrackers")
    Int2ObjectMap<AEntityTrackerEntry> getEntityMap();
}
