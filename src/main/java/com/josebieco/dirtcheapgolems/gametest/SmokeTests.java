package com.josebieco.dirtcheapgolems.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.gametest.GameTestPrefix;

@GameTestNamespace("dirtcheapgolems")
@GameTestPrefix("smoke")
public final class SmokeTests {
    @GameTest(structure = Arena.STRUCTURE)
    public static void arenaLoads(GameTestHelper helper) {
        helper.succeed();
    }
}
