package org.samo_lego.taterzens.npc;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static org.samo_lego.taterzens.Taterzens.config;

/**
 * Deafult NPC data.
 * Used for taterzen attributes.
 */
public class NPCData {
    /**
     * Current equipment editor for Taterzen.
     */
    @Nullable
    public PlayerEntity equipmentEditor = null;
    /**
     * Default Taterzen movement.
     */
    public Movement movement = Movement.NONE;
    /**
     * Path nodes, used when movement
     * is set to {@link Movement#FORCED_PATH}
     * or {@link Movement#PATH}.
     */
    public ArrayList<BlockPos> pathTargets = new ArrayList<>();
    /**
     * Current index position
     * in {@link NPCData#pathTargets}.
     */
    public int currentMoveTarget = 0;
    /**
     * Messages of Taterzen.
     * Saved as &lt;Message Text, Delay&gt;
     */
    public final ArrayList<Pair<Text, Integer>> messages = new ArrayList<>();
    /**
     * Permission level of Taterzen.
     */
    public int permissionLevel = config.defaults.commandPermissionLevel;

    /**
     * Default behaviour of Taterzen.
     */
    public Behaviour behaviour = Behaviour.PASSIVE;

    public final Follow follow = new Follow();
    public boolean allowSounds = !config.defaults.ambientSounds.isEmpty() || !config.defaults.hurtSounds.isEmpty() || !config.defaults.deathSounds.isEmpty();

    public ArrayList<String> ambientSounds = new ArrayList<>();

    public ArrayList<String> hurtSounds = new ArrayList<>();

    public ArrayList<String> deathSounds = new ArrayList<>();

    public long minCommandInteractionTime = config.defaults.minInteractionTime;
    public String commandCooldownMessage = config.defaults.commandCooldownMessage;

    public final Map<String, Boolean> booleanTags = new HashMap<>();

    public static class Follow {
        /**
         * UUID of entity to follow.
         */
        @Nullable
        public UUID targetUuid;
        public FollowTypes type = FollowTypes.NONE;
    }

    /**
     * Types of movement a Taterzen can perform.
     * FORCED types will always follow the type strictly.
     *
     */
    public enum Movement {
        /**
         * No movement at all.
         */
        NONE,
        /**
         * Rotation of the body and head, free at Taterzen's will.
         */
        LOOK,
        /**
         * Rotation of the body and head when a player is in radius of 4 blocks.
         */
        FORCED_LOOK,
        /**
         * Movement to the selected nodes from pathTargets, at Taterzen's will.
         */
        PATH,
        /**
         * Forced movement to the selected nodes from pathTargets.
         */
        FORCED_PATH,
        /**
         * Ticks movement, but does nothing by itself
         */
        TICK,
        /**
         * Wandering around at Taterzen's will.
         */
        FREE
    }

    /**
     * Behaviour types.
     */
    public enum Behaviour {
        /**
         * Doesn't attack. What's a weapon?
         */
        PASSIVE,
        /**
         * Peaceful, but can bite back if attacked.
         */
        DEFENSIVE,
        /**
         * Will turn against hostile mobs.
         */
        FRIENDLY,
        /**
         * Attacks all living creatures.
         */
        HOSTILE
    }

    public enum FollowTypes {
        NONE,
        UUID,
        PLAYERS,
        MOBS
    }
}
