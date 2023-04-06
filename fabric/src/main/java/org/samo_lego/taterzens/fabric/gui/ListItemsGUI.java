package org.samo_lego.taterzens.fabric.gui;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.samo_lego.taterzens.Taterzens;

import static org.samo_lego.taterzens.Taterzens.config;

public abstract class ListItemsGUI extends SimpleGui implements Inventory {
    protected static final NbtCompound customData = new NbtCompound();
    private static final int REGISTRY_ITEMS_SIZE = Taterzens.getInstance().getPlatform().getItemRegistrySize();
    private int currentPage = 0;

    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param player the player to server this gui to.
     * @param npcName player's taterzen.
     * @param titleTranslationKey title translation key for gui.
     */
    public ListItemsGUI(ServerPlayerEntity player, Text npcName, String titleTranslationKey) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);

        this.setTitle(Text.translatable(titleTranslationKey).append(": ").formatted(Formatting.YELLOW).append(npcName.copy()));

        // Info (which page)
        ItemStack info = new ItemStack(Items.PAPER);
        info.setNbt(customData.copy());
        info.setCustomName(getCurrentPageMarker());
        info.addEnchantment(null, 0);

        this.setSlot(3, info);

        // Previous page
        ItemStack back = new ItemStack(Items.MAGENTA_GLAZED_TERRACOTTA);
        back.setNbt(customData.copy());
        back.setCustomName(Text.translatable("spectatorMenu.previous_page"));
        back.addEnchantment(null, 0);

        GuiElement previousScreenButton = new GuiElement(back, (index, type1, action) -> {
            if (--this.currentPage < 0)
                this.currentPage = 0;
            info.setCustomName(getCurrentPageMarker());
        });
        this.setSlot(0, previousScreenButton);

        // Next page
        ItemStack next = new ItemStack(Items.LIGHT_BLUE_GLAZED_TERRACOTTA);
        next.setNbt(customData.copy());
        next.setCustomName(Text.translatable("spectatorMenu.next_page"));
        next.addEnchantment(null, 0);

        GuiElement nextScreenButton = new GuiElement(next, (_i, _clickType, _slotActionType) -> {
            if (++this.currentPage > this.getMaxPages())
                this.currentPage = this.getMaxPages();
            info.setCustomName(getCurrentPageMarker());
        });
        this.setSlot(1, nextScreenButton);


        // Close screen button
        ItemStack close = new ItemStack(Items.STRUCTURE_VOID);
        close.setNbt(customData.copy());
        close.setCustomName(Text.translatable("spectatorMenu.close"));
        close.addEnchantment(null, 0);

        GuiElement closeScreenButton = new GuiElement(close, (_i, _clickType, _slotActionType) -> {
            this.close();
            player.closeHandledScreen();
        });
        this.setSlot(8, closeScreenButton);
    }

    /**
     * Gets an item from registry by string hash.
     * @param name string to convert into item
     * @return item, converted from string hash. If air would be returned, it is switched top stone instead.
     */
    public static Item getFromName(String name) {
        int i = Math.abs(name.hashCode());
        Item item = Item.byRawId(i % REGISTRY_ITEMS_SIZE);
        if (item.equals(Items.AIR))
            item = Items.STONE;

        return item;
    }

    /**
     * Gets current page info (Page X of Y)
     *
     * @return translated page info text.
     */
    private MutableText getCurrentPageMarker() {
        return Text.translatable("book.pageIndicator", this.currentPage + 1, this.getMaxPages() + 1);
    }

    public int getCurrentPage() {
       return this.currentPage;
    }


    @Override
    public int size() {
        return 9 * 6;
    }

    protected int getSlot2MessageIndex(int slotIndex) {
        return this.getCurrentPage() * this.getSize() + slotIndex;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    public abstract int getMaxPages();


    static {
        customData.putInt("CustomModelData", config.guiItemModelData);
        customData.putInt("HideFlags", 127);
    }
}
