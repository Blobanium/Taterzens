package org.samo_lego.taterzens.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.taterzens.Taterzens;

import static org.samo_lego.taterzens.commands.NpcCommand.npcNode;

public class NpcGUICommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(npcNode.createBuilder().executes(NpcGUICommand::openGUI));
    }

    private static int openGUI(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Taterzens.getInstance().getPlatform().openEditorGui(player);
        return 1;
    }
}
