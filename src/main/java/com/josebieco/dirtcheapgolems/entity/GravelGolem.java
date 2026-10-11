package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.GravelArtilleryGoal;
import com.josebieco.dirtcheapgolems.entity.ai.GravelLeapGoal;
import com.josebieco.dirtcheapgolems.entity.ai.LowestFirstTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.ForgeEventFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Artilheiro de Cerco" (spec v2 6.2). Artillery lives in {@link GravelArtilleryGoal}; the leap/collapse goal
 * is added on top in {@link #registerGoals()} (priority 1 is left free for it).
 */
public class GravelGolem extends AbstractCheapGolem {
    public GravelGolem(EntityType<? extends GravelGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Follow range is the upper bound for targeting; the configured range is enforced by the target selector.
        return createBaseAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.15)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new GravelLeapGoal(this));
        this.goalSelector.addGoal(2, new GravelArtilleryGoal(this));
        this.targetSelector.addGoal(1, new LowestFirstTargetGoal(this, 10, true,
            (target, level) -> target instanceof Enemy && this.distanceTo(target) <= GolemConfig.gravelArtilleryRange()));
    }

    /** The block directly above the target's head. */
    public static BlockPos headPos(LivingEntity target) {
        return BlockPos.containing(target.getX(), target.getBoundingBox().maxY - 1.0E-3, target.getZ()).above();
    }

    /**
     * The collapse (spec v2 6.2): hurts the target, buries it under falling gravel (or drops gravel items when
     * mob griefing or leapPlacesBlocks is off) and removes the golem without {@link #crumble()} drops.
     * Only air is ever replaced.
     */
    public void collapseOnto(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return;
        target.hurtServer(level, level.damageSources().mobAttack(this), (float) GolemConfig.gravelLeapImpactDamage());
        int blocks = GolemConfig.gravelLeapBlocks();
        if (GolemConfig.gravelLeapPlacesBlocks() && ForgeEventFactory.getMobGriefingEvent(level, this)) {
            // Falling blocks do not collide with mobs: gravel dropped from above the head settles in the feet
            // block and the one stacked on top of it in the eye block, which is what actually suffocates.
            BlockPos head = headPos(target);
            List<BlockPos> sides = new ArrayList<>();
            for (Direction d : Direction.Plane.HORIZONTAL) sides.add(head.relative(d));
            Collections.shuffle(sides, new java.util.Random(this.getRandom().nextLong()));
            List<BlockPos> positions = new ArrayList<>();
            positions.add(head);
            positions.add(head.above());
            positions.addAll(sides);
            for (BlockPos pos : positions.subList(0, Math.min(blocks, positions.size()))) {
                if (level.getBlockState(pos).isAir()) {
                    FallingBlockEntity.fall(level, pos, Blocks.GRAVEL.defaultBlockState());
                }
            }
        } else {
            level.addFreshEntity(new ItemEntity(level, target.getX(), target.getY(), target.getZ(),
                new ItemStack(Items.GRAVEL, blocks)));
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState()),
            target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.GRAVEL_BREAK, SoundSource.NEUTRAL, 1.2F, 0.7F);
        this.discard();
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.GRAVEL.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.GRAVEL);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.gravelDropChance();
    }
}
