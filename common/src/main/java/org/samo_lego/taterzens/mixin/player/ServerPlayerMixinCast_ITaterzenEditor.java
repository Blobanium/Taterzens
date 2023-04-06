package org.samo_lego.taterzens.mixin.player;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.samo_lego.taterzens.npc.TaterzenNPC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.successText;

/**
 * Additional methods for players to track {@link TaterzenNPC}
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixinCast_ITaterzenEditor implements ITaterzenEditor {

    @Unique
    private final ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

    @Unique
    private TaterzenNPC selectedNpc;
    @Unique
    private int selectedMsgId = -1; // -1 as no selected msg to edit

    @Unique
    private byte lastRenderTick;
    @Unique
    private EditorMode editorMode = EditorMode.NONE;

    /**
     * Used for showing the path particles.
     */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        ITaterzenEditor editor = (ITaterzenEditor) this.self;
        if (editor.getNpc() != null && lastRenderTick++ > 4) {
            if (this.editorMode == EditorMode.PATH) {
                ArrayList<BlockPos> pathTargets = editor.getNpc().getPathTargets();
                DustParticleEffect effect = new DustParticleEffect(
                        new Vector3f(
                                config.path.color.red / 255.0F,
                                config.path.color.green / 255.0F,
                                config.path.color.blue / 255.0F
                        ),
                        1.0F);

                for (int i = 0; i < pathTargets.size(); ++i) {
                    BlockPos pos = pathTargets.get(i);
                    BlockPos nextPos = pathTargets.get(i + 1 == pathTargets.size() ? 0 : i + 1);

                    int deltaX = pos.getX() - nextPos.getX();
                    int deltaY = pos.getY() - nextPos.getY();
                    int deltaZ = pos.getZ() - nextPos.getZ();

                    double distance = Math.sqrt(pos.getSquaredDistance(nextPos));
                    for (double j = 0; j < distance; j += 0.5D) {
                        double x = pos.getX() - j / distance * deltaX;
                        double y = pos.getY() - j / distance * deltaY;
                        double z = pos.getZ() - j / distance * deltaZ;
                        ParticleS2CPacket packet = new ParticleS2CPacket(effect, true, x + 0.5D, y + 1.5D, z + 0.5D, 0.1F, 0.1F, 0.1F, 1.0F, 1);
                        this.self.networkHandler.sendPacket(packet);
                    }
                }
            }
            if (this.editorMode != EditorMode.NONE) {
                self.sendMessage(successText("taterzens.tooltip.current_editor", String.valueOf(this.editorMode)), true);
            }

            this.lastRenderTick = 0;
        }
    }

    @Override
    public void setEditorMode(EditorMode mode) {
        ITaterzenEditor editor = (ITaterzenEditor) this.self;

        if (editor.getNpc() != null) {
            World world = self.getWorld();
            if (this.editorMode == EditorMode.PATH && mode != EditorMode.PATH) {
                editor.getNpc().getPathTargets().forEach(blockPos -> self.networkHandler.sendPacket(
                        new BlockUpdateS2CPacket(blockPos, world.getBlockState(blockPos))
                ));
            } else if (this.editorMode != EditorMode.PATH && mode == EditorMode.PATH) {
                editor.getNpc().getPathTargets().forEach(blockPos -> self.networkHandler.sendPacket(
                        new BlockUpdateS2CPacket(blockPos, Blocks.REDSTONE_BLOCK.getDefaultState())
                ));
            }

            if (this.editorMode == EditorMode.MESSAGES && mode != EditorMode.MESSAGES) {
                this.setEditingMessageIndex(-1);
            }
        }

        this.editorMode = mode;
    }

    @Override
    public EditorMode getEditorMode() {
        return this.editorMode;
    }

    /**
     * Gets the selected {@link TaterzenNPC} if player has it.
     * @return TaterzenNPC if player has one selected, otherwise null.
     */
    @Nullable
    @Override
    public TaterzenNPC getNpc() {
        return this.selectedNpc;
    }

    @Override
    public boolean selectNpc(@Nullable TaterzenNPC npc) {
        if (npc != null && !npc.allowEditBy(this.self) &&
                !Taterzens.getInstance().getPlatform().checkPermission(
                        this.self.getCommandSource(), "taterzens.npc.select.bypass", config.perms.selectBypassLevel)) {
            return false;
        }

        if (this.getEditorMode() != EditorMode.NONE) {
            this.setEditorMode(EditorMode.NONE);
        }

        TaterzenNPC selectedNpc = this.selectedNpc;
        this.selectedNpc = npc;

        if (npc != null) {
            npc.sendProfileUpdates();
        }

        if (selectedNpc != null) {
            selectedNpc.sendProfileUpdates();
        }

        return true;
    }

    @Override
    public void setEditingMessageIndex(int selected) {
        this.selectedMsgId = selected;
    }

    @Override
    public int getEditingMessageIndex() {
        return this.selectedMsgId;
    }
}
