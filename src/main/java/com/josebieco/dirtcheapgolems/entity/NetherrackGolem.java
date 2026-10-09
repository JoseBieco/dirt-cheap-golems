package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.KamikazeChargeGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** "Kamikaze Térmico" (spec §6.3): a walking light that blows itself up against hostiles. */
public class NetherrackGolem extends AbstractCheapGolem {
    private static final EntityDataAccessor<Integer> DATA_FUSE =
        SynchedEntityData.defineId(NetherrackGolem.class, EntityDataSerializers.INT);

    private final WalkingLight walkingLight = new WalkingLight();
    /** Light saved in NBT by a previous life of this golem; removed before any new light is placed. */
    private @Nullable BlockPos staleLight;

    public NetherrackGolem(EntityType<? extends NetherrackGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Follow range is the upper bound for targeting; the configured kamikaze range is enforced by the target selector.
        return createBaseAttributes().add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new KamikazeChargeGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
            (target, level) -> target instanceof Enemy && this.distanceTo(target) <= GolemConfig.netherrackKamikazeRange()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FUSE, 0);
    }

    // ---- fuse (synced so clients can show sparks) ----

    public int getFuseTicks() {
        return this.entityData.get(DATA_FUSE);
    }

    public void startFuse() {
        if (getFuseTicks() > 0) return;
        this.entityData.set(DATA_FUSE, 1);
        this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.6F);
    }

    public void advanceFuse() {
        if (getFuseTicks() > 0) this.entityData.set(DATA_FUSE, getFuseTicks() + 1);
    }

    public void cancelFuse() {
        this.entityData.set(DATA_FUSE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (getFuseTicks() > 0) {
                this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.5), this.getY(0.8), this.getRandomZ(0.5), 0.0, 0.03, 0.0);
                this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.5), this.getY(1.0), this.getRandomZ(0.5), 0.0, 0.05, 0.0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return;
        if (staleLight != null) {
            WalkingLight.removeLightAt(level, staleLight);
            staleLight = null;
        }
        walkingLight.update(level, this.blockPosition().above(), GolemConfig.netherrackLightLevel());
    }

    /** Water or rain (spec §6.3, D7): instantly becomes stone at its feet, without drops. */
    @Override
    protected void environmentTick() {
        if (!(this.level() instanceof ServerLevel level)) return;
        if (!this.isInWater() && !level.isRainingAt(this.blockPosition().above())) return;

        walkingLight.clear(level);
        var feet = this.blockPosition();
        var state = level.getBlockState(feet);
        if (!state.isAir() && !state.canBeReplaced()) {
            crumble();
            return;
        }
        level.setBlock(feet, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(0.5), this.getZ(), 20, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, feet, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        this.discard();
    }

    /** Every removal path (death, discard, chunk unload, dimension change) takes the light away. */
    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (!this.level().isClientSide()) walkingLight.clear(this.level());
    }

    /** Crumbles into ashes: smoke instead of material particles, drops charcoal. */
    @Override
    protected void crumble() {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return;
        walkingLight.clear(level);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 20, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6F, 1.2F);
        if (this.getRandom().nextDouble() < getCrumbleDropChance())
            this.spawnAtLocation(level, getCrumbleDrop());
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        walkingLight.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        staleLight = WalkingLight.load(input);
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.NETHERRACK.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.CHARCOAL);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.netherrackDropChance();
    }
}
