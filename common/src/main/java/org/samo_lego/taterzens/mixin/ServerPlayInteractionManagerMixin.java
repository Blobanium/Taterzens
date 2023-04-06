package org.samo_lego.taterzens.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayInteractionManagerMixin {

    @Final
    @Shadow
    protected ServerPlayerEntity player;

    /**
     * Used for detecting block breaking. Broken blocks count as new nodes
     * for the path of player's {@link org.samo_lego.taterzens.npc.TaterzenNPC}.
     * Activated only if player is in path edit mode and has a selected Taterzen.
     *
     * @param blockPos  position of the broken block
     * @param action    action the player is trying to do
     * @param direction direction
     * @param i
     * @param j
     * @param ci
     */
    @Inject(
            method = "processBlockBreakingAction",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAttackBlock(BlockPos blockPos, PlayerActionC2SPacket.Action action, Direction direction, int i, int j, CallbackInfo ci) {
        if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            ITaterzenEditor player = (ITaterzenEditor) this.player;
            if (player.getNpc() != null && ((ITaterzenEditor) this.player).getEditorMode() == ITaterzenEditor.EditorMode.PATH) {
                player.getNpc().addPathTarget(blockPos);
                ((ServerPlayerEntity) player).networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, Blocks.REDSTONE_BLOCK.getDefaultState()));
                ci.cancel();
            }
        }
    }
}
