package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
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
        if (source.getEntity() instanceof LivingEntity && !source.is(DamageTypeTags.IS_PROJECTILE))
            consumeIntegrity(GolemConfig.basaltMeleeHitCost());
        return super.hurtServer(level, source, damage);
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
