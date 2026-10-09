package com.josebieco.dirtcheapgolems.entity.projectile;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.registry.ModEffects;
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

/** Sand Golem projectile (spec §6.2): small damage, Slowness II and Sand in Eyes. Flies through anything not hostile. */
public class SandBlast extends ThrowableItemProjectile {
    public static final float SPEED = 1.5F;

    public SandBlast(EntityType<? extends SandBlast> type, Level level) {
        super(type, level);
    }

    public SandBlast(Level level, LivingEntity owner) {
        super(ModEntities.SAND_BLAST.get(), owner, level, new ItemStack(Items.SAND));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SAND;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    /**
     * Only hostile mobs stop the blast; it passes through players, golems and passive mobs (spec §5.8).
     */
    public static boolean isValidTarget(Entity entity) {
        return HostileTargets.isValid(entity);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return isValidTarget(entity) && super.canHitEntity(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.tickCount % 2 == 0)
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (!(hitResult.getEntity() instanceof LivingEntity target) || !isValidTarget(target)) return;
        target.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), (float) GolemConfig.sandDamage());
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, GolemConfig.sandSlownessTicks(), 1), this.getOwner());
        target.addEffect(new MobEffectInstance(ModEffects.sandInEyes(), GolemConfig.sandInEyesTicks()), this.getOwner());
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.0);
            this.discard();
        }
    }
}
