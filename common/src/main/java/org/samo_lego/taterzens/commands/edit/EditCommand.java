package org.samo_lego.taterzens.commands.edit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import org.samo_lego.taterzens.commands.edit.commands.CommandsCommand;
import org.samo_lego.taterzens.commands.edit.messages.MessagesCommand;

import static net.minecraft.server.command.CommandManager.literal;

public class EditCommand {

    public static void registerNode(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> npcNode, CommandRegistryAccess commandBuildContext) {
        LiteralCommandNode<ServerCommandSource> editNode = literal("edit")
                .build();

        npcNode.addChild(editNode);

        // Other sub nodes from "edit"
        BehaviourCommand.registerNode(editNode);
        CommandsCommand.registerNode(dispatcher, editNode);
        EquipmentCommand.registerNode(editNode);
        MessagesCommand.registerNode(editNode);
        MountCommand.registerNode(editNode);
        MovementCommand.registerNode(editNode);
        NameCommand.registerNode(editNode);
        PathCommand.registerNode(editNode);
        PoseCommand.registerNode(editNode);
        ProfessionsCommand.registerNode(editNode);
        RespawnPointCommand.registerNode(editNode);
        SkinCommand.registerNode(editNode);
        SoundCommand.registerNode(editNode);
        TagsCommand.registerNode(editNode);
        TypeCommand.registerNode(editNode, commandBuildContext);
    }
}
