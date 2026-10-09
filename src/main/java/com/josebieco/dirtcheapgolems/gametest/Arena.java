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

    /** A real survival player standing in the level at the given relative position. Remove with {@link #removePlayer}. */
    public static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper helper, net.minecraft.world.phys.Vec3 relative) {
        var player = helper.makeMockServerPlayerFull(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = helper.absoluteVec(relative);
        player.setPos(pos.x, pos.y, pos.z);
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    public static void removePlayer(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
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
