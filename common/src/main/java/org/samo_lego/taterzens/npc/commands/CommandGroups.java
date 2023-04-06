package org.samo_lego.taterzens.npc.commands;

import com.google.common.collect.ImmutableList;
import org.samo_lego.taterzens.npc.TaterzenNPC;

import java.util.ArrayList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

public class CommandGroups extends ArrayList<ArrayList<AbstractTaterzenCommand>> {
    private int groupIndex;
    private final TaterzenNPC npc;

    public CommandGroups(TaterzenNPC npc) {
        this.npc = npc;
        this.groupIndex = 0;
    }

    public boolean addCommand(AbstractTaterzenCommand command) {
        if (this.groupIndex >= this.size()) {
            this.add(new ArrayList<>());
        }
        return this.get(this.groupIndex).add(command);
    }

    @Override
    public void clear() {
        super.clear();
        this.groupIndex = 0;
    }

    public void execute(ServerPlayerEntity player) {
        if (this.isEmpty()) {
            return;
        }

        if (this.groupIndex >= this.size()) {
            this.groupIndex = 0;
        }

        var commands = ImmutableList.copyOf(this.get(this.groupIndex));
        for (var cmd : commands) {
            cmd.execute(this.npc, player);
        }
        ++this.groupIndex;
    }

    public void toTag(NbtCompound tag) {
        tag.putInt("GroupIndex", this.groupIndex);
        var commands = new NbtList();

        for (int i = 0; i < this.size(); ++i) {
            var cmds = this.get(i);
            NbtList cmdList = new NbtList();
            cmds.forEach(cmd -> cmdList.add(cmd.toTag(new NbtCompound())));
            commands.add(cmdList);
        }
        tag.put("Contents", commands);
    }

    public void fromTag(NbtCompound tag) {
        this.groupIndex = tag.getInt("GroupIndex");

        NbtList cmdsArray = (NbtList) tag.get("Contents");
        if (cmdsArray != null) {
            for (var cmds : cmdsArray) {
                var cmdList = new ArrayList<AbstractTaterzenCommand>();
                for (var cmd : (NbtList) cmds) {
                    var cmdTag = (NbtCompound) cmd;

                    AbstractTaterzenCommand toAdd;
                    if (AbstractTaterzenCommand.CommandType.valueOf(cmdTag.getString("Type")) == AbstractTaterzenCommand.CommandType.BUNGEE) {
                        toAdd = new BungeeCommand();
                    } else {
                        toAdd = new MinecraftCommand();
                    }
                    toAdd.fromTag(cmdTag);
                    cmdList.add(toAdd);
                }
                this.add(cmdList);
            }
        }
    }

    @Deprecated
    public void fromOldTag(NbtList minecraftCommands, NbtList bungeeCommands) {
        this.groupIndex = 0;

        // Commands
        var cmds = new ArrayList<AbstractTaterzenCommand>();
        if (minecraftCommands != null) {
            minecraftCommands.forEach(cmdTag -> cmds.add(new MinecraftCommand(cmdTag.asString())));
        }

        // Bungee commands
        if (bungeeCommands != null) {
            bungeeCommands.forEach(cmdTag -> {
                NbtList cmdList = (NbtList) cmdTag;
                String command = cmdList.get(0).asString();
                String player = cmdList.get(1).asString();
                String argument = cmdList.get(2).asString();

                cmds.add(new BungeeCommand(BungeeCommand.BungeeMessage.valueOf(command), player, argument));
            });
        }
        this.add(cmds);
    }

    public int createGroup() {
        this.add(new ArrayList<>());
        this.groupIndex = this.size() - 1;
        return this.groupIndex;
    }
}
