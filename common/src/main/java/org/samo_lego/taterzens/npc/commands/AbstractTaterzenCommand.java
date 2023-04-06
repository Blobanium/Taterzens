package org.samo_lego.taterzens.npc.commands;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.samo_lego.taterzens.npc.TaterzenNPC;

public abstract class AbstractTaterzenCommand {
    public static final String CLICKER_PLACEHOLDER = "--clicker--";
    private final CommandType type;

    public AbstractTaterzenCommand(CommandType type) {
        this.type = type;
    }

    public abstract void execute(TaterzenNPC npc, PlayerEntity player);

    public CommandType getType() {
        return this.type;
    }

    @Override
    public abstract String toString();

    public NbtCompound toTag(NbtCompound tag) {
        tag.putString("Type", this.type.toString());
        return tag;
    }

    public abstract void fromTag(NbtCompound cmdTag);

    public enum CommandType {
        BUNGEE,
        DEFAULT,
    }
}
