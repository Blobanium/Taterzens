package org.samo_lego.taterzens.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.taterzens.commands.NpcCommand;
import org.samo_lego.taterzens.fabric.gui.MessagesEditGUI;

import static org.samo_lego.taterzens.commands.edit.messages.MessagesCommand.messagesNode;

public class MessagesReorderCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(((LiteralCommandNode<ServerCommandSource>) messagesNode.getChild("swap")).createBuilder().executes(MessagesReorderCommand::reorderMessages));
    }


    private static int reorderMessages(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        return NpcCommand.selectedTaterzenExecutor(player, taterzen ->
                new MessagesEditGUI(player, taterzen).open()
        );
    }
}
