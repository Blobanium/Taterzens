package org.samo_lego.taterzens.mixin.network;

import org.jetbrains.annotations.Nullable;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.samo_lego.taterzens.mixin.accessors.AClientboundSetEntityDataPacket;
import org.samo_lego.taterzens.mixin.accessors.AEntity;
import org.samo_lego.taterzens.npc.TaterzenNPC;
import org.samo_lego.taterzens.util.NpcPlayerUpdate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import static org.samo_lego.taterzens.Taterzens.config;

/**
 * Used to "fake" the TaterzenNPC entity type.
 */
@Mixin(value = ServerPlayNetworkHandler.class, priority = 900)
public abstract class ServerGamePacketListenerImplMixin_PacketFaker {

    @Shadow
    public ServerPlayerEntity player;

    @Final
    @Shadow
    private ClientConnection connection;

    @Shadow
    public abstract void sendPacket(Packet<?> packet, @Nullable PacketCallbacks packetSendListener);

    @Shadow
    public abstract void sendPacket(Packet<?> packet);

    @Unique
    private boolean taterzens$skipCheck;
    @Unique
    private final Map<UUID, NpcPlayerUpdate> taterzens$tablistQueue = new LinkedHashMap<>();
    @Unique
    private int taterzens$queueTick;

    /**
     * Changes entity type if entity is an instance of {@link TaterzenNPC}.
     */
    @Inject(method = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V"))
    private void changeEntityType(Packet<?> packet, PacketCallbacks listener, CallbackInfo ci) {
        World world = player.getWorld();
        if (packet instanceof BundlePacket<?> bPacket && !this.taterzens$skipCheck) {
            for (Packet<?> subPacket : bPacket.getPackets()) {
                if (subPacket instanceof PlayerSpawnS2CPacket playerAddPacket) {
                    Entity entity = player.getWorld().getEntityById(playerAddPacket.getId());

                    if (entity instanceof TaterzenNPC npc) {
                        var uuid = npc.getGameProfile().getId();
                        this.taterzens$tablistQueue.remove(uuid);
                        this.taterzens$tablistQueue.put(uuid, new NpcPlayerUpdate(npc.getGameProfile(), npc.getTabListName(), taterzens$queueTick + config.taterzenTablistTimeout));
                    }
                }
            }
        } else if (packet instanceof EntityTrackerUpdateS2CPacket) {
            Entity entity = world.getEntityById(((AClientboundSetEntityDataPacket) packet).getEntityId());

            if (!(entity instanceof TaterzenNPC taterzen))
                return;
            PlayerEntity fakePlayer = taterzen.getFakePlayer();
            List<DataTracker.SerializedEntry<?>> trackedValues = fakePlayer.getDataTracker().getChangedEntries();

            if (taterzen.equals(((ITaterzenEditor) this.player).getNpc()) && trackedValues != null && config.glowSelectedNpc) {
                trackedValues.removeIf(value -> value.id() == 0);
                Byte flags = fakePlayer.getDataTracker().get(AEntity.getFLAGS());
                // Modify Taterzen to have fake glowing effect for the player
                flags = (byte) (flags | 1 << AEntity.getFLAG_GLOWING());

                DataTracker.SerializedEntry<Byte> glowingTag = DataTracker.SerializedEntry.of(AEntity.getFLAGS(), flags);
                trackedValues.add(glowingTag);
            }

            ((AClientboundSetEntityDataPacket) packet).setPackedItems(trackedValues);
        }
    }

    @Inject(method = "onPlayerMove", at = @At("RETURN"))
    private void removeTaterzenFromTablist(CallbackInfo ci) {
        if (taterzens$tablistQueue.isEmpty()) return;

        taterzens$queueTick++;

        List<UUID> toRemove = new ArrayList<>();
        for (var iterator = taterzens$tablistQueue.values().iterator(); iterator.hasNext(); ) {
            var current = iterator.next();
            if (current.removeAt() > taterzens$queueTick) break;

            iterator.remove();
            toRemove.add(current.profile().getId());
        }
        if (toRemove.isEmpty()) return;

        PlayerRemoveS2CPacket taterzensRemovePacket = new PlayerRemoveS2CPacket(toRemove);

        this.taterzens$skipCheck = true;
        this.sendPacket(taterzensRemovePacket);
        this.taterzens$skipCheck = false;
    }
}
