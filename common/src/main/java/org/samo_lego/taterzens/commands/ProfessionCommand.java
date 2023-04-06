package org.samo_lego.taterzens.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import org.samo_lego.taterzens.Taterzens;

import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;

public class ProfessionCommand {
    public static final LiteralCommandNode<ServerCommandSource> PROFESSION_COMMAND_NODE;

    /**
     * Registers "/profession" node. Can be used to manage scarpet professions,
     * or other professions if they hook into the node.
     * @param dispatcher command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.getRoot().addChild(PROFESSION_COMMAND_NODE);
    }

    static {
        PROFESSION_COMMAND_NODE = literal("profession")
                    .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.profession", config.perms.professionCommandPL)
                )
                .build();
    }
}
