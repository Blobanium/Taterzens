package org.samo_lego.taterzens.mixin.accessors;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.EntityTrackingListener;

@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage.EntityTracker")
public interface AEntityTrackerEntry {
    @Accessor("entity")
    Entity getPlayer();

    @Accessor("listeners")
    Set<EntityTrackingListener> getSeenBy();

    @Accessor("entry")
    EntityTrackerEntry getEntry();
}
