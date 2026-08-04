package com.doraamo.portal;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public final class PortalDoorPlacer {

    public static final int SAFE_SEARCH_RADIUS = 32;

    private PortalDoorPlacer() {
    }

    @Nullable
    public static BlockPos placeDoorExact(ServerLevel world, BlockPos exact, Direction facing,
                                          BlockPortalDoor.DoorType type, @Nullable PortalRef mainLink) {
        world.getChunk(exact);
        if (!canPlaceDoorSafely(world, exact)) {
            return null;
        }
        return placeDoorAt(world, exact, facing, type, mainLink);
    }

    @Nullable
    public static BlockPos placeDoorSafeNear(ServerLevel world, BlockPos prefer, Direction facing,
                                             BlockPortalDoor.DoorType type, @Nullable PortalRef mainLink) {
        BlockPos base = findSafeDoorPos(world, prefer, SAFE_SEARCH_RADIUS);
        if (base == null) {
            return null;
        }
        return placeDoorAt(world, base, facing, type, mainLink);
    }

    @Nullable
    public static BlockPos placeDoorAt(ServerLevel world, BlockPos base, Direction facing,
                                       BlockPortalDoor.DoorType type, @Nullable PortalRef mainLink) {
        if (base.getY() < 1 || base.getY() >= world.getMaxBuildHeight() - 2) {
            return null;
        }

        ensureFoothold(world, base.below());

        BlockState lower = ModBlocks.PORTAL_DOOR.get().defaultBlockState()
                .setValue(BlockPortalDoor.FACING, facing)
                .setValue(BlockPortalDoor.HALF, BlockPortalDoor.Half.LOWER)
                .setValue(BlockPortalDoor.TYPE, type);
        BlockState upper = ModBlocks.PORTAL_DOOR.get().defaultBlockState()
                .setValue(BlockPortalDoor.FACING, facing)
                .setValue(BlockPortalDoor.HALF, BlockPortalDoor.Half.UPPER)
                .setValue(BlockPortalDoor.TYPE, type);

        world.setBlock(base, lower, 3);
        world.setBlock(base.above(), upper, 3);

        BlockEntity te = world.getBlockEntity(base);
        if (te instanceof TileEntityPortalDoor portal) {
            if (type == BlockPortalDoor.DoorType.SUB && mainLink != null) {
                portal.setupAsSub(mainLink.dim, mainLink.pos);
                PortalNetworkData data = PortalNetworkData.get();
                if (data != null) {
                    data.registerSub(mainLink, new PortalRef(DimUtil.levelKey(world), base));
                }
            }
        }
        return base;
    }

    public static BlockPos playerStandPos(Level world, BlockPos doorLower, Direction facing) {
        BlockPos front = doorLower.relative(facing);
        if (canStandPlayer(world, front)) {
            return front;
        }
        if (canStandPlayer(world, doorLower)) {
            return doorLower;
        }
        return doorLower;
    }

    public static boolean canStandPlayer(Level world, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos ground = feet.below();
        if (isLiquid(world, feet) || isLiquid(world, head) || isLiquid(world, ground)) {
            return false;
        }
        BlockState groundState = world.getBlockState(ground);
        if (!groundState.isFaceSturdy(world, ground, Direction.UP) || !groundState.getFluidState().isEmpty()) {
            return false;
        }
        return !blocksMovementSolid(world, feet) && !blocksMovementSolid(world, head);
    }

    private static boolean blocksMovementSolid(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == ModBlocks.PORTAL_DOOR.get()) {
            return false;
        }
        return state.blocksMotion() && !state.getBlock().isPossibleToRespawnInThis(state);
    }

    @Nullable
    public static BlockPos findSafeDoorPos(ServerLevel world, BlockPos prefer, int radius) {
        world.getChunk(prefer);

        int minY = verticalMin(world);
        int maxY = verticalMax(world);

        if (canPlaceDoorSafely(world, prefer)) {
            return prefer;
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = prefer.getX() + dx;
                    int z = prefer.getZ() + dz;
                    int preferY = Mth.clamp(prefer.getY(), minY, maxY);
                    BlockPos found = scanColumn(world, x, z, preferY, minY, maxY);
                    if (found == null) {
                        continue;
                    }
                    double dist = found.distSqr(prefer);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = found;
                        if (r == 0 && dist < 1.0D) {
                            return best;
                        }
                    }
                }
            }
            if (best != null && r >= 4) {
                return best;
            }
        }
        return best;
    }

    @Nullable
    private static BlockPos scanColumn(ServerLevel world, int x, int z, int preferY, int minY, int maxY) {
        for (int y = preferY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (canPlaceDoorSafely(world, pos)) {
                return pos;
            }
        }
        for (int y = preferY + 1; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (canPlaceDoorSafely(world, pos)) {
                return pos;
            }
        }
        return null;
    }

    public enum PlaceHazard {
        NONE,
        OUT_OF_BOUNDS,
        FLOATING,
        WALL,
        FLOODED,
        LAVA,
        FIRE
    }

    public static void ensureFoothold(ServerLevel world, BlockPos ground) {
        BlockState g = world.getBlockState(ground);
        boolean need = !g.isFaceSturdy(world, ground, Direction.UP) || !g.getFluidState().isEmpty();
        if (need) {
            world.setBlock(ground, ModBlocks.OBSIDIAN_TURF.get().defaultBlockState(), 3);
        }
    }

    public static PlaceHazard diagnose(Level world, BlockPos pos) {
        if (pos.getY() < verticalMin(world) || pos.getY() > verticalMax(world)) {
            return PlaceHazard.OUT_OF_BOUNDS;
        }
        BlockPos head = pos.above();
        BlockPos ground = pos.below();
        if (isLava(world, pos) || isLava(world, head) || isLava(world, ground)) {
            return PlaceHazard.LAVA;
        }
        if (isWater(world, pos) || isWater(world, head)) {
            return PlaceHazard.FLOODED;
        }
        BlockState groundState = world.getBlockState(ground);
        if (groundState.isFlammable(world, ground, Direction.UP) || groundState.is(Blocks.CACTUS)) {
            return PlaceHazard.FIRE;
        }
        if (!isFreeForPortal(world, pos) || !isFreeForPortal(world, head)) {
            return PlaceHazard.WALL;
        }
        if (!groundState.isFaceSturdy(world, ground, Direction.UP) || !groundState.getFluidState().isEmpty()) {
            return PlaceHazard.NONE;
        }
        return PlaceHazard.NONE;
    }

    public static boolean canPlaceDoorSafely(Level world, BlockPos pos) {
        return diagnose(world, pos) == PlaceHazard.NONE;
    }

    @Nullable
    public static BlockPos placeDoorForced(ServerLevel world, BlockPos exact, Direction facing,
                                           BlockPortalDoor.DoorType type, @Nullable PortalRef mainLink) {
        if (exact.getY() < 1 || exact.getY() >= world.getMaxBuildHeight() - 2) {
            return null;
        }
        world.getChunk(exact);
        world.removeBlock(exact, false);
        world.removeBlock(exact.above(), false);
        return placeDoorAt(world, exact, facing, type, mainLink);
    }

    private static boolean isWater(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getFluidState().getType() == Fluids.WATER;
    }

    private static boolean isLava(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getFluidState().getType() == Fluids.LAVA;
    }

    private static boolean isFreeForPortal(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (isLiquid(world, pos)) {
            return false;
        }
        return state.isAir() || state.canBeReplaced();
    }

    private static boolean isLiquid(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getFluidState().isEmpty();
    }

    private static int verticalMin(Level world) {
        if (DimUtil.isNether(DimUtil.levelKey(world))) {
            return 32;
        }
        return 1;
    }

    private static int verticalMax(Level world) {
        if (DimUtil.isNether(DimUtil.levelKey(world))) {
            return Math.min(world.getMaxBuildHeight() - 3, 120);
        }
        if (DimUtil.isEnd(DimUtil.levelKey(world))) {
            return Math.min(world.getMaxBuildHeight() - 3, 200);
        }
        return Math.min(world.getMaxBuildHeight() - 3, 254);
    }
}
