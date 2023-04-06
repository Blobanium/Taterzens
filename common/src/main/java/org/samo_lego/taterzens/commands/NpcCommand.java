package org.samo_lego.taterzens.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.NotNull;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.api.TaterzensAPI;
import org.samo_lego.taterzens.commands.edit.EditCommand;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.samo_lego.taterzens.mixin.accessors.ACommandSourceStack;
import org.samo_lego.taterzens.npc.TaterzenNPC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.argument.MessageArgumentType.message;
import static org.samo_lego.taterzens.Taterzens.TATERZEN_NPCS;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.*;

public class NpcCommand {
    public static LiteralCommandNode<ServerCommandSource> npcNode;

    private static final double MAX_DISTANCE = 8.02;
    private static final double SQRD_DIST = MAX_DISTANCE * MAX_DISTANCE;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        npcNode = dispatcher.register(literal("npc")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc", config.perms.npcCommandPermissionLevel))
                .then(literal("create")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.create", config.perms.npcCommandPermissionLevel))
                        .then(argument("name", message())
                                .suggests((context, builder) -> CommandSource.suggestMatching(getOnlinePlayers(context), builder))
                                .executes(NpcCommand::spawnTaterzen)
                        )
                        .executes(NpcCommand::spawnTaterzen)
                )
                .then(literal("select")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.select", config.perms.npcCommandPermissionLevel))
                        .then(literal("id")
                            .then(argument("id", IntegerArgumentType.integer(1))
                                    .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.select.id", config.perms.npcCommandPermissionLevel))
                                    .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableTaterzenIndices(), builder))
                                    .executes(NpcCommand::selectTaterzenById)
                            )
                        )
                        .then(literal("name")
                            .then(argument("name", StringArgumentType.string())
                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.select.name", config.perms.npcCommandPermissionLevel))
                                .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableTaterzenNames(), builder))
                                .executes(NpcCommand::selectTaterzenByName)
                            )
                        )
                        .then(literal("uuid")
                            .then(argument("uuid", StringArgumentType.string())
                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.select.uuid", config.perms.npcCommandPermissionLevel))
                                .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableTaterzenUUIDs(), builder))
                                .executes(NpcCommand::selectTaterzenByUUID)
                            )
                        )
                        .executes(NpcCommand::selectTaterzen)
                )
                .then(literal("deselect")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.select.deselect", config.perms.npcCommandPermissionLevel))
                        .executes(NpcCommand::deselectTaterzen)
                )
                .then(literal("list")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.list", config.perms.npcCommandPermissionLevel))
                        .executes(NpcCommand::listTaterzens)
                )
                .then(literal("remove")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.remove", config.perms.npcCommandPermissionLevel))
                        .executes(NpcCommand::removeTaterzen)
                )
        );

        EditCommand.registerNode(dispatcher, npcNode, commandBuildContext);
        PresetCommand.registerNode(npcNode);
        TeleportCommand.registerNode(npcNode);
        ActionCommand.registerNode(npcNode);
        LockCommand.registerNode(npcNode);
    }

    /**
     * Error text for no selected taterzen
     * @return formatted error text.
     */
    public static MutableText noSelectedTaterzenError() {
        return translate("taterzens.error.select")
                .formatted(Formatting.RED)
                .styled(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.command.list")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc list"))
                );
    }

    /**
     * Gets player's selected Taterzen and executes the consumer.
     * @param entity player to get Taterzen for or Taterzen itself
     * @param npcConsumer lambda that gets selected Taterzen as argument
     * @return 1 if player has npc selected and predicate test passed, otherwise 0
     */
    public static int selectedTaterzenExecutor(@NotNull Entity entity, Consumer<TaterzenNPC> npcConsumer) {
        TaterzenNPC taterzen = null;
        if(entity instanceof ITaterzenEditor player)
            taterzen = player.getNpc();
        else if(entity instanceof TaterzenNPC taterzenNPC)
            taterzen = taterzenNPC;
        if(taterzen != null) {
            npcConsumer.accept(taterzen);
            return 1;
        }
        entity.sendMessage(noSelectedTaterzenError());
        return 0;
    }

    private static int deselectTaterzen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ((ITaterzenEditor) player).selectNpc(null);
        source.sendFeedback(translate("taterzens.command.deselect").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int listTaterzens(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        boolean console = source.getEntity() == null;
        TaterzenNPC npc = null;

        if (!console) {
            npc = ((ITaterzenEditor) source.getPlayerOrThrow()).getNpc();
        }

        MutableText response = translate("taterzens.command.list").formatted(Formatting.AQUA);

        int i = 1;
        for (var taterzenNPC : TATERZEN_NPCS.values()) {
            String name = taterzenNPC.getName().getString();

            boolean sel = taterzenNPC == npc;

            response.append(
                            Text.literal("\n" + i + "-> " + name)
                                    .formatted(sel ? Formatting.BOLD : Formatting.RESET)
                                    .formatted(sel ? Formatting.GREEN : (i % 2 == 0 ? Formatting.YELLOW : Formatting.GOLD))
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc select uuid " + taterzenNPC.getUuid()))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate(sel ? "taterzens.tooltip.current_selection" : "taterzens.tooltip.new_selection", name)))))
                    .append(
                            Text.literal(" (" + (console ? taterzenNPC.getUuidAsString() : "uuid") + ")")
                                    .formatted(Formatting.GRAY)
                                    .styled(style -> style
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.tooltip.see_uuid")))
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, taterzenNPC.getUuidAsString()))
                                    )
                    );
            ++i;
        }

        source.sendFeedback(response, false);
        return 1;
    }

    private static List<String> getAvailableTaterzenIndices() {
        return IntStream.range(0, TATERZEN_NPCS.size())
                .mapToObj(i -> String.valueOf(i + 1))
                .toList();
    }

    private static int selectTaterzenById(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int id = IntegerArgumentType.getInteger(context, "id");
        ServerCommandSource source = context.getSource();
        if (id > TATERZEN_NPCS.size()) {
            source.sendError(errorText("taterzens.error.404.id", String.valueOf(id)));
        } else {
            TaterzenNPC taterzen = (TaterzenNPC) TATERZEN_NPCS.values().toArray()[id - 1];
            ServerPlayerEntity player = source.getPlayerOrThrow();
            TaterzenNPC npc = ((ITaterzenEditor) player).getNpc();

            if (npc != null) {
                ((ITaterzenEditor) player).selectNpc(null);
            }

            boolean selected = ((ITaterzenEditor) player).selectNpc(taterzen);
            if (selected) {
                source.sendFeedback(
                        successText("taterzens.command.select", taterzen.getName().getString()),
                        false
                );
            } else {
                source.sendError(
                        errorText("taterzens.command.error.locked", taterzen.getName().getString())
                );
            }
        }
        return 1;
    }

    private static List<String> getAvailableTaterzenNames() {
        // Adds quotation marks to the suggested name, such that
        // Names containing a whitespace character (ex. the
        // name is 'Foo Bar') can be completed and correctly
        // used without the user having to enclose the argument
        // name with quotation marks themselves.
        return TATERZEN_NPCS.values().stream().map(npc -> "\"" + npc.getName().getString() + "\"").toList();
    }

    private static int selectTaterzenByName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "name");

        // first count how many NPCs have the same name.
        // If there is more than one NPC with an identical name, the command will fail.
        // Otherwise, the NPC will be selected.
        // In case no NPC with that name is found, taterzen is null
        TaterzenNPC taterzen = null;
        int count = 0; // number of npcs with identical name
        for (TaterzenNPC npcIt : TATERZEN_NPCS.values()) {
            if (npcIt.getName().getString().equals(name)) {
                taterzen = npcIt;
                count++;

                if (count > 1) {
                    source.sendError(errorText("taterzens.error.multiple.name", name));
                    return 0;
                }
            }
        }

        if (count == 0) { // equivalent to taterzen == null
            source.sendError(errorText("taterzens.error.404.name", name));
            return 0;
        } else { // if count == 1
            ServerPlayerEntity player = source.getPlayerOrThrow();
            TaterzenNPC npc = ((ITaterzenEditor) player).getNpc();
            if (npc != null) {
                ((ITaterzenEditor) player).selectNpc(null);
            }
            boolean selected = ((ITaterzenEditor) player).selectNpc(taterzen);
            if (selected) {
                source.sendFeedback(
                        successText("taterzens.command.select", taterzen.getName().getString()),
                        false
                );
                return 1;
            } else {
                source.sendError(
                        errorText("taterzens.command.error.locked", taterzen.getName().getString())
                );
                return 0;
            }
        }
    }

    private static List<String> getAvailableTaterzenUUIDs() {
        return TATERZEN_NPCS.keySet().stream().map(UUID::toString).toList();
    }

    private static int selectTaterzenByUUID(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        UUID uuid;
        try {
            uuid = UUID.fromString(StringArgumentType.getString(context, "uuid"));
        } catch (IllegalArgumentException ex) {
            source.sendError(errorText("argument.uuid.invalid"));
            return 0;
        }

        TaterzenNPC taterzen = TATERZEN_NPCS.get(uuid);

        if (taterzen == null) {
            source.sendError(errorText("taterzens.error.404.uuid", uuid.toString()));
            return 0;
        } else {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            TaterzenNPC npc = ((ITaterzenEditor) player).getNpc();
            if (npc != null) {
                ((ITaterzenEditor) player).selectNpc(null);
            }
            boolean selected = ((ITaterzenEditor) player).selectNpc(taterzen);
            if (selected) {
                source.sendFeedback(
                        successText("taterzens.command.select", taterzen.getName().getString()),
                        false
                );
                return 1;
            } else {
                source.sendError(
                        errorText("taterzens.command.error.locked", taterzen.getName().getString())
                );
                return 0;
            }
        }
    }


    private static int removeTaterzen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        return selectedTaterzenExecutor(player, taterzen -> {
            taterzen.kill();
            source.sendFeedback(
                    successText("taterzens.command.remove", taterzen.getName().getString()),
                    false
            );
            ((ITaterzenEditor) player).selectNpc(null);
        });
    }

    private static int selectTaterzen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        // Made with help of https://github.com/Patbox/polydex/blob/2e1cd03470c6202bf0522c845caa35b20244f8b9/src/main/java/eu/pb4/polydex/impl/display/PolydexTargetImpl.java#L44
        Vec3d min = player.getCameraPosVec(0);

        Vec3d vec3d2 = player.getRotationVec(1.0F);
        Vec3d max = min.add(vec3d2.x * MAX_DISTANCE, vec3d2.y * MAX_DISTANCE, vec3d2.z * MAX_DISTANCE);
        Box box = player.getBoundingBox().stretch(vec3d2.multiply(MAX_DISTANCE)).expand(1.0D);

        final var hit = ProjectileUtil.raycast(player, min, max, box, entity -> entity.canHit() && entity instanceof TaterzenNPC, SQRD_DIST);

        if (hit != null) {
            TaterzenNPC taterzen = (TaterzenNPC) hit.getEntity();
            boolean selected = ((ITaterzenEditor) player).selectNpc(taterzen);
            if (selected) {
                source.sendFeedback(
                        successText("taterzens.command.select", taterzen.getName().getString()),
                        false
                );
            } else {
                source.sendError(
                        errorText("taterzens.command.error.locked", taterzen.getName().getString())
                );
            }
        } else {
            source.sendError(
                    translate("taterzens.error.404.detected")
                            .formatted(Formatting.RED)
            );
        }
        return 1;
    }


    private static Collection<String> getOnlinePlayers(CommandContext<ServerCommandSource> context) {
        Collection<String> names = new ArrayList<>();
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(
                player -> names.add(player.getGameProfile().getName())
        );

        return names;
    }

    private static int spawnTaterzen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        TaterzenNPC npc = ((ITaterzenEditor) player).getNpc();
        if(npc != null) {
            ((ITaterzenEditor) player).selectNpc(null);
        }

        String taterzenName;
        try {
            taterzenName = MessageArgumentType.getMessage(context, "name").getString();
        } catch(IllegalArgumentException ignored) {
            // no name was provided, defaulting to player's own name
            taterzenName = player.getGameProfile().getName();
        }

        TaterzenNPC taterzen = TaterzensAPI.createTaterzen(player, taterzenName);
        // Making sure permission level is as high as owner's, to prevent permission bypassing.
        taterzen.setPermissionLevel(((ACommandSourceStack) source).getPermissionLevel());

        // Lock if needed
        if (config.lockAfterCreation)
            taterzen.setLocked(player);

        player.getWorld().spawnEntity(taterzen);

        ((ITaterzenEditor) player).selectNpc(taterzen);
        player.sendMessage(successText("taterzens.command.create", taterzen.getName().getString()));

        return 1;
    }
}
