package org.samo_lego.taterzens.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

@Mixin(PlayerListS2CPacket.class)
public interface AClientboundPlayerInfoPacket {
    @Mutable
    @Accessor("entries")
    void setEntries(List<PlayerListS2CPacket.Entry> entries);
}
