package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base of every cheap golem: integrity (spec §5.4), home (§5.7) and crumbling (§5.6).
 * Knows nothing about specific golems; subclasses add goals and environmental triggers.
 */
public abstract class AbstractCheapGolem extends AbstractGolem {
    private static final EntityDataAccessor<Integer> DATA_INTEGRITY =
        SynchedEntityData.defineId(AbstractCheapGolem.class, EntityDataSerializers.INT);

    private static final String TAG_INTEGRITY = "Integrity";
    private static final String TAG_INTEGRITY_TICKS = "IntegrityTicks";
    private static final String TAG_HOME_POS = "HomePos";

    private Integrity integrity = new Integrity();
    private @Nullable BlockPos homePos;
    private int homeRadiusOverride = -1;

    protected AbstractCheapGolem(EntityType<? extends AbstractCheapGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createBaseAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_INTEGRITY, Integrity.MAX);
    }

    // ---- integrity ----

    public int getIntegrity() {
        return this.entityData.get(DATA_INTEGRITY);
    }

    public CrackStage getCrackStage() {
        return CrackStage.forIntegrity(getIntegrity());
    }

    /** Lowers integrity; crumbles in the same tick when it reaches 0. Server only. */
    public void consumeIntegrity(int amount) {
        if (this.level().isClientSide() || this.isRemoved()) return;
        integrity.consume(amount);
        syncIntegrity();
    }

    private void syncIntegrity() {
        this.entityData.set(DATA_INTEGRITY, integrity.value());
        if (integrity.isDepleted() && !this.isRemoved())
            crumble();
    }

    // ---- home ----

    public @Nullable BlockPos getHomePos() {
        return homePos;
    }

    /** Sets the home and restricts the golem to {@link #effectiveHomeRadius()} around it. */
    public void setHomePos(BlockPos pos) {
        this.homePos = pos.immutable();
        this.setHomeTo(this.homePos, effectiveHomeRadius());
    }

    public int effectiveHomeRadius() {
        return homeRadiusOverride > 0 ? homeRadiusOverride : GolemConfig.homeRadius();
    }

    /** Test hook: overrides {@link GolemConfig#homeRadius()} for this golem only. Not persisted. */
    public void setHomeRadiusOverride(int radius) {
        this.homeRadiusOverride = radius;
        if (homePos != null) this.setHomeTo(homePos, effectiveHomeRadius());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        var result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        if (homePos == null) setHomePos(this.blockPosition());
        return result;
    }

    // ---- ticking ----

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel) || this.isRemoved()) return;

        if (homePos == null) setHomePos(this.blockPosition());

        environmentTick();
        if (this.isRemoved()) return;

        if (integrity.tickPassive(GolemConfig.passiveDecayTicks()))
            syncIntegrity();
    }

    /** Environmental triggers of the golem. Called every server tick, before passive decay. */
    protected void environmentTick() {}

    // ---- crumbling ----

    protected abstract BlockState getMaterialState();

    protected abstract ItemStack getCrumbleDrop();

    protected abstract double getCrumbleDropChance();

    private @Nullable Double crumbleDropChanceOverride;

    /** Called after activation with the player who used the core. */
    protected void onCreatedBy(Player player) {}

    /** Called after activation with the material of the bottom pillar block (captured before removal). */
    protected void onCreatedFrom(BlockState bottom) {}

    /** Activation entry point (GolemActivation lives in another package): runs both creation hooks in order. */
    public final void notifyCreated(BlockState bottom, Player player) {
        onCreatedFrom(bottom);
        onCreatedBy(player);
    }

    /** Sneak-click with a Golem Core (spec v2 section 3.2). Base golems ignore it. */
    public InteractionResult receiveCommand(Player player, ItemStack core) {
        return InteractionResult.PASS;
    }

    /** Test hook: when non-null, replaces {@link #getCrumbleDropChance()} in {@link #crumble()}. */
    public void setCrumbleDropChanceOverride(@Nullable Double chance) {
        this.crumbleDropChanceOverride = chance;
    }

    /** Particles and sound of crumbling; defaults to the material's block particles and break sound. */
    protected void playCrumbleEffects(ServerLevel level) {
        var state = getMaterialState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
            this.getX(), this.getY(0.5), this.getZ(), 30, 0.25, 0.3, 0.25, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            state.getSoundType().getBreakSound(), SoundSource.NEUTRAL, 1.0F, 0.9F);
    }

    /** Crumble effects, chance-based drop, then removal. No XP. */
    protected void crumble() {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return;
        playCrumbleEffects(level);
        if (this.getRandom().nextDouble() < (crumbleDropChanceOverride != null ? crumbleDropChanceOverride : getCrumbleDropChance()))
            this.spawnAtLocation(level, getCrumbleDrop());
        this.discard();
    }

    /** Death by health 0 crumbles instead of the vanilla death animation and loot. */
    @Override
    public void die(DamageSource source) {
        if (net.minecraftforge.event.ForgeEventFactory.onLivingDeath(this, source)) return;
        if (this.isRemoved() || this.dead) return;
        this.handleKillingBlow();
        crumble();
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    // ---- persistence ----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt(TAG_INTEGRITY, integrity.value());
        output.putInt(TAG_INTEGRITY_TICKS, integrity.ticksSinceDecay());
        if (homePos != null) output.store(TAG_HOME_POS, BlockPos.CODEC, homePos);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        integrity = new Integrity(input.getIntOr(TAG_INTEGRITY, Integrity.MAX));
        integrity.setTicksSinceDecay(input.getIntOr(TAG_INTEGRITY_TICKS, 0));
        this.entityData.set(DATA_INTEGRITY, integrity.value());
        input.read(TAG_HOME_POS, BlockPos.CODEC).ifPresent(this::setHomePos);
    }
}
