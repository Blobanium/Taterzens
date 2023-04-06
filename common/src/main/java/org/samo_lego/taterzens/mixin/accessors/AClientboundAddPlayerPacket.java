package org.samo_lego.taterzens.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;

@Mixin(PlayerSpawnS2CPacket.class)
public interface AClientboundAddPlayerPacket {
    @Mutable
    @Accessor("id")
    void setId(int id);

    @Mutable
    @Accessor("id")
    int getId();

    @Mutable
    @Accessor("uuid")
    void setUuid(UUID uuid);

    @Mutable
    @Accessor("x")
    void setX(double x);

    @Mutable
    @Accessor("y")
    void setY(double y);

    @Mutable
    @Accessor("z")
    void setZ(double z);

    @Mutable
    @Accessor("pitch")
    void setYRot(byte yRot);

    @Mutable
    @Accessor("yaw")
    void setXRot(byte xRot);
}
