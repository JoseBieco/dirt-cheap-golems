package com.josebieco.dirtcheapgolems.gametest;

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
}
