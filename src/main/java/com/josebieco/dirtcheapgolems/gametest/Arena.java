package com.josebieco.dirtcheapgolems.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Shared arena for game tests: Forge generates empty (all air) structures named forge:emptyXxYxZ. */
public final class Arena {
    public static final String STRUCTURE = "forge:empty9x5x9";

    private Arena() {}

    /** Stone floor at relative y = 0, so things stand at y = 1. */
    public static void floor(GameTestHelper helper) {
        helper.makeFloor(Blocks.STONE, 0);
    }

    /** Test arenas land in arbitrary biomes (deserts never rain): force plains over the arena. */
    public static void rainyBiome(GameTestHelper helper) {
        var min = helper.absolutePos(BlockPos.ZERO);
        var max = helper.absolutePos(new BlockPos(8, 4, 8));
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withLevel(helper.getLevel()).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, String.format("fillbiome %d %d %d %d %d %d minecraft:plains",
            Math.min(min.getX(), max.getX()), min.getY() - 1, Math.min(min.getZ(), max.getZ()),
            Math.max(min.getX(), max.getX()), max.getY() + 1, Math.max(min.getZ(), max.getZ())));
    }
}
