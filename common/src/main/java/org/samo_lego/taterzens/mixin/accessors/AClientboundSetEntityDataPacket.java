package org.samo_lego.taterzens.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public interface AClientboundSetEntityDataPacket {
    @Mutable
    @Accessor("id")
    int getEntityId();

    @Mutable
    @Accessor("trackedValues")
    void setPackedItems(List<DataTracker.SerializedEntry<?>> packedItems);
}
