package org.samo_lego.taterzens.mixin.accessors;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface APlayer {

    //Change to MAIN_ARM if somehow PLAYER_MODEL_PARTS doesn't work
    @Accessor("PLAYER_MODEL_PARTS")
    static TrackedData<Byte> getPLAYER_MODE_CUSTOMISATION() {
        throw new AssertionError();
    }
}
