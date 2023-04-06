package org.samo_lego.taterzens.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.npc.NPCData;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.errorText;
import static org.samo_lego.taterzens.util.TextUtil.successText;

public class ActionCommand {
    public static void registerNode(LiteralCommandNode<ServerCommandSource> npcNode) {
        LiteralCommandNode<ServerCommandSource> actionNode = literal("action")
                .then(literal("goto")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.action.goto", config.perms.npcCommandPermissionLevel))
                        .then(argument("block pos", BlockPosArgumentType.blockPos())
                                .executes(ActionCommand::gotoBlock)
                        )
                )
                .then(literal("interact")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.action.interact", config.perms.npcCommandPermissionLevel))
                        .then(argument("block pos", BlockPosArgumentType.blockPos())
                                .executes(ActionCommand::interactWithBlock)
                        )
                )
                .build();

        npcNode.addChild(actionNode);
    }

    private static int interactWithBlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPosArgumentType.getValidBlockPos(context, "block pos");
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            if(taterzen.interact(pos)) {
                source.sendFeedback(
                        successText("taterzens.command.action.interact.success", taterzen.getName().getString(), pos.toShortString()),
                        false
                );
            } else {

                source.sendFeedback(
                        errorText("taterzens.command.action.interact.fail", pos.toShortString()),
                        false
                );
            }
        });
    }

    private static int gotoBlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPosArgumentType.getValidBlockPos(context, "block pos");
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            taterzen.setMovement(NPCData.Movement.TICK);

            taterzen.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1);

            source.sendFeedback(
                    successText("taterzens.command.action.goto.success", taterzen.getName().getString(), pos.toShortString()),
                    false
            );
        });
    }
}
