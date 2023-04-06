package org.samo_lego.taterzens.fabric.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class RedirectedSlot extends Slot {
    public RedirectedSlot(Inventory container, int index) {
        super(container, index, 0, 0);
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        ItemStack carried = player.currentScreenHandler.getCursorStack();
        if (!carried.isEmpty()) {
            this.setStackNoCallbacks(carried);
            carried.setCount(0);
            return false;
        }
        return true;
    }
}
