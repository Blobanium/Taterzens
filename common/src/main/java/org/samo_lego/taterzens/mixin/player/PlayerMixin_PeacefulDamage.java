package org.samo_lego.taterzens.mixin.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import org.samo_lego.taterzens.npc.TaterzenNPC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin_PeacefulDamage extends LivingEntity {

    @Unique
    private final PlayerEntity self = (PlayerEntity) (Object) this;

    protected PlayerMixin_PeacefulDamage(EntityType<? extends LivingEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "damage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V",
                    shift = At.Shift.AFTER),
            cancellable = true
    )
    private void enableTaterzenPeacefulDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof TaterzenNPC && this.self.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            // Vanilla cancels damage if the world is in peaceful mode
            cir.setReturnValue(amount == 0.0f || super.damage(source, amount));
        }
    }

}
