package org.samo_lego.taterzens.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.samo_lego.taterzens.Taterzens;

import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.successText;

public class LockCommand {
    public static void registerNode(LiteralCommandNode<ServerCommandSource> npcNode) {
        LiteralCommandNode<ServerCommandSource> lockingNode = literal("lock")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.lock", config.perms.npcCommandPermissionLevel))
                .executes(context -> lock(context, true))
                .build();


        LiteralCommandNode<ServerCommandSource> unlockingNode = literal("unlock")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.unlock", config.perms.npcCommandPermissionLevel))
                .executes(context -> lock(context, false))
                .build();

        npcNode.addChild(lockingNode);
        npcNode.addChild(unlockingNode);
    }

    private static int lock(CommandContext<ServerCommandSource> context, boolean lock) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        return NpcCommand.selectedTaterzenExecutor(entity, taterzen -> {
            if (lock) {
                taterzen.setLocked(entity);
                source.sendFeedback(
                        successText("taterzens.command.lock.success", taterzen.getName().getString()),
                        false
                );
            } else {
                source.sendFeedback(
                        successText("taterzens.command.unlock.success", taterzen.getName().getString()),
                        false
                );
            }
        });
    }
}
