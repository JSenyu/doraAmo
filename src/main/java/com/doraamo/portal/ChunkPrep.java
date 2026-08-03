package com.doraamo.portal;

import com.doraamo.block.ModBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * Forces chunk terrain + decoration/structure population before placing portals,
 * so later generation does not overwrite the door.
 */
public final class ChunkPrep {

    private ChunkPrep() {
    }

    public static void forcePopulateAround(WorldServer world, BlockPos center, int radiusChunks) {
        if (world == null || center == null) {
            return;
        }
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) {
            world.getChunkFromBlockCoords(center);
            return;
        }
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;

        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                world.getChunkFromChunkCoords(cx + dx, cz + dz);
            }
        }
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                Chunk chunk = world.getChunkFromChunkCoords(cx + dx, cz + dz);
                if (!chunk.isTerrainPopulated()) {
                    try {
                        chunk.populate(provider, provider.chunkGenerator);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Vanilla-like End obsidian platform at spawn, plus a one-block turf extension for the portal
     * so platform resets do not destroy the door.
     *
     * @return lower-half position for the portal on the turf extension
     */
    public static BlockPos prepareEndPortalSite(WorldServer world, EnumFacing facing) {
        BlockPos spawn = world.provider.getSpawnCoordinate();
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
                world.setBlockState(ground, Blocks.OBSIDIAN.getDefaultState(), 2);
                for (int y = 0; y < 3; y++) {
                    world.setBlockToAir(new BlockPos(sx + x, sy + y, sz + z));
                }
            }
        }

        EnumFacing dir = facing == null ? EnumFacing.EAST : facing;
        int ex = sx + dir.getFrontOffsetX() * 3;
        int ez = sz + dir.getFrontOffsetZ() * 3;
        BlockPos turf = new BlockPos(ex, sy - 1, ez);
        world.setBlockState(turf, ModBlocks.OBSIDIAN_TURF.getDefaultState(), 3);
        BlockPos door = turf.up();
        world.setBlockToAir(door);
        world.setBlockToAir(door.up());
        world.setBlockToAir(door.offset(dir));
        world.setBlockToAir(door.offset(dir).up());
        return door;
    }
}
