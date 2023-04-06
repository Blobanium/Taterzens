package org.samo_lego.taterzens.npc.commands;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.samo_lego.taterzens.npc.TaterzenNPC;

public class MinecraftCommand extends AbstractTaterzenCommand {
    private String command;

    public MinecraftCommand(String command) {
        super(CommandType.DEFAULT);
        this.command = command;
    }

    public MinecraftCommand() {
        super(CommandType.DEFAULT);
        this.command = "";
    }

    @Override
    public void execute(TaterzenNPC npc, PlayerEntity player) {
        npc.getServer().getCommandManager().executeWithPrefix(
                npc.getCommandSource(), command.replaceAll(CLICKER_PLACEHOLDER, player.getGameProfile().getName()));
    }

    @Override
    public String toString() {
        return this.command;
    }

    @Override
    public NbtCompound toTag(NbtCompound tag) {
        super.toTag(tag).putString("Command", this.command);
        return tag;
    }

    @Override
    public void fromTag(NbtCompound cmdTag) {
        this.command = cmdTag.getString("Command");
    }
}
