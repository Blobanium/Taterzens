package org.samo_lego.taterzens.mixin.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.npc.commands.BungeeCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket.BRAND;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.npc.commands.BungeeCommand.AVAILABLE_SERVERS;
import static org.samo_lego.taterzens.npc.commands.BungeeCommand.BUNGEE_CHANNEL;

/**
 * Handles bungee packets.
 */
@Mixin(value = ServerPlayNetworkHandler.class)
public class ServerGamePacketListenerImplMixin_BungeeListener {
    @Unique
    private static final String GET_SERVERS = "GetServers";
    @Shadow
    public ServerPlayerEntity player;

    @Unique
    private static final String taterzens$permission = "taterzens.npc.edit.commands.addBungee";

    @Inject(method = "onCustomPayload", at = @At("TAIL"))
    private void onCustomPayload(CustomPayloadC2SPacket packet, CallbackInfo ci) {
        Identifier packetId = packet.getChannel();
        ServerCommandSource commandSourceStack = player.getCommandSource();
        boolean hasPermission = Taterzens.getInstance().getPlatform().checkPermission(commandSourceStack, taterzens$permission, config.perms.npcCommandPermissionLevel);

        if (AVAILABLE_SERVERS.isEmpty() && config.bungee.enableCommands && hasPermission) {
            if (packetId.equals(BUNGEE_CHANNEL)) {
                // Reading data
                byte[] bytes = new byte[packet.getData().readableBytes()];
                packet.getData().readBytes(bytes);

                // Parsing the response
                if (bytes.length != 0) {
                    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
                    String subchannel = in.readUTF();

                    if (subchannel.equals(GET_SERVERS)) {
                        // Adding available servers to suggestions
                        String[] servers = in.readUTF().split(", ");
                        Collections.addAll(AVAILABLE_SERVERS, servers);
                    }
                }
            } else if (packetId.equals(BRAND)) {
                // Fetch available servers from proxy
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(GET_SERVERS);
                BungeeCommand.sendProxyPacket((ServerPlayNetworkHandler) (Object) this, out.toByteArray());
            }
        }
    }
}
