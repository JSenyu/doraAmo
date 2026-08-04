package com.doraamo.portal;

import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

public final class PortalFinder {

    public static final int SEARCH_CHUNK_RADIUS = 6;

    public static final class NearestPortal {
        public final BlockPos pos;
        public final boolean subGate;
        public final double distance;

        public NearestPortal(BlockPos pos, boolean subGate, double distance) {
            this.pos = pos;
            this.subGate = subGate;
            this.distance = distance;
        }
    }

    private PortalFinder() {
    }

    @Nullable
    public static NearestPortal findNearest(World world, PlayerEntity player) {
        if (world == null || player == null) {
            return null;
        }
        ChunkPos origin = new ChunkPos(player.blockPosition());
        BlockPos bestPos = null;
        boolean bestSub = false;
        double bestDist = Double.MAX_VALUE;

        for (int cx = -SEARCH_CHUNK_RADIUS; cx <= SEARCH_CHUNK_RADIUS; cx++) {
            for (int cz = -SEARCH_CHUNK_RADIUS; cz <= SEARCH_CHUNK_RADIUS; cz++) {
                if (!world.hasChunk(origin.x + cx, origin.z + cz)) {
                    continue;
                }
                Chunk chunk = world.getChunk(origin.x + cx, origin.z + cz);
                for (TileEntity te : chunk.getBlockEntities().values()) {
                    if (!(te instanceof TileEntityPortalDoor)) {
                        continue;
                    }
                    TileEntityPortalDoor portal = (TileEntityPortalDoor) te;
                    BlockPos p = portal.getBlockPos();
                    double dist = player.distanceToSqr(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestPos = p.immutable();
                        bestSub = portal.isSubGate();
                    }
                }
            }
        }
        if (bestPos == null) {
            return null;
        }
        return new NearestPortal(bestPos, bestSub, Math.sqrt(bestDist));
    }

    public static String directionKey(PlayerEntity player, BlockPos target) {
        double dx = (target.getX() + 0.5D) - player.getX();
        double dz = (target.getZ() + 0.5D) - player.getZ();
        double dy = (target.getY() + 0.5D) - (player.getY() + player.getEyeHeight());

        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 2.0D && Math.abs(dy) > 2.0D) {
            return dy > 0 ? "doraamo.tuner.dir.up" : "doraamo.tuner.dir.down";
        }

        double angle = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
        int sector = MathHelper.floor((angle + 180.0D + 22.5D) / 45.0D) & 7;
        switch (sector) {
            case 0: return "doraamo.tuner.dir.s";
            case 1: return "doraamo.tuner.dir.sw";
            case 2: return "doraamo.tuner.dir.w";
            case 3: return "doraamo.tuner.dir.nw";
            case 4: return "doraamo.tuner.dir.n";
            case 5: return "doraamo.tuner.dir.ne";
            case 6: return "doraamo.tuner.dir.e";
            case 7:
            default: return "doraamo.tuner.dir.se";
        }
    }
}
