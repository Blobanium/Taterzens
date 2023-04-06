package org.samo_lego.taterzens.fabric.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static org.samo_lego.taterzens.Taterzens.config;

public class EditorGUI {

    private static final ItemStack YES_BUTTON = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
    private static final ItemStack NO_BUTTON = new ItemStack(Items.RED_STAINED_GLASS_PANE);
    private static final HashMap<String, ItemStack> itemCommandMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static SimpleGui createCommandGui(ServerPlayerEntity player, @Nullable SimpleGui previousScreen, CommandNode<ServerCommandSource> parentNode, List<String> currentCommandPath, boolean givenInput) {
        // If node is not an argument, we skip to first child node that is an argument or has more than 1 child node
        while (parentNode.getChildren().size() == 1 && !(parentNode instanceof ArgumentCommandNode<?, ?>)) {
            CommandNode<ServerCommandSource> childNode = (CommandNode<ServerCommandSource>) parentNode.getChildren().toArray()[0];
            if (childNode instanceof ArgumentCommandNode) {
                givenInput = false;
            } else if (childNode.getChildren().size() > 0) {
                currentCommandPath.add(parentNode.getName());
            } else {
                break;
            }
            parentNode = childNode;

        }

        var childNodes = parentNode.getChildren();
        boolean argumentNode = parentNode instanceof ArgumentCommandNode<?, ?>;

        SimpleGui constructedGui;
        if(argumentNode && !givenInput) {
            constructedGui = new AnvilInputGui(player, true);

            final CommandNode<ServerCommandSource> finalParentNode = parentNode;
            GuiElement confirmButton = new GuiElement(YES_BUTTON, (index, clickType, slotActionType) -> {
                String arg = ((AnvilInputGui) constructedGui).getInput();
                // We "set" the argument to overwrite parent node (arg name)
                currentCommandPath.add(arg);

                CommandNode<ServerCommandSource> newNode = finalParentNode;
                if (childNodes.size() == 1)
                    newNode = (CommandNode<ServerCommandSource>) childNodes.toArray()[0];

                proccessClick(clickType, newNode, constructedGui, currentCommandPath, player, childNodes.size() != 1);
            });

            // Pre-written  text
            MutableText argTitle = Text.literal(parentNode.getName()).formatted(Formatting.YELLOW);
            ItemStack nameStack = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);  // invisible (kinda)
            nameStack.setCustomName(argTitle);

            // Buttons
            constructedGui.setSlot(2, confirmButton);
            constructedGui.setSlot(0, nameStack);

            // Default input value
            String[] examples = parentNode.getExamples().toArray(new String[0]);
            if (examples.length > 0)
                nameStack.setCustomName(Text.literal(examples[0]));

            for (int i = 1; i < examples.length; ++i) {
                ItemStack exampleStack = new ItemStack(Items.PAPER);
                String example = examples[i];
                exampleStack.setCustomName(Text.literal(example));

                // 2 being the last slot index in anvil inventory
                constructedGui.setSlot(i * 2 + 1, new GuiElement(exampleStack, (index, type, action) -> {
                    String input = ((AnvilInputGui) constructedGui).getInput();
                    ((AnvilInputGui) constructedGui).setDefaultInputValue(exampleStack.getName().getString());
                    exampleStack.setCustomName(Text.literal(input));
                }));
            }
        } else {
            // Creates the biggest possible container
            constructedGui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, true);

            // Close screen button
            ItemStack close = new ItemStack(Items.STRUCTURE_VOID);
            close.setCustomName(Text.translatable("spectatorMenu.close"));
            close.addEnchantment(null, 0);

            GuiElement closeScreenButton = new GuiElement(close, (i, clickType, slotActionType) -> player.closeHandledScreen());
            constructedGui.setSlot(8, closeScreenButton);

            // Integer to track item positions
            AtomicInteger i = new AtomicInteger(10);

            // Looping through command node, 8 * 9 being the available inventory space
            int addedSpace = (childNodes.size() < 8 * 9 / 3) ? (3) : (8 * 9 / childNodes.size());
            for (CommandNode<ServerCommandSource> node : childNodes) {
                // Tracking current command "path"
                // after each menu is opened, we add a node to queue
                ArrayList<String> parents = new ArrayList<>(currentCommandPath);
                String nodeName = node.getName();
                if(!(node instanceof ArgumentCommandNode<?, ?>))
                    parents.add(nodeName);

                // Set stack "icon"
                ItemStack stack = itemCommandMap.getOrDefault(nodeName, new ItemStack(ListItemsGUI.getFromName(nodeName))).copy();
                stack.setCustomName(Text.literal(nodeName));

                // Recursively adding the command nodes
                constructedGui.setSlot(i.getAndAdd(addedSpace), new GuiElement(stack, (index, clickType, slotActionType) -> {
                    // Different action happens on right or left click
                    proccessClick(clickType, node, constructedGui, parents, player, false);
                }));
            }
        }

        // Back button
        ItemStack back = new ItemStack(Items.MAGENTA_GLAZED_TERRACOTTA);
        back.setCustomName(Text.translatable("gui.back"));
        back.addEnchantment(null, 0);

        GuiElement backScreenButton = new GuiElement(back, (i, clickType, slotActionType) -> {
            if (previousScreen == null) {
                player.closeHandledScreen();
            } else {
                constructedGui.close();
                previousScreen.open();
            }
        });
        constructedGui.setSlot(argumentNode && !givenInput ? 1 : 0, backScreenButton);

        // GUI Title - each node adds to it
        var title = new StringBuilder();
        currentCommandPath.forEach(s -> title.append(s).append(" "));
        var textTitle = Text.literal(title.toString());

        constructedGui.setTitle(textTitle.formatted(Formatting.YELLOW));
        constructedGui.setAutoUpdate(true);

        return constructedGui;
    }

    private static void proccessClick(ClickType clickType, CommandNode<ServerCommandSource> node, SimpleGui gui, List<String> currentCommandPath, ServerPlayerEntity player, boolean givenInput) {
        StringBuilder builder = new StringBuilder();
        // Builds the command from parents
        currentCommandPath.forEach(s -> builder.append(s).append(" "));
        // Delete last space
        builder.deleteCharAt(builder.length() - 1);

        if (config.prefersExecution.contains(builder.toString())) {
            // Swaps the click type
           clickType = clickType == ClickType.MOUSE_LEFT ? ClickType.MOUSE_RIGHT : ClickType.MOUSE_LEFT;
        }

        if (clickType == ClickType.MOUSE_LEFT && node.getChildren().size() > 0 || (node instanceof ArgumentCommandNode<?, ?> && !givenInput)) {
            createCommandGui(player, gui, node, currentCommandPath, givenInput).open();
        } else {
            execute(player, currentCommandPath);
        }
        gui.close();
    }


    /**
     * Executes the command
     * @param player player to execute command as.
     * @param commandTree command tree to execute.
     */
    private static void execute(ServerPlayerEntity player, List<String> commandTree) {
        try {
            // Execute
            // we "fake" the command
            StringBuilder builder = new StringBuilder();

            // Builds the command from commandTree
            commandTree.forEach(nd -> builder.append(nd).append(" "));
            // Delete last space
            builder.deleteCharAt(builder.length() - 1);

            player.closeHandledScreen();

            player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource(), builder.toString());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal(e.getMessage()));
        }
    }



    static {
        final NbtCompound customData = new NbtCompound();
        customData.putInt("CustomModelData", config.guiItemModelData);
        customData.putInt("HideFlags", 127);

        YES_BUTTON.setCustomName(Text.translatable("gui.done"));
        NO_BUTTON.setCustomName(Text.translatable("gui.cancel"));

        ItemStack create = new ItemStack(Items.PLAYER_HEAD);
        create.setNbt(customData.copy());
        itemCommandMap.put("create", create);

        ItemStack select = new ItemStack(Items.SPECTRAL_ARROW);
        select.setNbt(customData.copy());
        itemCommandMap.put("select", select);

        ItemStack deselect = new ItemStack(Items.ARROW);
        deselect.setNbt(customData.copy());
        itemCommandMap.put("deselect", deselect);

        ItemStack list = new ItemStack(Items.PAPER);
        list.setNbt(customData.copy());
        itemCommandMap.put("list", list);

        ItemStack remove = new ItemStack(Items.BARRIER);
        remove.setNbt(customData.copy());
        itemCommandMap.put("remove", remove);

        // Edit
        ItemStack edit = new ItemStack(Items.TRIDENT);
        edit.setNbt(customData.copy());
        itemCommandMap.put("edit", edit);

        ItemStack behaviour = new ItemStack(Items.CREEPER_HEAD);
        behaviour.setNbt(customData.copy());
        itemCommandMap.put("behaviour", behaviour);

        ItemStack commands = new ItemStack(Items.COMMAND_BLOCK);
        commands.setNbt(customData.copy());
        itemCommandMap.put("commands", commands);

        ItemStack equipment = new ItemStack(Items.IRON_CHESTPLATE);
        equipment.setNbt(customData.copy());
        itemCommandMap.put("equipment", equipment);

        ItemStack messages = new ItemStack(Items.WRITABLE_BOOK);
        messages.setNbt(customData.copy());
        itemCommandMap.put("messages", messages);

        ItemStack movement = new ItemStack(Items.MINECART);
        movement.setNbt(customData.copy());
        itemCommandMap.put("movement", movement);

        ItemStack look = new ItemStack(Items.ENDER_EYE);
        look.setNbt(customData.copy());
        itemCommandMap.put("look", look);

        ItemStack name = new ItemStack(Items.NAME_TAG);
        name.setNbt(customData.copy());
        itemCommandMap.put("name", name);

        ItemStack path = new ItemStack(Items.POWERED_RAIL);
        path.setNbt(customData.copy());
        itemCommandMap.put("path", path);


        ItemStack pose = new ItemStack(Items.ARMOR_STAND);
        pose.setNbt(customData.copy());
        itemCommandMap.put("pose", pose);

        ItemStack mount = new ItemStack(Items.SADDLE);
        mount.setNbt(customData.copy());
        itemCommandMap.put("mount", mount);

        ItemStack professions = new ItemStack(Items.DIAMOND_PICKAXE);
        professions.setNbt(customData.copy());
        itemCommandMap.put("professions", professions);

        ItemStack skin = new ItemStack(Items.PLAYER_HEAD);
        skin.setNbt(customData.copy());
        itemCommandMap.put("skin", skin);

        ItemStack tags = new ItemStack(Items.GLOW_ITEM_FRAME);
        tags.setNbt(customData.copy());
        itemCommandMap.put("tags", tags);

        ItemStack type = new ItemStack(Items.SHEEP_SPAWN_EGG);
        type.setNbt(customData.copy());
        itemCommandMap.put("type", type);

        // Messages
        ItemStack messageId = new ItemStack(Items.KNOWLEDGE_BOOK);
        messageId.setNbt(customData.copy());
        itemCommandMap.put("message id", messageId);


        ItemStack messageSwap = new ItemStack(Items.WEEPING_VINES);
        messageSwap.setNbt(customData.copy());
        itemCommandMap.put("swap", messageSwap);

        ItemStack clear = new ItemStack(Items.LAVA_BUCKET);
        clear.setNbt(customData.copy());
        itemCommandMap.put("clear", clear);


        // Presets
        ItemStack preset = new ItemStack(Items.CREEPER_HEAD);
        preset.setNbt(customData.copy());
        itemCommandMap.put("preset", preset);

        ItemStack save = new ItemStack(Items.CAULDRON);
        save.setNbt(customData.copy());
        itemCommandMap.put("save", save);

        ItemStack load = new ItemStack(Items.GLOW_SQUID_SPAWN_EGG);
        load.setNbt(customData.copy());
        itemCommandMap.put("load", load);


        ItemStack tp = new ItemStack(Items.ENDER_PEARL);
        tp.setNbt(customData.copy());
        itemCommandMap.put("tp", tp);

        ItemStack entity = new ItemStack(Items.ZOMBIE_HEAD);
        entity.setNbt(customData.copy());
        itemCommandMap.put("entity", entity);

        ItemStack location = new ItemStack(Items.TRIPWIRE_HOOK);
        location.setNbt(customData.copy());
        itemCommandMap.put("location", location);


        ItemStack action = new ItemStack(Items.CHAIN_COMMAND_BLOCK);
        action.setNbt(customData.copy());
        itemCommandMap.put("action", action);

        ItemStack goTo = new ItemStack(Items.MINECART);
        goTo.setNbt(customData.copy());
        itemCommandMap.put("goto", goTo);

        ItemStack interact = new ItemStack(Items.REDSTONE_TORCH);
        interact.setNbt(customData.copy());
        itemCommandMap.put("interact", interact);

        // Types
        ItemStack player = new ItemStack(Items.PLAYER_HEAD);
        GameProfile texturesProfile = new GameProfile(null, "taterzen");
        PropertyMap properties = texturesProfile.getProperties();
        properties.put("textures", new Property("ewogICJ0aW1lc3RhbXAiIDogMTYwNjIyODAxMzY0NCwKICAicHJvZmlsZUlkIiA6ICJiMGQ0YjI4YmMxZDc0ODg5YWYwZTg2NjFjZWU5NmFhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaW5lU2tpbl9vcmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTczNTE0YTIzMjQ1ZjE1ZGJhZDVmYjRlNjIyMTYzMDIwODY0Y2NlNGMxNWQ1NmRlM2FkYjkwZmE1YTcxMzdmZCIKICAgIH0KICB9Cn0=", "T4Mifh5Yr/+jjAe6y+Ai7d1BPIWQGXc6vwtDL9GgxvQFYtxeD2VuSMNniLoSkP5koBDyHE9ZLgzE2GGAbBSGFgdEKBK7stUPEaUhCET6NKQGli369my3t4Z/4fTkFd9lJmMjP84xIo33E69umQLRZN6MfxmAFXdAl0fkjBdpVi3zLsTdgyu01PhlF9/P4TMXJmNjeiUDt6IjdHgWN1UVFYfAMr9UnCvBNQ/Z4MzxXEm8lGrhq0u7piZqJZ4hb15vHVfixXwtJQkJSBxyzry2W9ZZ2l4xReYX4LbBxU2mRVY5ylRbbolpDuMjXJ6vcg+hRQ9c5HhKkYLm/GOloYEHF/LA5FjGD0QGPW/+uzPfFc9b9swdTUXrJS18/d0dYUDvnHWacDuSoQDfb9eszvs4p6JW04Kd/fPAjLrHm36itVgmrkGa4+fA0Sd/3qo3JaRN6rkbzvppc9s7T2jrhz2+h+hSiiXdRv7v1vMhHVFaOayzBmckL+aKcq7HEsDg1MMauoA/OzkWekuk4FqbgZz49nylOcCHVfd7X1SO7D1BicTgdvGGTOVZtYCyfMKCxcxXFgcqQe88BcLujYWsWafO+VPer9RykXAStb80L020KA0FsQ3zOIC0SBgGlTH5E2Z66AyBEcevYqfIUu1G6Gq4uWINrMae4ZKAABOhtoWH+1Y="));
        player.setNbt(customData.copy());
        NbtCompound skinTag = new NbtCompound();
        NbtHelper.writeGameProfile(skinTag, texturesProfile);
        player.setNbt(skinTag);

        itemCommandMap.put("minecraft:player", player);
        itemCommandMap.put("player", player);
        itemCommandMap.put("reset", player);
    }
}
