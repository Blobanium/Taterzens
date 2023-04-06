package org.samo_lego.taterzens.commands.edit.commands;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.commands.NpcCommand;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.successText;

public class CooldownCommand {

    public static void registerNode(LiteralCommandNode<ServerCommandSource> commandsNode) {
        LiteralCommandNode<ServerCommandSource> cooldown = literal("cooldown")
                .requires(cs -> Taterzens.getInstance().getPlatform().checkPermission(cs, "taterzens.edit.commands.cooldown", config.perms.npcCommandPermissionLevel))
                .then(literal("set")
                        .requires(cs -> Taterzens.getInstance().getPlatform().checkPermission(cs, "taterzens.edit.commands.cooldown.set", config.perms.npcCommandPermissionLevel))
                        .then(argument("cooldown", LongArgumentType.longArg(0))
                                .executes(CooldownCommand::setCooldown)
                        )
                )
                .then(literal("editMessage")
                        .requires(cs -> Taterzens.getInstance().getPlatform().checkPermission(cs, "taterzens.edit.commands.cooldown.edit_message", config.perms.npcCommandPermissionLevel))
                        .then(argument("new cooldown message", MessageArgumentType.message())
                                .executes(CooldownCommand::setMessage)
                        )
                )
                .build();

        commandsNode.addChild(cooldown);
    }

    private static int setMessage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = context.getSource().getEntityOrThrow();
        String msg = MessageArgumentType.getMessage(context, "new cooldown message").getString();
        return NpcCommand.selectedTaterzenExecutor(entity, taterzen -> {
            taterzen.setCooldownMessage(msg);
            entity.sendMessage(
                    successText("taterzens.command.commands.cooldown.edit_message", msg, taterzen.getName().getString()));
        });

    }

    private static int setCooldown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = context.getSource().getEntityOrThrow();
        long cooldown = LongArgumentType.getLong(context, "cooldown");
        return NpcCommand.selectedTaterzenExecutor(entity, taterzen -> {
            taterzen.setMinCommandInteractionTime(cooldown);
            entity.sendMessage(
                    successText("taterzens.command.commands.cooldown.set", String.valueOf(cooldown), taterzen.getName().getString()));
        });
    }
}
