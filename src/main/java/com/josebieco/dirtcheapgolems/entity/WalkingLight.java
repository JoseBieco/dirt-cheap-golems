package com.josebieco.dirtcheapgolems.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Tracks the single {@code minecraft:light} block a golem keeps at its head (spec §6.3).
 * Only ever replaces air, and only ever removes a light block it placed itself.
 */
public final class WalkingLight {
    private static final String TAG_LIGHT_POS = "LightPos";

    private @Nullable BlockPos current;

    public @Nullable BlockPos current() {
        return current;
    }

    /** Moves the light to {@code desired}: removes the old one, places a new one only if the spot is air. */
    public void update(Level level, BlockPos desired, int lightLevel) {
        if (desired.equals(current)) return;
        clear(level);
        if (level.isEmptyBlock(desired) && level.getBlockState(desired).isAir()) {
            level.setBlock(desired, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel), Block.UPDATE_ALL);
            current = desired.immutable();
        }
    }

    /** Removes the current light if it is still a light block and its chunk is loaded. */
    public void clear(Level level) {
        if (current == null) return;
        removeLightAt(level, current);
        current = null;
    }

    static void removeLightAt(Level level, BlockPos pos) {
        if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.LIGHT))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    public void save(ValueOutput output) {
        if (current != null) output.store(TAG_LIGHT_POS, BlockPos.CODEC, current);
    }

    /**
     * Reads the light saved with the entity. It is not adopted as the current light: the caller must
     * remove it (see {@link #removeLightAt}) before placing a new one, so a light left behind by a crash
     * or an unload can never become an orphan.
     */
    public static @Nullable BlockPos load(ValueInput input) {
        return input.read(TAG_LIGHT_POS, BlockPos.CODEC).orElse(null);
    }
}
