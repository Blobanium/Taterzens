package org.samo_lego.taterzens.fabric.client_fix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.npc.TaterzenNPC;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // We just do this to avoid client crashes
        EntityRendererRegistry.register(Taterzens.TATERZEN_TYPE.get(), context -> new MobEntityRenderer<>(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 1.0F) {
            @Override
            public Identifier getTexture(TaterzenNPC entity) {
                return DefaultSkinHelper.getTexture(entity.getUuid());
            }
        });
    }
}
