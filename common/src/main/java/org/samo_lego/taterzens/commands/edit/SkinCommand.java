package org.samo_lego.taterzens.commands.edit;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.commands.NpcCommand;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.argument.MessageArgumentType.message;
import static org.samo_lego.taterzens.Taterzens.GSON;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.compatibility.ModDiscovery.FABRICTAILOR_LOADED;
import static org.samo_lego.taterzens.mixin.accessors.APlayer.getPLAYER_MODE_CUSTOMISATION;
import static org.samo_lego.taterzens.util.TextUtil.*;
import static org.samo_lego.taterzens.util.WebUtil.urlRequest;

public class SkinCommand {

    private static final String MINESKIN_API_URL = "https://api.mineskin.org/get/uuid/";
    private static final String MOJANG_NAME2UUID = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_UUID2SKIN = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    public static void registerNode(LiteralCommandNode<ServerCommandSource> editNode) {
        LiteralCommandNode<ServerCommandSource> skinNode = literal("skin")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.skin", config.perms.npcCommandPermissionLevel))
                .then(argument("mineskin|player", message())
                        .executes(SkinCommand::setCustomSkin)
                )
                .executes(SkinCommand::copySkinLayers)
                .build();

        editNode.addChild(skinNode);
    }

    private static int setCustomSkin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String id = MessageArgumentType.getMessage(context, "mineskin|player").getString();
        Entity entity = source.getEntityOrThrow();

        return NpcCommand.selectedTaterzenExecutor(entity, taterzen -> {
            // Shameless self-promotion
            if(config.fabricTailorAdvert) {
                if(FABRICTAILOR_LOADED) {
                    source.sendFeedback(translate("advert.fabrictailor.skin_command")
                                    .formatted(Formatting.GOLD)
                                    .styled(style ->
                                            style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/skin set"))
                                    ),
                            false
                    );
                } else {
                    source.sendFeedback(translate("advert.fabrictailor")
                                    .formatted(Formatting.ITALIC)
                                    .formatted(Formatting.GOLD)
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/FabricTailor"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("advert.tooltip.install", "FabricTailor")))
                                    ),
                            false
                    );
                }

            }
            THREADPOOL.submit(() -> {
                URL url = null;
                if(id.contains(":") ) {
                    // Mineskin
                    String param = id.substring(id.lastIndexOf('/') + 1);  // + 1 so as to not include "/"
                    String mineskinUrl = MINESKIN_API_URL + param;
                    try {
                        url = new URL(mineskinUrl);
                    } catch(MalformedURLException e) {
                        source.sendError(errorText("taterzens.error.invalid.url", mineskinUrl));
                    }
                } else {
                    // Get skin by player's name
                    try {
                        String uuidReply = urlRequest(new URL(MOJANG_NAME2UUID + id));
                        if(uuidReply != null) {
                            JsonObject replyJson = GSON.fromJson(uuidReply, JsonObject.class);
                            String uuid = replyJson.get("id").getAsString();

                            url = new URL(String.format(MOJANG_UUID2SKIN, uuid));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(url != null) {
                    try {
                        String reply = urlRequest(url);
                        if(reply != null && !reply.contains("error") && !reply.isEmpty()) {

                            String value = reply.split("\"value\":\"")[1].split("\"")[0];
                            String signature = reply.split("\"signature\":\"")[1].split("\"")[0];

                            // Setting the skin
                            if(!value.isEmpty() && !signature.isEmpty()) {
                                NbtCompound skinTag = new NbtCompound();
                                skinTag.putString("value", value);
                                skinTag.putString("signature", signature);

                                taterzen.setSkinFromTag(skinTag);
                                taterzen.sendProfileUpdates();


                                context.getSource().sendFeedback(
                                        successText("taterzens.command.skin.fetched", id),
                                        false
                                );
                            }
                        } else {
                            context.getSource().sendError(errorText("taterzens.command.skin.error", id));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }


    private static int copySkinLayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        return NpcCommand.selectedTaterzenExecutor(player, taterzen -> {
            Byte skinLayers = player.getDataTracker().get(getPLAYER_MODE_CUSTOMISATION());
            taterzen.setSkinLayers(skinLayers);

            taterzen.sendProfileUpdates();
            source.sendFeedback(
                    successText("taterzens.command.skin.mirrored", taterzen.getName().getString()),
                    false
            );
        });
    }


}
