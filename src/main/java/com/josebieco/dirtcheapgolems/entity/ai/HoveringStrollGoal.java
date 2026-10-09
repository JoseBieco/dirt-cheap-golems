package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.entity.EndStoneGolem;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Flying stroll whose targets stay in the idle hover band above the ground. */
public class HoveringStrollGoal extends WaterAvoidingRandomFlyingGoal {
    private final EndStoneGolem golem;

    public HoveringStrollGoal(EndStoneGolem golem, double speed) {
        super(golem, speed);
        this.golem = golem;
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        Vec3 pos = super.getPosition();
        if (pos == null) return null;
        double ground = golem.groundHeightAt(pos.x, pos.y, pos.z);
        if (Double.isNaN(ground)) return pos;
        double hover = EndStoneGolem.HOVER_MIN + golem.getRandom().nextDouble() * (EndStoneGolem.HOVER_MAX - EndStoneGolem.HOVER_MIN);
        return new Vec3(pos.x, ground + hover, pos.z);
    }
}
