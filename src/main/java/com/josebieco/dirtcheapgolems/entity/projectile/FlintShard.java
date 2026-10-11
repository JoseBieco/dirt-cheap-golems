package com.josebieco.dirtcheapgolems.entity.projectile;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Gravel Golem artillery (spec v2 6.2): ballistic shard, damage plus Slowness I. Flies through anything not hostile. */
public class FlintShard extends ThrowableItemProjectile {
    public static final float SPEED = 1.4F;
    public static final double GRAVITY = 0.03;

    public FlintShard(EntityType<? extends FlintShard> type, Level level) {
        super(type, level);
    }

    public FlintShard(Level level, LivingEntity owner) {
        super(ModEntities.FLINT_SHARD.get(), owner, level, new ItemStack(Items.FLINT));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FLINT;
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    public static boolean isValidTarget(Entity entity) {
        return HostileTargets.isValid(entity);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return isValidTarget(entity) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (!(hitResult.getEntity() instanceof LivingEntity target) || !isValidTarget(target)) return;
        target.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), (float) GolemConfig.gravelShardDamage());
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, GolemConfig.gravelSlownessTicks(), 0), this.getOwner());
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 10, 0.15, 0.15, 0.15, 0.05);
            this.discard();
        }
    }
}
