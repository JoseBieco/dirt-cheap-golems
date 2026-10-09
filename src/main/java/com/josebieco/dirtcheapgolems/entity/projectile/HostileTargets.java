package com.josebieco.dirtcheapgolems.entity.projectile;

import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Shared rule of the golem projectiles (spec 5.8): only hostile mobs stop them; everything else is flown through. */
final class HostileTargets {
    private HostileTargets() {}

    /** Golems are checked explicitly in case another mod marks one as an {@link Enemy}. */
    static boolean isValid(Entity entity) {
        return entity instanceof Enemy && !(entity instanceof Player) && !(entity instanceof AbstractCheapGolem)
            && !(entity instanceof IronGolem) && !(entity instanceof SnowGolem);
    }
}
