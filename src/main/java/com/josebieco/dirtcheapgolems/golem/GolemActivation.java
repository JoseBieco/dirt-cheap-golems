package com.josebieco.dirtcheapgolems.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Turns a two-block pillar of the same material into a golem (spec §5.2). */
public final class GolemActivation {
    private GolemActivation() {}

    private record Pillar(BlockPos top, BlockPos bottom, GolemType type) {}

    public static InteractionResult tryActivate(Level level, BlockPos clicked, Player player, ItemStack stack) {
        var pillar = findPillar(level, clicked);
        if (pillar == null) return InteractionResult.PASS;
        if (!mayModify(level, player, pillar.top(), stack) || !mayModify(level, player, pillar.bottom(), stack))
            return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        var entityType = pillar.type().entityType();
        if (entityType == null) return InteractionResult.PASS;

        BlockState material = level.getBlockState(pillar.bottom());
        level.setBlock(pillar.top(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pillar.bottom(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        var golem = entityType.spawn(serverLevel, pillar.bottom(), EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            // Could not spawn (e.g. cancelled by another mod): put the pillar back, keep the core.
            level.setBlock(pillar.bottom(), material, Block.UPDATE_ALL);
            level.setBlock(pillar.top(), material, Block.UPDATE_ALL);
            return InteractionResult.PASS;
        }
        golem.setHomePos(pillar.bottom());

        if (!player.hasInfiniteMaterials()) stack.shrink(1);

        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, material),
            pillar.bottom().getX() + 0.5, pillar.bottom().getY() + 1.0, pillar.bottom().getZ() + 0.5,
            40, 0.3, 0.6, 0.3, 0.1);
        level.playSound(null, pillar.bottom(), material.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.SUCCESS;
    }

    /** Clicked block as top with the one below, or clicked block as bottom with the one above. */
    private static Pillar findPillar(Level level, BlockPos clicked) {
        var asTop = GolemType.fromPillar(level.getBlockState(clicked), level.getBlockState(clicked.below()));
        if (asTop.isPresent()) return new Pillar(clicked, clicked.below(), asTop.get());
        var asBottom = GolemType.fromPillar(level.getBlockState(clicked.above()), level.getBlockState(clicked));
        return asBottom.map(type -> new Pillar(clicked.above(), clicked, type)).orElse(null);
    }

    private static boolean mayModify(Level level, Player player, BlockPos pos, ItemStack stack) {
        return level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, stack);
    }
}
