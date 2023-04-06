package org.samo_lego.taterzens.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.entity.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.api.TaterzensAPI;
import org.samo_lego.taterzens.api.professions.TaterzenProfession;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.samo_lego.taterzens.interfaces.ITaterzenPlayer;
import org.samo_lego.taterzens.mixin.accessors.AChunkMap;
import org.samo_lego.taterzens.mixin.accessors.AClientboundAddPlayerPacket;
import org.samo_lego.taterzens.mixin.accessors.AClientboundPlayerInfoPacket;
import org.samo_lego.taterzens.mixin.accessors.AEntityTrackerEntry;
import org.samo_lego.taterzens.npc.ai.goal.*;
import org.samo_lego.taterzens.npc.commands.AbstractTaterzenCommand;
import org.samo_lego.taterzens.npc.commands.CommandGroups;
import org.samo_lego.taterzens.util.TextUtil;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static net.minecraft.util.Hand.MAIN_HAND;
import static org.samo_lego.taterzens.Taterzens.*;
import static org.samo_lego.taterzens.mixin.accessors.APlayer.getPLAYER_MODE_CUSTOMISATION;
import static org.samo_lego.taterzens.util.TextUtil.errorText;
import static org.samo_lego.taterzens.util.TextUtil.successText;

/**
 * The NPC itself.
 */
public class TaterzenNPC extends PathAwareEntity implements CrossbowUser, RangedAttackMob {

    /**
     * Data of the NPC.
     */
    private final NPCData npcData = new NPCData();

    private final CommandGroups commandGroups;
    private ServerPlayerEntity fakePlayer;
    private final LinkedHashMap<Identifier, TaterzenProfession> professions = new LinkedHashMap<>();
    private GameProfile gameProfile;

    /**
     * Goals
     * Public so they can be accessed from professions.
     */
    public final LookAtEntityGoal lookPlayerGoal = new LookAtEntityGoal(this, PlayerEntity.class, 8.0F);
    public final LookAroundGoal lookAroundGoal = new LookAroundGoal(this);
    public final WanderAroundGoal wanderAroundFarGoal = new WanderAroundGoal(this, 1.0D, 30);

    public final SwimGoal swimGoal = new SwimGoal(this);

    /**
     * Target selectors.
     */
    public final ActiveTargetGoal<LivingEntity> followTargetGoal = new ActiveTargetGoal<>(this, LivingEntity.class, 100, false, true, target -> !this.isTeammate(target));
    public final ActiveTargetGoal<HostileEntity> followMonstersGoal = new ActiveTargetGoal<>(this, HostileEntity.class, 100, false, true, target -> !this.isTeammate(target));

    /**
     * Tracking movement
     */
    public final TrackEntityGoal trackLivingGoal = new TrackEntityGoal(this, LivingEntity.class, target -> !(target instanceof ServerPlayerEntity) && target.isAlive());
    public final TrackEntityGoal trackPlayersGoal = new TrackEntityGoal(this, ServerPlayerEntity.class, target -> !((ServerPlayerEntity) target).isDisconnected() && target.isAlive());
    public final TrackUuidGoal trackUuidGoal = new TrackUuidGoal(this, entity -> entity.getUuid().equals(this.npcData.follow.targetUuid) && entity.isAlive());


    /**
     * Used for {@link NPCData.Movement#PATH}.
     */
    public final LazyPathGoal pathGoal = new LazyPathGoal(this, 1.0D);
    public final DirectPathGoal directPathGoal = new DirectPathGoal(this, 1.0D);

    /**
     * Attack-based goals
     */
    public final ProjectileAttackGoal projectileAttackGoal = new ProjectileAttackGoal(this, 1.2D, 40, 40.0F);
    public final ReachMeleeAttackGoal reachMeleeAttackGoal = new ReachMeleeAttackGoal(this, 1.2D, false);
    public final TeamRevengeGoal revengeGoal = new TeamRevengeGoal(this);
    public final MeleeAttackGoal attackMonstersGoal = new MeleeAttackGoal(this, 1.2D, false);
    private @Nullable Vec3d respawnPosition;

    /**
     * UUID of the "owner" that has locked this NPC.
     */
    private UUID lockedUuid;
    private final Map<UUID, Long> commandTimes = new HashMap<>();
    private ServerPlayerEntity lookTarget;

    public TaterzenNPC(World world) {
        this(TATERZEN_TYPE.get(), world);
    }

    /**
     * Creates a TaterzenNPC.
     * You'd probably want to use
     * {@link org.samo_lego.taterzens.api.TaterzensAPI#createTaterzen(ServerWorld, String, Vec3d, float[])} or
     * {@link org.samo_lego.taterzens.api.TaterzensAPI#createTaterzen(ServerPlayerEntity, String)}
     * instead, as this one doesn't set the position and custom name.
     *
     * @param entityType Taterzen entity type
     * @param world Taterzen's world
     */
    public TaterzenNPC(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(0.6F);
        this.setCanPickUpLoot(true);
        this.setCustomNameVisible(true);
        this.setCustomName(this.getName());
        this.setInvulnerable(config.defaults.invulnerable);
        this.setPersistent();
        this.experiencePoints = 0;
        this.setMovementSpeed(0.4F);

        this.gameProfile = new GameProfile(this.getUuid(), this.getName().getString());
        this.commandGroups = new CommandGroups(this);

        // Null check due top gravity changer incompatibility
        if (this.fakePlayer == null) {
            this.constructFakePlayer();
        }

        // Set the sounds of this NPC to the default values from the config file
        // (will be overwritten by individual configuration when e.g. loading corresponding NBT data)
        if (!config.defaults.ambientSounds.isEmpty()) {
            this.npcData.ambientSounds = new ArrayList<>(config.defaults.ambientSounds);
        }
        if (!config.defaults.hurtSounds.isEmpty()) {
            this.npcData.hurtSounds = new ArrayList<>(config.defaults.hurtSounds);
        }
        if (!config.defaults.deathSounds.isEmpty()) {
            this.npcData.deathSounds = new ArrayList<>(config.defaults.deathSounds);
        }

    }

    /**
     * Creates default taterzen attributes.
     * @return attribute supplier builder.
     */
    public static DefaultAttributeContainer.Builder createTaterzenAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.25D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2505D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D);
    }

    /**
     * Creates a fake player for this NPC
     * in order to be able to use the
     * player synched data.
     */
    public void constructFakePlayer() {
        this.fakePlayer = new ServerPlayerEntity(this.getServer(), (ServerWorld) this.world, this.gameProfile);
        this.fakePlayer.getDataTracker().set(getPLAYER_MODE_CUSTOMISATION(), (byte) 0x7f);
        this.fakePlayer.setPosition(this.getX(), this.getY(), this.getZ());
        this.fakePlayer.setPitch(this.getPitch());
        this.fakePlayer.setYaw(this.getYaw());
        this.fakePlayer.setHeadYaw(this.headYaw);
    }

    /**
     * Adds sounds to the list of ambient sounds of a Taterzen.
     * @param ambientSound The ambient sound resource location to add.
     */
    public void addAmbientSound(String ambientSound) {
        this.npcData.ambientSounds.add(ambientSound);
    }

    /**
     * Adds sounds to the list of hurt sounds of a Taterzen.
     * @param hurtSound The hurt sound resource location to add.
     */
    public void addHurtSound(String hurtSound) {
        this.npcData.hurtSounds.add(hurtSound);
    }

    /**
     * Adds sounds to the list of death sounds of a Taterzen.
     * @param deathSound The death sound resource location to add.
     */
    public void addDeathSound(String deathSound) {
        this.npcData.deathSounds.add(deathSound);
    }

    /**
     * Removes sounds from the list of ambient sounds of a Taterzen.
     * @param index The index of the ambient sound resource location within the NPCData structure.
     */
    public void removeAmbientSound(int index) {
        this.npcData.ambientSounds.remove(index);
    }

    /**
     * Removes sounds from the list of hurt sounds of a Taterzen.
     * @param index The index of the hurt sound resource location within the NPCData structure.
     */
    public void removeHurtSound(int index) {
        this.npcData.hurtSounds.remove(index);
    }

    /**
     * Removes sounds from the list of death sounds of a Taterzen.
     * @param index The index of the death sound resource location within the NPCData structure.
     */
    public void removeDeathSound(int index) {
        this.npcData.deathSounds.remove(index);
    }

    /**
     * Adds command to the list
     * of commands that will be executed on
     * right-clicking the Taterzen.
     *
     * @param command command to add
     */
    public boolean addCommand(AbstractTaterzenCommand command) {
        if (command.getType() == AbstractTaterzenCommand.CommandType.BUNGEE && !config.bungee.enableCommands) {
            return false;
        }
        return this.commandGroups.addCommand(command);
    }

    /**
     * Adds command to the list
     * of commands that will be executed on
     * right-clicking the Taterzen.
     *
     * @param command    command to add
     * @param groupIndex index of the group to add the command to
     */
    public boolean addCommand(AbstractTaterzenCommand command, int groupIndex) {
        if (command.getType() == AbstractTaterzenCommand.CommandType.BUNGEE && !config.bungee.enableCommands) {
            return false;
        }
        return this.commandGroups.get(groupIndex).add(command);
    }

    /**
     * Gets all available commands
     *
     * @return array of groups, each containing array of commands.
     */
    public CommandGroups getCommandGroups() {
        return this.commandGroups;
    }

    /**
     * Gets commands from specific group.
     *
     * @param group group index.
     * @return array list of commands that will be executed on right click.
     */
    public ArrayList<AbstractTaterzenCommand> getGroupCommands(int group) {
        return this.commandGroups.get(group);
    }

    /**
     * Removes certain command from command list.
     *
     * @param groupIndex   index of the group.
     * @param commandIndex index of the command.
     */
    public void removeGroupCommand(int groupIndex, int commandIndex) {
        this.commandGroups.get(groupIndex).remove(commandIndex);
    }

    /**
     * Clears all the commands Taterzen
     * executes on right-click.
     */
    public void clearAllCommands() {
        this.commandGroups.clear();
    }


    public void clearGroupCommands(int index) {
        this.commandGroups.remove(index);
    }

    @Override
    protected int getPermissionLevel() {
        return this.npcData.permissionLevel;
    }

    public void setPermissionLevel(int newPermissionLevel) {
        this.npcData.permissionLevel = newPermissionLevel;
    }

    /**
     * Sets {@link org.samo_lego.taterzens.npc.NPCData.Movement movement type}
     * and initialises the goals.
     *
     * @param movement movement type
     */
    public void setMovement(NPCData.Movement movement) {
        this.npcData.movement = movement;
        this.goalSelector.remove(this.wanderAroundFarGoal);
        this.goalSelector.remove(this.directPathGoal);
        this.goalSelector.remove(this.pathGoal);
        this.goalSelector.remove(this.lookPlayerGoal);
        this.goalSelector.remove(this.lookAroundGoal);

        // Follow types
        this.goalSelector.remove(this.trackLivingGoal);
        this.goalSelector.remove(this.trackUuidGoal);
        this.goalSelector.remove(this.trackPlayersGoal);

        this.npcData.follow.targetUuid = null;
        this.npcData.follow.type = NPCData.FollowTypes.NONE;

        this.trackPlayersGoal.resetTrackingEntity();
        this.trackLivingGoal.resetTrackingEntity();

        for(TaterzenProfession profession : this.professions.values()) {
            profession.onMovementSet(movement);
        }

        if (movement != NPCData.Movement.NONE && movement != NPCData.Movement.FORCED_LOOK) {
            int priority = 8;
            if (movement == NPCData.Movement.FORCED_PATH) {
                this.goalSelector.add(4, directPathGoal);
                priority = 5;
            } else if (movement == NPCData.Movement.PATH) {
                this.goalSelector.add(4, pathGoal);
            } else if (movement == NPCData.Movement.FREE) {
                this.goalSelector.add(6, wanderAroundFarGoal);
            }

            this.goalSelector.add(priority, lookPlayerGoal);
            this.goalSelector.add(priority + 1, lookAroundGoal);
        }

        if (this.getTag("AllowSwimming", config.defaults.allowSwim)) {
            this.goalSelector.add(0, this.swimGoal);
        }
    }

    /**
     * Gets current movement of taterzen.
     * @return current movement
     */
    public NPCData.Movement getMovement() {
        return this.npcData.movement;
    }

    /**
     * Adds block position as a node in path of Taterzen.
     * @param blockPos position to add.
     */
    public void addPathTarget(BlockPos blockPos) {
        this.npcData.pathTargets.add(blockPos);
        this.setPositionTarget(this.npcData.pathTargets.get(0), 1);
    }

    @Override
    public DataTracker getDataTracker() {
        if (this.fakePlayer == null) {
            // Fixes gravity changer incompatibility
            this.constructFakePlayer();
        }
        return this.fakePlayer.getDataTracker();
    }

    /**
     * Handles name visibility on sneaking
     * @param sneaking whether npc's name should look like on sneaking.
     */
    @Override
    public void setSneaking(boolean sneaking) {
        this.fakePlayer.setSneaking(sneaking);
        super.setSneaking(sneaking);
    }

    /**
     * Sets the npc pose.
     * @param pose entity pose.
     */
    @Override
    public void setPose(EntityPose pose) {
        this.fakePlayer.setPose(pose);
        super.setPose(pose);
    }

    /**
     * Removes node from path targets.
     * @param blockPos position from path to remove
     */
    public void removePathTarget(BlockPos blockPos) {
        this.npcData.pathTargets.remove(blockPos);
    }

    /**
     * Gets the path nodes / targets.
     * @return array list of block positions.
     */
    public ArrayList<BlockPos> getPathTargets() {
        return this.npcData.pathTargets;
    }

    /**
     * Clears all the path nodes / targets.
     */
    public void clearPathTargets() {
        this.npcData.pathTargets = new ArrayList<>();
        this.npcData.currentMoveTarget = 0;
    }

    /**
     * Ticks the movement depending on {@link org.samo_lego.taterzens.npc.NPCData.Movement} type
     */
    @Override
    public void tickMovement() {
        if(this.npcData.equipmentEditor != null)
            return;

        // Profession event
        professionLoop:
        for(TaterzenProfession profession : this.professions.values()) {
            ActionResult result = profession.tickMovement();
            switch(result) {
                case CONSUME: // Stop processing others, but continue with base Taterzen movement tick
                    break professionLoop;
                case FAIL: // Stop whole movement tick
                    return;
                case SUCCESS: // Continue with super, but skip Taterzen's movement tick
                    super.tickMovement();
                    return;
                default: // Continue with other professions
                    break;
            }
        }

        // FORCED_LOOK is processed in tick(), as we get nearby players there
        if(this.npcData.movement != NPCData.Movement.NONE && this.npcData.movement != NPCData.Movement.FORCED_LOOK) {
            this.setYaw(this.headYaw); // Rotates body as well
            LivingEntity target = this.getTarget();

            if((this.npcData.movement == NPCData.Movement.FORCED_PATH ||
                this.npcData.movement == NPCData.Movement.PATH ) &&
                    !this.npcData.pathTargets.isEmpty() &&
                    !this.isNavigating()) {
                // Checking here as well (if path targets size was changed during the previous tick)
                if(this.npcData.currentMoveTarget >= this.npcData.pathTargets.size())
                    this.npcData.currentMoveTarget = 0;

                if(this.getPositionTarget().getSquaredDistance(this.getPos()) < 5.0D) {
                    if(++this.npcData.currentMoveTarget >= this.npcData.pathTargets.size())
                        this.npcData.currentMoveTarget = 0;

                    // New target
                    this.setPositionTarget(this.npcData.pathTargets.get(this.npcData.currentMoveTarget), 1);
                }
            }

            super.tickMovement();
            if (this.isAttacking() && this.getTag("JumpAttack", config.defaults.jumpWhileAttacking) && this.onGround && target != null && this.squaredDistanceTo(target) < 4.0D && this.random.nextInt(5) == 0)
                this.jump();
        } else {
            // As super.aiStep() isn't executed, we check for items that are available to be picked up
            if (this.isAlive() && !this.dead) {
                List<ItemEntity> list = this.world.getNonSpectatingEntities(ItemEntity.class, this.getBoundingBox().expand(1.0D, 0.0D, 1.0D));

                for (ItemEntity itemEntity : list) {
                    if (!itemEntity.isRemoved() && !itemEntity.getStack().isEmpty() && !itemEntity.cannotPickup() && this.canGather(itemEntity.getStack())) {
                        this.loot(itemEntity);
                    }
                }
            }
        }
    }

    /**
     * Ticks the Taterzen and sends appropriate messages
     * to players in radius of 2 blocks.
     */
    @Override
    public void tick() {
        super.tick();

        Box box = this.getBoundingBox().expand(4.0D);
        List<ServerPlayerEntity> players = this.world.getNonSpectatingEntities(ServerPlayerEntity.class, box);

        if(!this.npcData.messages.isEmpty()) {
            for(ServerPlayerEntity player: players) {
                // Filter them here (not use a predicate above)
                // as we need the original list below
                if(((ITaterzenEditor) player).getEditorMode() == ITaterzenEditor.EditorMode.MESSAGES || this.distanceTo(player) > config.messages.speakDistance)
                    continue;

                ITaterzenPlayer pl = (ITaterzenPlayer) player;
                int msgPos = pl.getLastMsgPos(this.getUuid());
                if (msgPos >= this.npcData.messages.size())
                    msgPos = 0;
                if (this.npcData.messages.get(msgPos).getSecond() < pl.ticksSinceLastMessage(this.getUuid())) {
                    player.sendMessage(
                            Text.translatable(config.messages.structure, this.getName().copy(), this.npcData.messages.get(msgPos).getFirst()));
                    // Resetting message counter
                    pl.resetMessageTicks(this.getUuid());

                    ++msgPos;
                    // Setting new message position
                    pl.setLastMsgPos(this.getUuid(), msgPos);
                }
            }
        }
        if(!players.isEmpty()) {
            // We tick forced look here, as we already have players list.
            if(this.npcData.movement == NPCData.Movement.FORCED_LOOK) {
                if (this.lookTarget == null || this.distanceTo(this.lookTarget) > 5.0D || this.lookTarget.isDisconnected() || !this.lookTarget.isAlive()) {
                    this.lookTarget = players.get(this.random.nextInt(players.size()));
                }

                this.lookAtEntity(this.lookTarget, 60.0F, 60.0F);
                this.setHeadYaw(this.getYaw());
            }
            // Tick profession
            for(TaterzenProfession profession : this.professions.values()) {
                profession.onPlayersNearby(players);
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        final List<Packet<ClientPlayPacketListener>> packets = new ArrayList<>();

        // Add to tab list
        final var playerAddPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, this.fakePlayer);
        //noinspection ConstantConditions
        var entry = new PlayerListS2CPacket.Entry(this.gameProfile.getId(), this.gameProfile, false, 0, GameMode.SURVIVAL, this.getDisplayName(), null);
        ((AClientboundPlayerInfoPacket) playerAddPacket).setEntries(Collections.singletonList(entry));
        packets.add(playerAddPacket);

        // Spawn player
        final var spawnPlayerPacket = new PlayerSpawnS2CPacket(this.fakePlayer);
        AClientboundAddPlayerPacket addPlayerPacketAccessor = (AClientboundAddPlayerPacket) spawnPlayerPacket;
        addPlayerPacketAccessor.setId(this.getId());
        addPlayerPacketAccessor.setUuid(this.getUuid());
        addPlayerPacketAccessor.setX(this.getX());
        addPlayerPacketAccessor.setY(this.getY());
        addPlayerPacketAccessor.setZ(this.getZ());
        addPlayerPacketAccessor.setYRot((byte) ((int) (this.getHeadYaw() * 256.0F / 360.0F)));
        addPlayerPacketAccessor.setXRot((byte) ((int) (this.getPitch() * 256.0F / 360.0F)));
        packets.add(spawnPlayerPacket);

        // Rotation
        final var rotateHeadPacket = new EntitySetHeadYawS2CPacket(this, (byte) ((int) (this.getHeadYaw() * 256.0F / 360.0F)));
        packets.add(rotateHeadPacket);

        return new BundleS2CPacket(packets);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public Text getTabListName() {
        if (!config.obscureTabList) return getName();

        var component = Text.literal("").formatted(Formatting.DARK_GRAY);
        component.append(getName());
        component.append(" [NPC]");
        return component;
    }

    /**
     * Sets the custom name
     * @param name new name to be set.
     */
    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        String profileName = "Taterzen";
        if(name != null) {
            profileName = name.getString();
            if(name.getString().length() > 16) {
                // Minecraft kicks you if player has name longer than 16 chars in GameProfile
                profileName = name.getString().substring(0, 16);
            }
        }
        NbtCompound skin = null;
        if(this.gameProfile != null)
            skin = this.writeSkinToTag(this.gameProfile);
        this.gameProfile = new GameProfile(this.getUuid(), profileName);
        if(skin != null) {
            this.setSkinFromTag(skin);
            this.sendProfileUpdates();
        }
    }

    /**
     * Updates Taterzen's {@link GameProfile} for others.
     */
    public void sendProfileUpdates() {
        if (this.world.isClient()) return;

        ServerChunkManager manager = (ServerChunkManager) this.world.getChunkManager();
        ThreadedAnvilChunkStorage storage = manager.threadedAnvilChunkStorage;
        AEntityTrackerEntry trackerEntry = ((AChunkMap) storage).getEntityMap().get(this.getId());
        if (trackerEntry != null) {
            trackerEntry.getSeenBy().forEach(tracking -> trackerEntry.getEntry().startTracking(tracking.getPlayer()));
        }
    }


    /**
     * Applies skin from {@link GameProfile}.
     *
     * @param texturesProfile GameProfile containing textures.
     */
    public void applySkin(GameProfile texturesProfile) {
        if(this.gameProfile == null)
            return;

        // Setting new skin
        setSkinFromTag(writeSkinToTag(texturesProfile));

        // Sending updates
        this.sendProfileUpdates();
    }

    /**
     * Sets the Taterzen skin from tag
     * @param tag compound tag containing the skin
     */
    public void setSkinFromTag(NbtCompound tag) {
        // Clearing current skin
        try {
            PropertyMap map = this.gameProfile.getProperties();
            Property skin = map.get("textures").iterator().next();
            map.remove("textures", skin);
        } catch (NoSuchElementException ignored) { }
        // Setting the skin
        try {
            String value = tag.getString("value");
            String signature = tag.getString("signature");

            if (!value.isEmpty() && !signature.isEmpty()) {
                PropertyMap propertyMap = this.gameProfile.getProperties();
                propertyMap.put("textures", new Property("textures", value, signature));
            }

        } catch (Error ignored) { }
    }

    /**
     * Writes skin to tag
     * @param profile game profile containing skin
     *
     * @return compound tag with skin values
     */
    public NbtCompound writeSkinToTag(GameProfile profile) {
        NbtCompound skinTag = new NbtCompound();
        try {
            PropertyMap propertyMap = profile.getProperties();
            Property skin = propertyMap.get("textures").iterator().next();

            skinTag.putString("value", skin.getValue());
            skinTag.putString("signature", skin.getSignature());
        } catch (NoSuchElementException ignored) { }

        return skinTag;
    }
    /**
     * Loads Taterzen from {@link NbtCompound}.
     * @param tag tag to load Taterzen from.
     */
    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);

        // Has a "preset" tag
        // We want to overwrite self data from that provided by preset
        if (tag.contains("PresetOverride")) {
            this.loadPresetTag(tag);
            return;  // Other data doesn't need to be loaded as it will be handled by preset
        }

        NbtCompound npcTag = tag.getCompound("TaterzenNPCTag");

        // Boolean tags
        NbtCompound tags = npcTag.getCompound("Tags");

        for (String key : tags.getKeys()) {
            this.setTag(key, tags.getBoolean(key));
        }

        // Skin layers
        this.setSkinLayers(npcTag.getByte("SkinLayers"));

        // Sounds
        NbtList ambientSounds = (NbtList) npcTag.get("AmbientSounds");
        if (ambientSounds != null) {
            this.npcData.ambientSounds.clear(); // removes default loaded sounds
            ambientSounds.forEach(snd -> this.addAmbientSound(snd.asString()));
        }

        NbtList hurtSounds = (NbtList) npcTag.get("HurtSounds");
        if (hurtSounds != null) {
            this.npcData.hurtSounds.clear(); // removes default loaded sounds
            hurtSounds.forEach(snd -> this.addHurtSound(snd.asString()));
        }

        NbtList deathSounds = (NbtList) npcTag.get("DeathSounds");
        if (deathSounds != null) {
            this.npcData.deathSounds.clear(); // removes default loaded sounds
            deathSounds.forEach(snd -> this.addDeathSound(snd.asString()));
        }


        // -------------------------------------------------------------
        // Deprecated since 1.10.0
        // Commands
        NbtList commands = (NbtList) npcTag.get("Commands");
        // Bungee commands
        NbtList bungeeCommands = (NbtList) npcTag.get("BungeeCommands");

        if (commands != null && bungeeCommands != null) {
            // Scheduled for removal
            this.commandGroups.fromOldTag(commands, bungeeCommands);
        }
        // -------------------------------------------------------------

        var cmds = npcTag.getCompound("CommandGroups");
        this.commandGroups.fromTag(cmds);

        NbtList pathTargets = (NbtList) npcTag.get("PathTargets");
        if (pathTargets != null) {
            if (pathTargets.size() > 0) {
                pathTargets.forEach(posTag -> {
                    if (posTag instanceof NbtCompound pos) {
                        BlockPos target = new BlockPos(pos.getInt("x"), pos.getInt("y"), pos.getInt("z"));
                        this.addPathTarget(target);
                    }
                });
                this.setPositionTarget(this.npcData.pathTargets.get(0), 1);
            }
        }

        NbtList messages = (NbtList) npcTag.get("Messages");
        if(messages != null && messages.size() > 0) {
            messages.forEach(msgTag -> {
                NbtCompound msgCompound = (NbtCompound) msgTag;
                this.addMessage(TextUtil.fromNbtElement(msgCompound.get("Message")), msgCompound.getInt("Delay"));
            });
        }

        this.setPermissionLevel(npcTag.getInt("PermissionLevel"));

        if (npcTag.contains("Behaviour")) {
            this.setBehaviour(NPCData.Behaviour.valueOf(npcTag.getString("Behaviour")));
        } else {
            this.setBehaviour(NPCData.Behaviour.PASSIVE);
        }

        String profileName = this.getName().getString();
        if(profileName.length() > 16) {
            // Minecraft kicks you if player has name longer than 16 chars in GameProfile
            profileName = profileName.substring(0, 16);
        }

        this.gameProfile = new GameProfile(this.getUuid(), profileName);

        // Skin is cached
        NbtCompound skinTag = npcTag.getCompound("skin");
        this.setSkinFromTag(skinTag);

        // Profession initialising
        NbtList professions = (NbtList) npcTag.get("Professions");
        if(professions != null && professions.size() > 0) {
            professions.forEach(professionTag -> {
                NbtCompound professionCompound = (NbtCompound) professionTag;

                Identifier professionId = new Identifier(professionCompound.getString("ProfessionType"));
                if (PROFESSION_TYPES.containsKey(professionId)) {
                    TaterzenProfession profession = PROFESSION_TYPES.get(professionId).apply(this);
                    this.addProfession(professionId, profession);

                    // Parsing profession data
                    profession.readNbt(professionCompound.getCompound("ProfessionData"));
                } else {
                    Taterzens.LOGGER.error("Taterzen {} was saved with profession id {}, but none of the mods provides it.", this.getName().getString(), professionId);
                }
            });
        }

        // Follow targets
        NbtCompound followTag = npcTag.getCompound("Follow");
        if(followTag.contains("Type"))
            this.setFollowType(NPCData.FollowTypes.valueOf(followTag.getString("Type")));

        if(followTag.contains("UUID"))
            this.setFollowUuid(followTag.getUuid("UUID"));

        if(npcTag.contains("Pose"))
            this.setPose(EntityPose.valueOf(npcTag.getString("Pose")));
        else
            this.setPose(EntityPose.STANDING);

        NbtCompound bodyRotations = npcTag.getCompound("BodyRotations");
        if(!bodyRotations.isEmpty()) {
            this.setPitch(bodyRotations.getFloat("XRot"));
            this.setYaw(bodyRotations.getFloat("YRot"));
        }

        if (npcTag.contains("movement"))
            this.setMovement(NPCData.Movement.valueOf(npcTag.getString("movement")));
        else
            this.setMovement(NPCData.Movement.NONE);

        if (npcTag.contains("LockedBy"))
            this.lockedUuid = npcTag.getUuid("LockedBy");


        // ------------------------------------------------------------
        //  Migration to 1.10.0
        if (npcTag.contains("AllowFlight"))
            this.setAllowFlight(npcTag.getBoolean("AllowFlight"));
        if (npcTag.contains("AllowSwimming"))
            this.setAllowSwimming(npcTag.getBoolean("AllowSwimming"));
        // --------------------------------------------------------------

        this.setMinCommandInteractionTime(npcTag.getLong("MinCommandInteractionTime"));
    }

    /**
     * Saves Taterzen to {@link NbtCompound tag}.
     *
     * @param tag tag to save Taterzen to.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);

        NbtCompound npcTag = new NbtCompound();

        // Vanilla saves CustomNameVisible only if set to true
        super.setCustomNameVisible(tag.contains("CustomNameVisible"));

        npcTag.putString("movement", this.npcData.movement.toString());

        // Boolean tags
        NbtCompound tags = new NbtCompound();

        for (Map.Entry<String, Boolean> entry : this.npcData.booleanTags.entrySet()) {
            tags.putBoolean(entry.getKey(), entry.getValue());
        }

        npcTag.put("Tags", tags);

        // Skin layers
        npcTag.putByte("SkinLayers", this.fakePlayer.getDataTracker().get(getPLAYER_MODE_CUSTOMISATION()));

        // Sounds
        NbtList ambientSounds = new NbtList();
        this.npcData.ambientSounds.forEach(snd -> ambientSounds.add(NbtString.of(snd)));
        npcTag.put("AmbientSounds", ambientSounds);

        NbtList hurtSounds = new NbtList();
        this.npcData.hurtSounds.forEach(snd -> hurtSounds.add(NbtString.of(snd)));
        npcTag.put("HurtSounds", hurtSounds);

        NbtList deathSounds = new NbtList();
        this.npcData.deathSounds.forEach(snd -> deathSounds.add(NbtString.of(snd)));
        npcTag.put("DeathSounds", deathSounds);

        // Commands
        var cmds = new NbtCompound();
        this.commandGroups.toTag(cmds);
        npcTag.put("CommandGroups", cmds);

        npcTag.put("skin", writeSkinToTag(this.gameProfile));

        NbtList pathTargets = new NbtList();
        this.npcData.pathTargets.forEach(blockPos -> {
            NbtCompound pos = new NbtCompound();
            pos.putInt("x", blockPos.getX());
            pos.putInt("y", blockPos.getY());
            pos.putInt("z", blockPos.getZ());
            pathTargets.add(pos);
        });
        npcTag.put("PathTargets", pathTargets);

        // Messages
        NbtList messages = new NbtList();
        this.npcData.messages.forEach(pair -> {
            NbtCompound msg = new NbtCompound();
            msg.put("Message", TextUtil.toNbtElement(pair.getFirst()));
            msg.putInt("Delay", pair.getSecond());
            messages.add(msg);
        });
        npcTag.put("Messages", messages);

        npcTag.putInt("PermissionLevel", this.npcData.permissionLevel);
        npcTag.putString("Behaviour", this.npcData.behaviour.toString());

        // Profession initialising
        NbtList professions = new NbtList();
        this.professions.forEach((id, profession) -> {
            NbtCompound professionCompound = new NbtCompound();

            professionCompound.putString("ProfessionType", id.toString());

            NbtCompound professionData = new NbtCompound();
            profession.saveNbt(professionData);
            professionCompound.put("ProfessionData", professionData);

            professions.add(professionCompound);
        });
        npcTag.put("Professions", professions);

        NbtCompound followTag = new NbtCompound();
        followTag.putString("Type", this.npcData.follow.type.toString());

        if(this.npcData.follow.targetUuid != null)
            followTag.putUuid("UUID", this.npcData.follow.targetUuid);

        npcTag.put("Follow", followTag);

        npcTag.putString("Pose", this.getPose().toString());

        NbtCompound bodyRotations = new NbtCompound();
        //fixme rotations are not getting saved
        bodyRotations.putFloat("XRot", this.getPitch());
        bodyRotations.putFloat("YRot", this.getYaw());
        npcTag.put("BodyRotations", bodyRotations);

        // Locking
        if (this.lockedUuid != null)
            npcTag.putUuid("LockedBy", this.lockedUuid);

        npcTag.putLong("MinCommandInteractionTime", this.npcData.minCommandInteractionTime);

        tag.put("TaterzenNPCTag", npcTag);
    }

    /**
     * Loads Taterzen data from preset file.
     * @param tag tag containing preset name.
     */
    private void loadPresetTag(NbtCompound tag) {
        String preset = tag.getString("PresetOverride") + ".json";
        File presetFile = new File(Taterzens.getInstance().getPresetDirectory() + "/" + preset);

        if (presetFile.exists()) {
            this.loadFromPresetFile(presetFile, preset);
        }
    }

    /**
     * Loads Taterzen data from preset file. Loads team data as well.
     * @param presetFile file containing a taterzen preset.
     * @param presetName name of the preset.
     */
    public void loadFromPresetFile(File presetFile, String presetName) {
        NbtCompound saveTag = TaterzensAPI.loadPresetTag(presetFile);
        saveTag.putString("UUID", this.getUuidAsString());

        // Avoid looping if user has messed with preset
        if (!presetName.isEmpty() && presetName.equals(saveTag.getString("PresetOverride"))) {
            saveTag.remove("PresetOverride");
            LOGGER.warn("Preset override loop detected in {}. Aborting it.", presetName);
        }

        Vec3d savedPos = this.getLerpedPos(0);
        this.readNbt(saveTag);
        this.setPosition(savedPos);

        NbtCompound npcTag = (NbtCompound) saveTag.get("TaterzenNPCTag");
        if (npcTag != null) {
            // Team stuff
            String savedTeam = npcTag.getString("SavedTeam");
            Team team = this.getWorld().getScoreboard().getTeam(savedTeam);
            if (team != null) {
                this.getWorld().getScoreboard().addPlayerToTeam(this.getEntityName(), team);
            }
        }
    }

    /**
     * Sets player as equipment editor.
     * @param player player that will be marked as equipment editor.
     */
    public void setEquipmentEditor(@Nullable PlayerEntity player) {
        this.npcData.equipmentEditor = player;
    }

    /**
     * Sets player as equipment editor.
     * @param player player to check.
     * @return true if player is equipment editor of the NPC, otherwise false.
     */
    public boolean isEquipmentEditor(@NotNull PlayerEntity player) {
        return player.equals(this.npcData.equipmentEditor);
    }

    /**
     * Handles interaction (right clicking on the NPC).
     * @param player player interacting with NPC
     * @param hand player's interacting hand
     * @return {@link ActionResult#PASS} if NPC has a right click action, otherwise {@link ActionResult#FAIL}
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.world.isClient()) return ActionResult.PASS;

        ITaterzenPlayer ipl = (ITaterzenPlayer) player;
        long lastAction = ((ServerPlayerEntity) player).getLastActionTime();

        // As weird as it sounds, this gets triggered twice, first time with the item stack player is holding
        // then with "air" if fake type is player / armor stand
        if (lastAction - ipl.getLastInteractionTime() < 50)
            return ActionResult.FAIL;
        ipl.setLastInteraction(lastAction);


        for(TaterzenProfession profession : this.professions.values()) {
            ActionResult professionResult = profession.interactAt(player, player.getPos(), hand);
            if(professionResult != ActionResult.PASS)
                return professionResult;
        }

        if (this.isEquipmentEditor(player)) {
            ItemStack stack = player.getStackInHand(hand).copy();

            if (stack.isEmpty() && player.isSneaking()) {
                this.dropEquipment(this.getDamageSources().playerAttack(player), 1, this.isEquipmentDropsAllowed());
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    this.fakePlayer.equipStack(slot, ItemStack.EMPTY);
                }
            } else if (player.isSneaking()) {
                this.equipStack(EquipmentSlot.MAINHAND, stack);
                this.fakePlayer.equipStack(EquipmentSlot.MAINHAND, stack);
            } else {
                EquipmentSlot slot = getPreferredEquipmentSlot(stack);
                this.equipLootStack(slot, stack);
                this.fakePlayer.equipStack(slot, stack);
            }
            // Updating behaviour (if npc had a sword and now has a bow, it won't
            // be able to attack otherwise.)
            this.setBehaviour(this.npcData.behaviour);
            return ActionResult.PASS;
        } else if(
                player.getStackInHand(hand).getItem().equals(Items.POTATO) &&
                        player.isSneaking() &&
                        Taterzens.getInstance().getPlatform().checkPermission(player.getCommandSource(), "taterzens.npc.select", config.perms.npcCommandPermissionLevel)
        ) {
            // Select this taterzen
            ((ITaterzenEditor) player).selectNpc(this);

            player.sendMessage(successText("taterzens.command.select", this.getName().getString()));

            return ActionResult.PASS;
        } else if (((ITaterzenEditor) player).getNpc() == this) {
            // Opens GUI for editing
            Taterzens.getInstance().getPlatform().openEditorGui((ServerPlayerEntity) player);
        }

        // Limiting command usage
        if (this.npcData.minCommandInteractionTime != -1) {
            long now = System.currentTimeMillis();
            long diff = (now - this.commandTimes.getOrDefault(player.getUuid(), 0L)) / 1000;

            if (diff > this.npcData.minCommandInteractionTime || this.npcData.minCommandInteractionTime == 0) {
                this.commandTimes.put(player.getUuid(), now);
                this.commandGroups.execute((ServerPlayerEntity) player);
            } else {
                // Inform player about the cooldown
                player.sendMessage(
                        errorText(this.npcData.commandCooldownMessage,
                                String.valueOf(this.npcData.minCommandInteractionTime - diff)));
            }
        }

        return this.interact(player, hand);
    }


    /**
     * Sets the cooldown message.
     * @param message new cooldown message.
     */
    public void setCooldownMessage(String message) {
        this.npcData.commandCooldownMessage = message;
    }

    /**
     * Sets the minimum time between command usage.
     * @param time new minimum time.
     */
    public void setMinCommandInteractionTime(long time) {
        this.npcData.minCommandInteractionTime = time;
    }


    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        // Additional drop check
        if (this.isEquipmentDropsAllowed())
            super.dropEquipment(source, lootingMultiplier, allowDrops);
        else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.equipStack(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return this.isEquipmentDropsAllowed();
    }

    @Override
    public boolean shouldDropXp() {
        return this.isEquipmentDropsAllowed();
    }

    /**
     * Adds the message to taterzen's message list.
     * @param text message to add
     */
    public void addMessage(Text text) {
        this.addMessage(text, config.messages.messageDelay);
    }

    /**
     * Adds the message to taterzen's message list.
     * @param text message to add
     * @param delay message delay, in ticks
     */
    public void addMessage(Text text, int delay) {
        this.npcData.messages.add(new Pair<>(text, delay));
    }

    /**
     * Edits the message from taterzen's message list at index.
     * @param index index of the message to edit
     * @param text new text message
     */
    public void editMessage(int index, Text text) {
        if(index >= 0 && index < this.npcData.messages.size())
            this.npcData.messages.set(index, new Pair<>(text, config.messages.messageDelay));
    }

    /**
     * Removes message at index.
     * @param index index of message to be removed.
     * @return removed message
     */
    public Text removeMessage(int index) {
        if(index < this.npcData.messages.size())
            return this.npcData.messages.remove(index).getFirst();
        return Text.literal("");
    }

    /**
     * Sets message delay for specified message.
     * E.g. if you want to set delay for message at index 2 (you want 3rd message to appear right after second),
     * you'd set delay for index 2 to zero.
     *
     * @param index index of the message to change delay for.
     * @param delay new delay.
     */
    public void setMessageDelay(int index, int delay) {
        if(index < this.npcData.messages.size()) {
            Pair<Text, Integer> newMsg = this.npcData.messages.get(index).mapSecond(previous -> delay);
            this.npcData.messages.set(index, newMsg);
        }
    }

    public void clearMessages() {
        this.npcData.messages.clear();
    }

    /**
     * Gets {@link ArrayList} of {@link Pair}s of messages and their delays.
     * @return arraylist of pairs with texts and delays.
     */
    public ArrayList<Pair<Text, Integer>> getMessages() {
        return this.npcData.messages;
    }

    /**
     * Used for disabling pushing
     * @param entity colliding entity
     */
    @Override
    public void pushAwayFrom(Entity entity) {
        if (this.getTag("Pushable", config.defaults.pushable)) {
            super.pushAwayFrom(entity);
        }
    }

    /**
     * Used for disabling pushing
     * @param entity colliding entity
     */
    @Override
    protected void pushAway(Entity entity) {
        if (this.getTag("Pushable", config.defaults.pushable)) {
            super.pushAway(entity);
        }
    }

    /**
     * Sets the pushable flag
     * @param pushable whether Taterzen can be pushed
     */
    public void setPushable(boolean pushable) {
        this.setTag("Pushable", pushable);
    }

    /**
     * Handles received hits.
     *
     * @param attacker entity that attacked NPC.
     * @return true if attack should be cancelled.
     */
    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity pl && this.isEquipmentEditor(pl)) {
            ItemStack main = this.getMainHandStack();
            this.setStackInHand(MAIN_HAND, this.getOffHandStack());
            this.setStackInHand(Hand.OFF_HAND, main);
            return true;
        }
        for(TaterzenProfession profession : this.professions.values()) {
            if (profession.handleAttack(attacker)) {
                return true;
            }
        }
        return this.isInvulnerable();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return this.isRemoved() || this.isInvulnerable() && !damageSource.isOf(DamageTypes.OUT_OF_WORLD);
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return !this.isLeashed() && this.isLeashable();
    }

    /**
     * Gets whether this NPC is leashable.
     *
     * @return whether this NPC is leashable.
     */
    private boolean isLeashable() {
        return this.getTag("Leashable", config.defaults.leashable);
    }

    /**
     * Sets whether Taterzen can be leashed.
     *
     * @param leashable Taterzen leashability.
     */
    public void setLeashable(boolean leashable) {
        this.setTag("Leashable", leashable);
    }

    @Override
    public void attachLeash(Entity entityIn, boolean sendAttachNotification) {
        super.attachLeash(entityIn, sendAttachNotification);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new LongDoorInteractGoal(this, true));
    }

    /**
     * Handles death of NPC.
     * @param source damage source responsible for death.
     */
    @Override
    public void onDeath(DamageSource source) {
        EntityPose pose = this.getPose();
        super.onDeath(source);
        if (this.respawnPosition != null) {
            // Taterzen should be respawned instead
            this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
            this.dead = false;
            this.setHealth(this.getMaxHealth());
            this.setVelocity(0.0D, 0.1D, 0.0D);
            this.setPosition(this.respawnPosition);
            this.setPose(pose);
        } else {
            TATERZEN_NPCS.remove(this.getUuid());

            for(TaterzenProfession profession : this.professions.values()) {
                profession.onRemove();
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        TATERZEN_NPCS.remove(this.getUuid());

        for(TaterzenProfession profession : this.professions.values()) {
            profession.onRemove();
        }
    }

    /**
     * Sets Taterzen's {@link NPCData.Behaviour}.
     * @param level behaviour level
     */
    public void setBehaviour(NPCData.Behaviour level) {
        this.npcData.behaviour = level;

        this.goalSelector.remove(reachMeleeAttackGoal);
        this.goalSelector.remove(projectileAttackGoal);
        this.goalSelector.remove(attackMonstersGoal);

        this.targetSelector.remove(followTargetGoal);
        this.targetSelector.remove(revengeGoal);
        this.targetSelector.remove(followMonstersGoal);

        for(TaterzenProfession profession : this.professions.values()) {
            profession.onBehaviourSet(level);
        }

        switch (level) {
            case DEFENSIVE -> {
                this.targetSelector.add(2, revengeGoal);
                this.setAttackGoal();
            }
            case FRIENDLY -> {
                this.targetSelector.add(2, revengeGoal);
                this.targetSelector.add(3, followMonstersGoal);
                this.goalSelector.add(3, attackMonstersGoal);
                this.setAttackGoal();
            }
            case HOSTILE -> {
                this.targetSelector.add(2, revengeGoal);
                this.targetSelector.add(3, followTargetGoal);
                this.setAttackGoal();
            }
            default -> {
            }
        }
    }

    /**
     * Sets proper attack goal, based on hand item stack.
     */
    private void setAttackGoal() {
        ItemStack mainHandStack = this.getMainHandStack();
        ItemStack offHandStack = this.getOffHandStack();

        if(mainHandStack.getItem() instanceof RangedWeaponItem || offHandStack.getItem() instanceof RangedWeaponItem) {
            this.goalSelector.add(3, projectileAttackGoal);
        } else {
            this.goalSelector.add(3, reachMeleeAttackGoal);
        }
    }

    /**
     * Gets the Taterzen's target selector.
     * @return target selector of Taterzen.
     */
    public GoalSelector getTargetSelector() {
        return this.targetSelector;
    }

    /**
     * Gets the Taterzen's goal selector.
     * @return goal selector of Taterzen.
     */
    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    @Override
    public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return this.npcData.behaviour != NPCData.Behaviour.PASSIVE;
    }

    @Override
    public void setCharging(boolean charging) {
    }

    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        var weaponHand = ProjectileUtil.getHandPossiblyHolding(this, Items.CROSSBOW);
        this.fakePlayer.setCurrentHand(weaponHand);
        this.setCurrentHand(weaponHand);
        // Crossbow attack
        this.shootProjectile(target, projectile, multiShotSpray);
    }

    @Override
    public void postShoot() {
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        for (TaterzenProfession profession : this.professions.values()) {
            if (profession.cancelRangedAttack(target))
                return;
        }

        // Ranged attack
        var weaponHand = ProjectileUtil.getHandPossiblyHolding(this, Items.BOW);
        var bow = this.getStackInHand(weaponHand);
        ItemStack arrowType = this.getProjectileType(bow);
        if (arrowType.isEmpty())
            arrowType = this.getProjectileType(this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.CROSSBOW)));

        PersistentProjectileEntity projectile = ProjectileUtil.createArrowProjectile(this, arrowType.copy(), pullProgress);

        //bow.use(this.level, this.fakePlayer, weaponHand);
        this.fakePlayer.setCurrentHand(weaponHand);
        this.setCurrentHand(weaponHand);
        this.shootProjectile(target, projectile, 0.0F);
    }

    private void shootProjectile(LivingEntity target, ProjectileEntity projectile, float multishotSpray) {
        double deltaX = target.getX() - this.getX();
        double y = target.getBodyY(0.3333333333333333D) - projectile.getY();
        double deltaZ = target.getZ() - this.getZ();
        double planeDistance = MathHelper.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));
        Vector3f launchVelocity = this.getProjectileLaunchVelocity(this, new Vec3d(deltaX, y + planeDistance * 0.2D, deltaZ), multishotSpray);

        projectile.setVelocity(launchVelocity.x(), launchVelocity.y(), launchVelocity.z(), 1.6F, 0);

        this.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 0.125F);
        this.world.spawnEntity(projectile);
    }

    @Override
    public boolean tryAttack(Entity target) {
        for(TaterzenProfession profession : this.professions.values()) {
            if(profession.cancelMeleeAttack(target))
                return false;
        }
        return super.tryAttack(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if(!this.npcData.allowSounds || this.npcData.ambientSounds.isEmpty())
            return null;

        int rnd = this.random.nextInt(this.npcData.ambientSounds.size());
        Identifier sound = new Identifier(this.npcData.ambientSounds.get(rnd));

        return Registries.SOUND_EVENT.get(sound);
    }

    public ArrayList<String> getAmbientSoundData() {
        return this.npcData.ambientSounds;
    }

    public void setAmbientSoundData(ArrayList<String> ambientSounds) {
        this.npcData.ambientSounds = ambientSounds;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if(!this.npcData.allowSounds || this.npcData.hurtSounds.isEmpty())
            return null;

        int rnd = this.random.nextInt(this.npcData.hurtSounds.size());
        Identifier sound = new Identifier(this.npcData.hurtSounds.get(rnd));

        return Registries.SOUND_EVENT.get(sound);
    }

    public ArrayList<String> getHurtSoundData() {
        return this.npcData.hurtSounds;
    }

    public void setHurtSoundData(ArrayList<String> hurtSounds) {
        this.npcData.hurtSounds = hurtSounds;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if(!this.npcData.allowSounds || this.npcData.deathSounds.isEmpty())
            return null;

        int rnd = this.random.nextInt(this.npcData.deathSounds.size());
        Identifier sound = new Identifier(this.npcData.deathSounds.get(rnd));

        return Registries.SOUND_EVENT.get(sound);
    }

    public ArrayList<String> getDeathSoundData() {
        return this.npcData.deathSounds;
    }

    public void setDeathSoundData(ArrayList<String> deathSounds) {
        this.npcData.deathSounds = deathSounds;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return 0.0F;
    }

    @Override
    protected Text getDefaultName() {
        return Text.literal("-" + config.defaults.name + "-");
    }

    public PlayerEntity getFakePlayer() {
        return this.fakePlayer;
    }

    /**
     * Toggles whether Taterzen will drop its equipment.
     *
     * @param drop drop rule
     */
    public void allowEquipmentDrops(boolean drop) {
        this.setTag("DropsAllowed", drop);
    }

    /**
     * Gets whether Taterzen will drop its equipment.
     *
     * @return drop rule
     */
    public boolean isEquipmentDropsAllowed() {
        return this.getTag("DropsAllowed", config.defaults.dropEquipment);
    }

    /**
     * Adds {@link TaterzenProfession} to Taterzen.
     * Profession must be registered with {@link org.samo_lego.taterzens.api.TaterzensAPI#registerProfession(Identifier, Function)}.
     *
     * @param professionId ResourceLocation of the profession
     */
    public void addProfession(Identifier professionId) {
        if (PROFESSION_TYPES.containsKey(professionId)) {
            this.addProfession(professionId, PROFESSION_TYPES.get(professionId).apply(this));
        } else {
            Taterzens.LOGGER.warn("Trying to add unknown profession {} to taterzen {}.", professionId, this.getName().getString());
        }
    }

    /**
     * Adds {@link TaterzenProfession} to Taterzen.
     * @param professionId ResourceLocation of the profession
     * @param profession profession object (implementing {@link TaterzenProfession})
     */
    public void addProfession(Identifier professionId, TaterzenProfession profession) {
        this.professions.put(professionId, profession);
    }

    /**
     * Gets taterzen's professions.
     *
     * @return all professions ids of taterzen's professions.
     */
    public Collection<Identifier> getProfessionIds() {
        return this.professions.keySet();
    }

    /**
     * Removes Taterzen's profession and triggers the corresponding {@link TaterzenProfession#onRemove()} event.
     * @param professionId id of the profession that is in Taterzen's profession map.
     */
    public void removeProfession(Identifier professionId) {
        TaterzenProfession toRemove = this.professions.get(professionId);

        if (toRemove != null) {
            toRemove.onProfessionRemoved();
            this.professions.remove(professionId);
        }
    }

    /**
     * Gets Taterzen's profession.
     * @param professionId id of the profession that is in Taterzen's profession map.
     */
    @Nullable
    public TaterzenProfession getProfession(Identifier professionId) {
        return this.professions.get(professionId);
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    /**
     * Manages item pickup.
     * @param item item to pick up.
     */
    @Override
    protected void loot(ItemEntity item) {
        // Profession event
        ItemStack stack = item.getStack();
        for (TaterzenProfession profession : this.professions.values()) {
            if (profession.tryPickupItem(item)) {
                this.triggerItemPickedUpByEntityCriteria(item);
                this.sendPickup(item, stack.getCount());
                stack.setCount(0);
                item.discard();
                return;
            }
        }
    }

    /**
     * Makes Taterzen interact with block at given position.
     * It doesn't work if given position is too far away (>4 blocks)
     * @param pos position of block to interact with.
     * @return true if interaction was successfull, otherwise false.
     */
    public boolean interact(BlockPos pos) {
        if(this.getPos().distanceTo(Vec3d.ofCenter(pos)) < 4.0D && !this.world.isClient()) {
            this.lookAt(pos);
            this.swingHand(MAIN_HAND);
            this.world.getBlockState(pos).onUse(this.world, this.fakePlayer, MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false));
            this.getMainHandStack().use(this.world, this.fakePlayer, MAIN_HAND);
            return true;
        }
        return false;
    }

    /**
     * Makes Taterzen look at given block position.
     * @param target target block to look at.
     */
    public void lookAt(BlockPos target) {
        Vec3d vec3d = this.getPos();
        double d = target.getX() - vec3d.x;
        double e = target.getY() - vec3d.y;
        double f = target.getZ() - vec3d.z;
        double g = Math.sqrt(d * d + f * f);
        this.setPitch(MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875D))));
        this.setBodyYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F));
        this.setHeadYaw(this.getHeadYaw());
    }

    /**
     * Sets whether Taterzen can perform jumps when in
     * proximity of target that it is attacking.
     * @param jumpWhileAttacking whether to jump during attacks.
     */
    public void setPerformAttackJumps(boolean jumpWhileAttacking) {
        this.setTag("JumpAttack", jumpWhileAttacking);
    }

    /**
     * Sets the target type to follow.
     * Changes movement to {@link NPCData.Movement#PATH} as well.
     * @param followType type of target to follow
     */
    public void setFollowType(NPCData.FollowTypes followType) {
        if (followType != NPCData.FollowTypes.NONE) {
            this.setMovement(NPCData.Movement.TICK);
        }
        this.npcData.follow.type = followType;

        switch (followType) {
            case MOBS -> this.goalSelector.add(4, trackLivingGoal);
            case PLAYERS -> this.goalSelector.add(4, trackPlayersGoal);
            case UUID -> this.goalSelector.add(4, trackUuidGoal);
            default -> {
            }
        }
    }

    /**
     * Gets follow type for taterzen.
     * @return follow type
     */
    public NPCData.FollowTypes getFollowType() {
        return this.npcData.follow.type;
    }

    /**
     * Sets the target uuid to follow.
     * @param followUuid uuid of target to follow
     */
    public void setFollowUuid(@Nullable UUID followUuid) {
        this.npcData.follow.targetUuid = followUuid;
    }

    /**
     * Gets the UUID of the entity that taterzen is following.
     * @return entity UUID if following, otherwise null.
     */
    @Nullable
    public UUID getFollowUuid() {
        return this.npcData.follow.targetUuid;
    }

    /**
     * Whether this Taterzen should make sound.
     * @param allowSounds whether to allow sounds or not.
     */
    public void setAllowSounds(boolean allowSounds) {
        this.npcData.allowSounds = allowSounds;
    }

    /**
     * Sets which skin layers should be shown to clients
     * @param skinLayers byte of skin layers, see wiki.wg for more info.
     */
    public void setSkinLayers(Byte skinLayers) {
        this.fakePlayer.getDataTracker().set(getPLAYER_MODE_CUSTOMISATION(), skinLayers);
    }



    /**
     * Sets the respawn position for taterzen. Can be null to disable respawning.
     * @param respawnPos new respawn position.
     */
    public void setRespawnPos(@Nullable Vec3d respawnPos) {
        this.respawnPosition = respawnPos;
    }

    /**
     * Sets whether taterzen should be able to fly.
     * @param allowFlight whether to allow taterzen to fly or not.
     */
    public void setAllowFlight(boolean allowFlight) {
        this.setTag("AllowFlight", allowFlight);

        if (allowFlight) {
            this.moveControl = new FlightMoveControl(this, 20, false);
            this.navigation = new BirdNavigation(this, world);
            this.getNavigation().setCanSwim(true);
        } else {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, world);
            ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        }
    }

    /**
     * Whether taterzen can take fall damage.
     * @param fallDistance fall distance.
     * @param multiplier damage multiplier.
     * @param source source of damage.
     * @return true if damage should be taken, otherwise false.
     */
    @Override
    public boolean handleFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return !this.getTag("AllowFlight", config.defaults.allowFlight) && super.handleFallDamage(fallDistance, multiplier, source);
    }

    /**
     * Whether taterzen should be allowed to be edited by entity.
     * @param entity entity to check.
     * @return true if taterzen can be edited by entity, otherwise false.
     */
    public boolean allowEditBy(Entity entity) {
        return this.allowEditBy(entity.getUuid());
    }

    /**
     * Whether taterzen should be allowed to be edited by provided uuid.
     * @param uuid uuid to check.
     * @return true if taterzen can be edited by provided uuid, otherwise false.
     */
    public boolean allowEditBy(UUID uuid) {
        return this.lockedUuid == null || this.lockedUuid.equals(uuid) || this.getUuid().equals(uuid);
    }

    /**
     * Tries to make taterzen to ride provided entity.
     * @param entity entity to ride.
     * @return true if taterzen was able to ride provided entity, otherwise false.
     */
    public boolean startRiding(Entity entity) {
        if (this.getTag("AllowRiding", config.defaults.allowRiding)) {
            return this.startRiding(entity, false);
        }
        return false;
    }

    /**
     * Whether taterzen is locked.
     * @return true if taterzen is locked, otherwise false.
     */
    public boolean isLocked() {
        return this.lockedUuid != null;
    }

    /**
     * Sets taterzen to be locked by provided owner's uuid.
     * @param owner entity to lock taterzen to.
     */
    public void setLocked(Entity owner) {
        this.setLocked(owner.getUuid());
    }

    /**
     * Sets taterzen to be locked by provided uuid.
     * @param uuid uuid to lock taterzen to.
     */
    public void setLocked(UUID uuid) {
        this.lockedUuid = uuid;
    }

    /**
     * Sets whether taterzen should be allowed to be ride entities.
     * (Mainly used for preventing them being picked up by boats / minecarts.)
     * @param allow whether to allow riding or not.
     */
    public void setAllowRiding(boolean allow) {
        this.setTag("AllowRiding", allow);
    }


    @Override
    public boolean canTarget(LivingEntity target) {
        return (!(target instanceof PlayerEntity) || (this.world.getDifficulty() != Difficulty.PEACEFUL || config.combatInPeaceful)) && target.canTakeDamage();
    }

    public void setAllowSwimming(boolean allowSwimming) {
        this.goalSelector.remove(this.swimGoal);
        if (allowSwimming) {
            this.goalSelector.add(0, this.swimGoal);
        }
        this.setSwimming(this.isSwimming() && allowSwimming);
        this.getNavigation().setCanSwim(allowSwimming);
        this.setTag("AllowSwimming", allowSwimming);
    }

    private void setTag(String name, boolean value) {
        this.npcData.booleanTags.put(name, value);
    }

    private void resetTag(String name) {
        this.npcData.booleanTags.remove(name);
    }

    private boolean getTag(String name, boolean defaultValue) {
        if (this.npcData.booleanTags.containsKey(name))
            return this.npcData.booleanTags.get(name);
        return defaultValue;
    }
}
