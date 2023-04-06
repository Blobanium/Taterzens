package org.samo_lego.taterzens.commands.edit;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.commands.NpcCommand;
import org.samo_lego.taterzens.compatibility.DisguiseLibCompatibility;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.suggestion.SuggestionProviders.SUMMONABLE_ENTITIES;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.compatibility.ModDiscovery.DISGUISELIB_LOADED;
import static org.samo_lego.taterzens.util.TextUtil.successText;
import static org.samo_lego.taterzens.util.TextUtil.translate;

public class TypeCommand {
    public static void registerNode(LiteralCommandNode<ServerCommandSource> editNode, CommandRegistryAccess commandBuildContext) {
        LiteralCommandNode<ServerCommandSource> typeNode = literal("type")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                .then(argument("entity type", RegistryEntryArgumentType.registryEntry(commandBuildContext, RegistryKeys.ENTITY_TYPE))
                        .suggests(SUMMONABLE_ENTITIES)
                        .executes(TypeCommand::changeType)
                        .then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                .executes(TypeCommand::changeType)
                        )
                )
                .then(literal("minecraft:player")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                        .executes(TypeCommand::resetType)
                )
                .then(literal("player")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                        .executes(TypeCommand::resetType)
                )
                .then(literal("reset")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                        .executes(TypeCommand::resetType)
                )
                .build();

        editNode.addChild(typeNode);
    }

    private static int changeType(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if(!DISGUISELIB_LOADED) {
            source.sendError(translate("advert.disguiselib.required")
                    .formatted(Formatting.RED)
                    .styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("advert.tooltip.install", "DisguiseLib")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/disguiselib"))
                    )
            );
            return -1;
        }

        Identifier disguise = RegistryEntryArgumentType.getSummonableEntityType(context, "entity type").registryKey().getValue();
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            NbtCompound nbt;
            try {
                nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt").copy();
            } catch(IllegalArgumentException ignored) {
                nbt = new NbtCompound();
            }
            nbt.putString("id", disguise.toString());

            EntityType.loadEntityWithPassengers(nbt, source.getWorld(), (entityx) -> {
                DisguiseLibCompatibility.disguiseAs(taterzen, entityx);
                source.sendFeedback(
                        translate(
                                "taterzens.command.entity_type.set",
                                Text.translatable(entityx.getType().getTranslationKey()).formatted(Formatting.YELLOW)
                        ).formatted(Formatting.GREEN),
                        false
                );
                return entityx;
            });
        });
    }

    private static int resetType(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if(!DISGUISELIB_LOADED) {
            source.sendError(translate("advert.disguiselib.required")
                    .formatted(Formatting.RED)
                    .styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("advert.tooltip.install", "DisguiseLib")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/disguiselib"))
                    )
            );
            return -1;
        }
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            DisguiseLibCompatibility.clearDisguise(taterzen);
            source.sendFeedback(
                    successText("taterzens.command.entity_type.reset", taterzen.getName().getString()),
                    false
            );
        });
    }

}
