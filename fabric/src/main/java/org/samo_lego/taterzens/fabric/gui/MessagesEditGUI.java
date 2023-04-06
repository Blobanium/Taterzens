package org.samo_lego.taterzens.fabric.gui;

import com.mojang.datafixers.util.Pair;
import org.samo_lego.taterzens.npc.TaterzenNPC;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static org.samo_lego.taterzens.Taterzens.config;

public class MessagesEditGUI extends ListItemsGUI {
    private final List<Pair<Text, Integer>> messages;

    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param player              the player to server this gui to.
     * @param taterzen             player's taterzen.
     */
    public MessagesEditGUI(ServerPlayerEntity player, TaterzenNPC taterzen) {
        super(player, taterzen.getName(), "chat_screen.title");

        this.messages = taterzen.getMessages();

        int i = 9;
        do {
            // - 9 as first row is occupied but we want to have index 0 at first element
            this.setSlotRedirect(i, new RedirectedSlot(this, i - 9));
        } while (++i < this.getSize());
    }

    private ItemStack getItem(Pair<Text, Integer> pair) {
        Text message = pair.getFirst();
        ItemStack itemStack = new ItemStack(getFromName(message.getString()));
        itemStack.setNbt(customData.copy());
        itemStack.setCustomName(message);

        return itemStack;
    }


    @Override
    public ItemStack getStack(int index) {
        ItemStack itemStack;
        index = getSlot2MessageIndex(index);

        if(index < this.messages.size()) {
            itemStack = getItem(this.messages.get(index));
        } else {
            itemStack = ItemStack.EMPTY;
        }
        return itemStack;
    }

    @Override
    public ItemStack removeStack(int index, int count) {
        return this.removeStack(index);
    }

    @Override
    public ItemStack removeStack(int index) {
        ItemStack itemStack = this.getStack(index);
        index = getSlot2MessageIndex(index);
        if(index < this.messages.size()) {
            Pair<Text, Integer> removed = this.messages.remove(index);
            itemStack.setCustomName(this.getItem(removed).getName());
        }

        return itemStack;
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        if (!stack.isEmpty()) {
            index = getSlot2MessageIndex(index);
            if (index > messages.size())
                index = messages.size();
            this.messages.add(index, new Pair<>(stack.getName(), config.messages.messageDelay));
            stack.setCount(0);
        }
    }

    @Override
    public boolean isEmpty() {
        return this.messages.isEmpty();
    }

    @Override
    public void clear() {
        this.messages.clear();
    }

    @Override
    public int getMaxPages() {
        if (this.messages == null)
            return 0;
        return this.messages.size() / this.getSize();
    }
}
