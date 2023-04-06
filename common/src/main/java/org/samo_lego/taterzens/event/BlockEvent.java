package org.samo_lego.taterzens.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;

public class BlockEvent {

    /**
     * Used if player is in path edit mode. Interacted blocks are removed from the path
     * of selected {@link org.samo_lego.taterzens.npc.TaterzenNPC}.
     *
     * @param Player player breaking the block.
     * @param world world where block is being broken.
     * @param blockPos position of block interaction.
     *
     * @return FAIL if player has selected NPC and is in path edit mode, otherwise PASS.
     */
    public static ActionResult onBlockInteract(PlayerEntity Player, World world, BlockPos blockPos) {
        if(Player instanceof ServerPlayerEntity) { // Prevents crash on client
            ITaterzenEditor player = (ITaterzenEditor) Player;
            if(player.getNpc() != null && ((ITaterzenEditor) Player).getEditorMode() == ITaterzenEditor.EditorMode.PATH) {
                player.getNpc().removePathTarget(blockPos);
                ((ServerPlayerEntity) player).networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, world.getBlockState(blockPos)));
                return ActionResult.FAIL;
            }
        }

        return ActionResult.PASS;
    }
}