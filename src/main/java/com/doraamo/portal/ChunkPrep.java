package com.doraamo.portal;

import com.doraamo.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.server.level.ServerLevel;

public final class ChunkPrep {

    private ChunkPrep() {
    }

    public static void forcePopulateAround(ServerLevel world, BlockPos center, int radiusChunks) {
        if (world == null || center == null) {
            return;
        }
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;

        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                world.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, true);
            }
        }
    }

    public static BlockPos prepareEndPortalSite(ServerLevel world, Direction facing) {
        BlockPos spawn = world.getSharedSpawnPos();
        if (spawn == null) {
            spawn = new BlockPos(100, 50, 0);
        }
        forcePopulateAround(world, spawn, 2);

        int sx = spawn.getX();
        int sy = spawn.getY();
        int sz = spawn.getZ();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos ground = new BlockPos(sx + x, sy - 1, sz + z);
                world.setBlock(ground, Blocks.OBSIDIAN.defaultBlockState(), 2);
                for (int y = 0; y < 3; y++) {
                    world.removeBlock(new BlockPos(sx + x, sy + y, sz + z), false);
                }
            }
        }

        Direction dir = facing == null ? Direction.EAST : facing;
        int ex = sx + dir.getStepX() * 3;
        int ez = sz + dir.getStepZ() * 3;
        BlockPos turf = new BlockPos(ex, sy - 1, ez);
        world.setBlock(turf, ModBlocks.OBSIDIAN_TURF.get().defaultBlockState(), 3);
        BlockPos door = turf.above();
        world.removeBlock(door, false);
        world.removeBlock(door.above(), false);
        world.removeBlock(door.relative(dir), false);
        world.removeBlock(door.relative(dir).above(), false);
        return door;
    }
}
