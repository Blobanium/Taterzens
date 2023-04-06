package org.samo_lego.taterzens.commands.edit;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.commands.NpcCommand;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.successText;

public class RespawnPointCommand {
    public static void registerNode(LiteralCommandNode<ServerCommandSource> editNode) {
        LiteralCommandNode<ServerCommandSource> respawnNode = literal("respawn")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.respawn", config.perms.npcCommandPermissionLevel))
                .then(literal("setCoordinates")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.respawn.coordinates", config.perms.npcCommandPermissionLevel))
                        .then(argument("coordinates", BlockPosArgumentType.blockPos())
                                .executes(RespawnPointCommand::setRespawnCoords)
                        )
                )
                .then(literal("toggle")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.respawn.toggle", config.perms.npcCommandPermissionLevel))
                        .then(argument("do respawn", BoolArgumentType.bool())
                                .executes(RespawnPointCommand::toggleRespawn)
                        )
                )
                .build();

        editNode.addChild(respawnNode);
    }

    private static int toggleRespawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource src = context.getSource();
        Vec3d respawnPos = BoolArgumentType.getBool(context, "do respawn") ? context.getSource().getPosition() : null;
        return NpcCommand.selectedTaterzenExecutor(src.getEntityOrThrow(), taterzen -> {
            taterzen.setRespawnPos(respawnPos);
            src.sendFeedback(successText("taterzens.command.respawn.toggle", String.valueOf(respawnPos == null)), false);
        });

    }

    private static int setRespawnCoords(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource src = context.getSource();
        BlockPos respawnPos = BlockPosArgumentType.getValidBlockPos(context, "coordinates");
        return NpcCommand.selectedTaterzenExecutor(src.getEntityOrThrow(), taterzen -> {
            taterzen.setRespawnPos(Vec3d.ofCenter(respawnPos));
            src.sendFeedback(successText("taterzens.command.respawn.coordinates", respawnPos.toShortString()), false);
        });
    }
}
