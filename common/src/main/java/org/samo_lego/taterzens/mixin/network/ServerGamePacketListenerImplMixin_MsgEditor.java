package org.samo_lego.taterzens.mixin.network;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.samo_lego.taterzens.npc.TaterzenNPC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.successText;
import static org.samo_lego.taterzens.util.TextUtil.translate;

@Mixin(PlayerManager.class)
public class ServerGamePacketListenerImplMixin_MsgEditor {

    /**
     * Catches messages; if player is in
     * message edit mode, messages sent to chat
     * will be saved to taterzen instead.
     */
    @Inject(
            method = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void taterzens_chatBroadcast(SignedMessage playerChatMessage, Predicate<ServerPlayerEntity> predicate, ServerPlayerEntity player, MessageType.Parameters bound, CallbackInfo ci) {
        if (player == null) return;

        ITaterzenEditor editor = (ITaterzenEditor) player;
        TaterzenNPC taterzen = editor.getNpc();
        String msg = playerChatMessage.getContent().getString();

        if (taterzen != null && ((ITaterzenEditor) player).getEditorMode() == ITaterzenEditor.EditorMode.MESSAGES && !msg.startsWith("/")) {
            if (msg.startsWith("delay")) {
                String[] split = msg.split(" ");
                if (split.length > 1) {
                    try {
                        int delay = Integer.parseInt(split[1]);
                        taterzen.setMessageDelay(editor.getEditingMessageIndex(), delay);
                        player.sendMessage(successText("taterzens.command.message.delay", String.valueOf(delay)), false);
                    } catch (NumberFormatException ignored) {

                    }
                }
            } else {
                Text text;
                if((msg.startsWith("{") && msg.endsWith("}") || (msg.startsWith("[") && msg.endsWith("]")))) {
                    // NBT tellraw message structure, try parse it
                    try {
                        text = Text.Serializer.fromJson(new StringReader(msg));
                    } catch(JsonParseException ignored) {
                        player.sendMessage(translate("taterzens.error.invalid.text").formatted(Formatting.RED), false);
                        ci.cancel();
                        return;
                    }
                } else
                    text = Text.literal(msg);
                if((editor).getEditingMessageIndex() != -1) {
                    // Editing selected message
                    taterzen.editMessage(editor.getEditingMessageIndex(), text); // Editing message
                    player.sendMessage(successText("taterzens.command.message.changed", text.getString()), false);

                    // Exiting the editor
                    if(config.messages.exitEditorAfterMsgEdit) {
                        ((ITaterzenEditor) player).setEditorMode(ITaterzenEditor.EditorMode.NONE);
                        (editor).setEditingMessageIndex(-1);
                        player.sendMessage(translate("taterzens.command.equipment.exit").formatted(Formatting.LIGHT_PURPLE), false);
                    }
                } else {
                    taterzen.addMessage(text); // Adding message
                    player.sendMessage(successText("taterzens.command.message.editor.add", text.getString()), false);
                }

            }
            ci.cancel();
        }
    }
}