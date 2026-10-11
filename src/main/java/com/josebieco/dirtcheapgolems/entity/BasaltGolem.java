package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.FollowOwnerShieldGoal;
import com.josebieco.dirtcheapgolems.entity.ai.InterceptThreatGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * "Escudo" (spec v2 6.3): never attacks, protects its owner (FOLLOW) or everyone near its home (GUARD).
 * The projectile magnet and the follow/intercept goals are added on top of this class.
 */
public class BasaltGolem extends AbstractCheapGolem {
    private static final EntityDataAccessor<Integer> DATA_MODE =
        SynchedEntityData.defineId(BasaltGolem.class, EntityDataSerializers.INT);

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_MODE = "Mode";
    private static final String TAG_SOURCE_BLOCK = "SourceBlock";

    private @Nullable UUID ownerId;
    private Block sourceBlock = Blocks.BASALT;

    public BasaltGolem(EntityType<? extends BasaltGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_MODE, ShieldMode.GUARD.ordinal());
    }

    // ---- goals, protected players and threat interception ----

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Following golems wait beside their owner instead of wandering off.
        this.goalSelector.removeAllGoals(goal -> goal instanceof WaterAvoidingRandomStrollGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return BasaltGolem.this.getMode() == ShieldMode.GUARD && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new InterceptThreatGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerShieldGoal(this));
    }

    /** FOLLOW: the owner if present. GUARD: every player within the home radius of the home. */
    public List<Player> protectedPlayers() {
        if (getMode() == ShieldMode.FOLLOW) {
            Player owner = getOwner();
            return owner == null ? List.of() : List.of(owner);
        }
        BlockPos home = getHomePos();
        if (home == null) return List.of();
        double radius = effectiveHomeRadius();
        var center = Vec3.atCenterOf(home);
        List<Player> result = new ArrayList<>();
        for (Player player : this.level().players())
            if (!player.isSpectator() && player.position().distanceToSqr(center) <= radius * radius) result.add(player);
        return result;
    }

    private static boolean isRangedThreat(Mob mob) {
        return mob instanceof RangedAttackMob || mob instanceof Ghast || mob instanceof Blaze;
    }

    /** The ranged attacker in threat range whose target is protected and nearest to it, else null. */
    public @Nullable Mob nearestThreat() {
        List<Player> protectedPlayers = protectedPlayers();
        if (protectedPlayers.isEmpty()) return null;
        double range = GolemConfig.basaltThreatRange();
        Mob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(range))) {
            if (!isRangedThreat(mob) || mob.isRemoved() || this.distanceTo(mob) > range) continue;
            if (!(mob.getTarget() instanceof Player target) || !protectedPlayers.contains(target)) continue;
            double distance = mob.distanceToSqr(target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = mob;
            }
        }
        return best;
    }

    /** 1.5 blocks from the threatened player toward the threat (clamped to the home radius in GUARD), or null. */
    public @Nullable Vec3 interceptPoint() {
        Mob threat = nearestThreat();
        if (threat == null || !(threat.getTarget() instanceof Player target)) return null;
        double dx = threat.getX() - target.getX();
        double dz = threat.getZ() - target.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) return null;
        double x = target.getX() + 1.5 * dx / length;
        double z = target.getZ() + 1.5 * dz / length;
        if (getMode() == ShieldMode.GUARD && getHomePos() != null) {
            var home = Vec3.atCenterOf(getHomePos());
            double radius = effectiveHomeRadius();
            double hx = x - home.x;
            double hz = z - home.z;
            double distance = Math.sqrt(hx * hx + hz * hz);
            if (distance > radius) {
                x = home.x + hx * radius / distance;
                z = home.z + hz * radius / distance;
            }
        }
        return new Vec3(x, target.getY(), z);
    }

    // ---- owner and mode ----

    public @Nullable UUID getOwnerId() {
        return ownerId;
    }

    /** The owner if online and in this golem's level, else null (the id is kept either way). */
    public @Nullable Player getOwner() {
        return ownerId == null ? null : this.level().getPlayerByUUID(ownerId);
    }

    public ShieldMode getMode() {
        var modes = ShieldMode.values();
        int ordinal = this.entityData.get(DATA_MODE);
        return modes[Math.max(0, Math.min(ordinal, modes.length - 1))];
    }

    /** Sets the mode: following lifts the home restriction, guarding reapplies it. */
    public void setMode(ShieldMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
        var home = getHomePos();
        if (mode == ShieldMode.FOLLOW) this.clearHome();
        else if (home != null) this.setHomeTo(home, effectiveHomeRadius());
    }

    @Override
    public void setHomePos(BlockPos pos) {
        super.setHomePos(pos);
        if (getMode() == ShieldMode.FOLLOW) this.clearHome();
    }

    public BlockState getSourceBlock() {
        return sourceBlock.defaultBlockState();
    }

    @Override
    protected void onCreatedBy(Player player) {
        this.ownerId = player.getUUID();
        setMode(ShieldMode.FOLLOW);
    }

    @Override
    protected void onCreatedFrom(BlockState bottom) {
        this.sourceBlock = bottom.getBlock();
    }

    @Override
    public InteractionResult receiveCommand(Player player, ItemStack core) {
        if (ownerId == null) return InteractionResult.PASS;
        if (!ownerId.equals(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.dirtcheapgolems.basalt.not_yours"));
            return InteractionResult.SUCCESS;
        }
        ShieldMode next;
        if (getMode() == ShieldMode.FOLLOW) {
            setHomePos(this.blockPosition());
            next = ShieldMode.GUARD;
        } else {
            // Following needs an owner to follow: online and in this dimension.
            next = getOwner() != null ? ShieldMode.FOLLOW : ShieldMode.GUARD;
        }
        setMode(next);
        player.sendOverlayMessage(Component.translatable(
            "message.dirtcheapgolems.basalt.mode." + next.name().toLowerCase(Locale.ROOT)));
        return InteractionResult.SUCCESS;
    }

    // ---- projectile magnet and melee hits ----

    /** Integrity cost of absorbing the projectile (arrows include tridents). */
    public int absorbCost(Projectile projectile) {
        if (projectile instanceof AbstractArrow) return GolemConfig.basaltArrowCost();
        if (projectile instanceof SmallFireball) return GolemConfig.basaltSmallFireballCost();
        if (projectile instanceof LargeFireball) return GolemConfig.basaltLargeFireballCost();
        return GolemConfig.basaltOtherProjectileCost();
    }

    /** Absorbs hostile projectiles in range: discarded without onHit, so fireballs never explode. */
    @Override
    protected void environmentTick() {
        if (!(this.level() instanceof ServerLevel level)) return;
        double radius = GolemConfig.basaltMagnetRadius();
        var center = this.position().add(0.0, this.getBbHeight() / 2.0, 0.0);
        var area = this.getBoundingBox().inflate(radius);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
            if (projectile.isRemoved()) continue;
            // Arrows/tridents stuck in a block are inert (isPushedByFluid is !isInGround for them).
            if (projectile instanceof AbstractArrow arrow && !arrow.isPushedByFluid()) continue;
            if (!(projectile.getOwner() instanceof Mob owner) || !(owner instanceof Enemy)) continue;
            if (projectile.position().distanceToSqr(center) > radius * radius) continue;
            var pos = projectile.position();
            projectile.discard();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getSourceBlock()),
                pos.x, pos.y, pos.z, 8, 0.15, 0.15, 0.15, 0.05);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 0.8F, 1.0F);
            consumeIntegrity(absorbCost(projectile));
            if (this.isRemoved()) return;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity && !source.is(DamageTypeTags.IS_PROJECTILE))
            consumeIntegrity(GolemConfig.basaltMeleeHitCost());
        return hurt;
    }

    // ---- crumbling (particles come from the base through getMaterialState) ----

    @Override
    protected BlockState getMaterialState() {
        return getSourceBlock();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(sourceBlock);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.basaltDropChance();
    }

    // ---- persistence ----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerId != null) output.store(TAG_OWNER, UUIDUtil.CODEC, ownerId);
        output.putString(TAG_MODE, getMode().name());
        output.store(TAG_SOURCE_BLOCK, BuiltInRegistries.BLOCK.byNameCodec(), sourceBlock);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerId = input.read(TAG_OWNER, UUIDUtil.CODEC).orElse(null);
        sourceBlock = input.read(TAG_SOURCE_BLOCK, BuiltInRegistries.BLOCK.byNameCodec()).orElse(Blocks.BASALT);
        ShieldMode mode;
        try {
            mode = ShieldMode.valueOf(input.getStringOr(TAG_MODE, ShieldMode.GUARD.name()));
        } catch (IllegalArgumentException e) {
            mode = ShieldMode.GUARD;
        }
        setMode(mode);
    }
}
