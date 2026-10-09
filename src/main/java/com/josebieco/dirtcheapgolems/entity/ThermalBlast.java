package com.josebieco.dirtcheapgolems.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Netherrack Golem's thermal blast (spec §6.3). Not a vanilla {@code Explosion}: it never touches blocks
 * and never places fire. It hurts and ignites hostile mobs only (and players, if configured).
 */
public final class ThermalBlast {
    private ThermalBlast() {}

    public static void detonate(ServerLevel level, AbstractCheapGolem source, double radius, float damage, int burnSeconds, boolean hurtPlayers) {
        var fire = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.IN_FIRE);
        var damageSource = new DamageSource(fire, source);
        var area = source.getBoundingBox().inflate(radius);
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> isAffected(e, source, hurtPlayers))) {
            if (entity.distanceTo(source) > radius) continue;
            entity.hurtServer(level, damageSource, damage);
            entity.igniteForSeconds(burnSeconds);
        }

        double y = source.getY(0.5);
        level.sendParticles(ParticleTypes.FLAME, source.getX(), y, source.getZ(), 60, radius / 2, 0.5, radius / 2, 0.05);
        level.sendParticles(ParticleTypes.LAVA, source.getX(), y, source.getZ(), 15, radius / 3, 0.3, radius / 3, 0.0);
        level.playSound(null, source.getX(), y, source.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    /** Spec §5.8: never golems or passive mobs; players only when blastHurtsPlayers is on. */
    static boolean isAffected(LivingEntity entity, AbstractCheapGolem source, boolean hurtPlayers) {
        if (entity == source || !entity.isAlive()) return false;
        if (entity instanceof AbstractCheapGolem || entity instanceof IronGolem || entity instanceof SnowGolem) return false;
        if (entity instanceof Player player) return hurtPlayers && !player.isCreative() && !player.isSpectator();
        return entity instanceof Enemy;
    }
}
