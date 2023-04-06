package org.samo_lego.taterzens.fabric.compatibility.carpet;

import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import org.samo_lego.taterzens.api.professions.AbstractProfession;
import org.samo_lego.taterzens.npc.NPCData;
import org.samo_lego.taterzens.npc.TaterzenNPC;

import java.util.HashSet;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import static org.samo_lego.taterzens.Taterzens.MOD_ID;

public class ScarpetProfession extends AbstractProfession {
    private static final TaterzenScarpetEvent PICKUP_EVENT = new TaterzenScarpetEvent("taterzen_tries_pickup", 3);
    private static final TaterzenScarpetEvent INTERACTION_EVENT = new TaterzenScarpetEvent("taterzen_interacted", 5);
    private static final TaterzenScarpetEvent BEING_ATTACKED_EVENT = new TaterzenScarpetEvent("taterzen_is_attacked", 3);
    private static final TaterzenScarpetEvent TICK_MOVEMENT_EVENT = new TaterzenScarpetEvent("taterzen_movement_ticks", 2);
    private static final TaterzenScarpetEvent REMOVED_EVENT = new TaterzenScarpetEvent("taterzen_removed", 2);
    private static final TaterzenScarpetEvent READ_NBT_EVENT = new TaterzenScarpetEvent("taterzen_nbt_loaded", 3);
    private static final TaterzenScarpetEvent SAVE_NBT_EVENT = new TaterzenScarpetEvent("taterzen_nbt_saved", 3);
    private static final TaterzenScarpetEvent MOVEMENT_SET_EVENT = new TaterzenScarpetEvent("taterzen_movement_set", 3);
    private static final TaterzenScarpetEvent BEHAVIOUR_SET_EVENT = new TaterzenScarpetEvent("taterzen_behaviour_set", 3);
    private static final TaterzenScarpetEvent TRY_RANGED_ATTACK_EVENT = new TaterzenScarpetEvent("taterzen_tries_ranged_attack", 3);
    private static final TaterzenScarpetEvent TRY_MELEE_ATTACK_EVENT = new TaterzenScarpetEvent("taterzen_tries_melee_attack", 3);
    private static final TaterzenScarpetEvent PLAYERS_NEARBY_EVENT = new TaterzenScarpetEvent("taterzen_approached_by", 3);

    private final HashSet<Value> SCARPET_TRAITS = new HashSet<>();
    public static final Identifier ID = new Identifier(MOD_ID, "scarpet_profession");

    public ScarpetProfession(TaterzenNPC npc) {
        super(npc);
    }

    /**
     * Adds a string profession to the taterzen that can be used (mainly) in scarpet.
     * @param scarpetTrait scarpet profession that should be added to taterzen.
     */
    public void addTrait(String scarpetTrait) {
        this.SCARPET_TRAITS.add(StringValue.of(scarpetTrait));
    }

    /**
     * Tries to remove the scarpet profession from taterzen.
     * @param scarpetTrait profession to remove.
     * @return true if removal was successful, otherwise false.
     */
    public boolean removeTrait(String scarpetTrait) {
        return this.SCARPET_TRAITS.remove(StringValue.of(scarpetTrait));
    }

    /**
     * Gets the set of scarpet professions.
     * @return set of scarpet professions that are linked to taterzen.
     */
    public HashSet<Value> getTraits() {
        return this.SCARPET_TRAITS;
    }
    @Override
    public boolean tryPickupItem(ItemEntity itemEntity) {
        PICKUP_EVENT.triggerCustomEvent(this.npc, this.getTraits(), itemEntity);
        return itemEntity.getStack().isEmpty() || itemEntity.isRemoved();
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d pos, Hand hand) {
        INTERACTION_EVENT.triggerCustomEvent(this.npc, this.getTraits(), player, ValueConversions.of(pos), hand);

        return super.interactAt(player, pos, hand);
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        BEING_ATTACKED_EVENT.triggerCustomEvent(this.npc, this.getTraits(), attacker);
        return super.handleAttack(attacker);
    }

    @Override
    public void onPlayersNearby(List<ServerPlayerEntity> players) {
        PLAYERS_NEARBY_EVENT.triggerCustomEvent(this.npc, this.getTraits(), players);
    }

    @Override
    public ActionResult tickMovement() {
        TICK_MOVEMENT_EVENT.triggerCustomEvent(this.npc, this.getTraits());
        return super.tickMovement();
    }

    @Override
    public void onRemove() {
        REMOVED_EVENT.triggerCustomEvent(this.npc, this.getTraits());
    }

    @Override
    public void readNbt(NbtCompound tag) {
        READ_NBT_EVENT.triggerCustomEvent(this.npc, this.getTraits(), tag);

        NbtList scarpetTraits = (NbtList) tag.get("ScarpetTraits");
        if (scarpetTraits != null) {
            scarpetTraits.forEach(profession -> this.addTrait(profession.asString()));
        }
    }

    @Override
    public void saveNbt(NbtCompound tag) {
        SAVE_NBT_EVENT.triggerCustomEvent(this.npc, this.getTraits(), tag);

        if (!this.SCARPET_TRAITS.isEmpty()) {
            NbtList scarpetTraits = new NbtList();
            this.SCARPET_TRAITS.forEach(prof -> scarpetTraits.add(NbtString.of(prof.getPrettyString())));
            tag.put("ScarpetTraits", scarpetTraits);
        }
    }

    @Override
    public void onMovementSet(NPCData.Movement movement) {
        MOVEMENT_SET_EVENT.triggerCustomEvent(this.npc, this.getTraits(), movement);
    }

    @Override
    public void onBehaviourSet(NPCData.Behaviour behaviourLevel) {
        BEHAVIOUR_SET_EVENT.triggerCustomEvent(this.npc, this.getTraits(), behaviourLevel);
    }

    @Override
    public boolean cancelRangedAttack(LivingEntity target) {
        TRY_RANGED_ATTACK_EVENT.triggerCustomEvent(this.npc, this.getTraits(), target);
        return super.cancelRangedAttack(target);
    }

    @Override
    public boolean cancelMeleeAttack(Entity target) {
        TRY_MELEE_ATTACK_EVENT.triggerCustomEvent(this.npc, this.getTraits(), target);
        return super.cancelMeleeAttack(target);
    }
}
