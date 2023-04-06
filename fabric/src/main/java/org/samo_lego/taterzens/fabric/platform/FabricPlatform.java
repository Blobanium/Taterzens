package org.samo_lego.taterzens.fabric.platform;

import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.taterzens.fabric.mixin.AMappedRegistry;
import org.samo_lego.taterzens.npc.TaterzenNPC;
import org.samo_lego.taterzens.platform.Platform;

import java.nio.file.Path;
import java.util.Collections;

import static org.samo_lego.taterzens.Taterzens.NPC_ID;
import static org.samo_lego.taterzens.Taterzens.TATERZEN_TYPE;
import static org.samo_lego.taterzens.commands.NpcCommand.npcNode;
import static org.samo_lego.taterzens.fabric.gui.EditorGUI.createCommandGui;

public class FabricPlatform extends Platform {

    private static final int REGISTRY_ITEMS_SIZE = ((AMappedRegistry<?>) Registries.ITEM).getById().size();

    @Override
    public Path getConfigDirPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public int getItemRegistrySize() {
        return REGISTRY_ITEMS_SIZE;
    }

    /**
     * Checks permission of commandSource using Lucko's
     * permission API.
     * If permission isn't set, it will require the commandSource
     * to have permission level set in the config.
     *
     * @param commandSource commandSource to check permission for.
     * @param permission permission node to check.
     * @param fallbackLevel level to require if permission isn't set
     * @return true if commandSource has the permission, otherwise false
     */
    @Override
    public boolean checkPermission(ServerCommandSource commandSource, String permission, int fallbackLevel) {
        // Enable command blocks, therefore null check
        return commandSource.getEntity() == null || Permissions.check(commandSource, permission, fallbackLevel);
    }


    @Override
    public void registerTaterzenType() {
        final EntityType<TaterzenNPC> type = Registry.register(
                Registries.ENTITY_TYPE,
                NPC_ID,
                FabricEntityTypeBuilder
                        .<TaterzenNPC>create(SpawnGroup.MISC, TaterzenNPC::new)
                        .dimensions(EntityDimensions.changing(0.6F, 1.8F))
                        .build());

        TATERZEN_TYPE = () -> type;
    }

    @Override
    public void openEditorGui(ServerPlayerEntity player) {
        SimpleGui editorGUI = createCommandGui(player, null, npcNode, Collections.singletonList("npc"), false);
        editorGUI.open();
    }
}
