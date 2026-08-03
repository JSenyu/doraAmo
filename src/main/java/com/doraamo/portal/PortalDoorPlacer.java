package com.doraamo.portal;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

/**
 * Places portal doors without breaking solid blocks.
 * Safe search: solid floor, 2 replaceable cells, no liquid, no floating.
 */
public final class PortalDoorPlacer {

    public static final int SAFE_SEARCH_RADIUS = 32;

    private PortalDoorPlacer() {
    }

    /**
     * Exact coordinate: place only at {@code exact} if safe; otherwise null (caller should warn).
     */
    @Nullable
    public static BlockPos placeDoorExact(WorldServer world, BlockPos exact, EnumFacing facing,
                                          BlockPortalDoor.EnumType type, @Nullable PortalRef mainLink) {
        world.getChunkFromBlockCoords(exact);
        if (!canPlaceDoorSafely(world, exact)) {
            return null;
        }
        return placeDoorAt(world, exact, facing, type, mainLink);
    }

    /**
     * Non-exact: search near prefer for a safe spot; never clears solid blocks or builds platforms.
     */
    @Nullable
    public static BlockPos placeDoorSafeNear(WorldServer world, BlockPos prefer, EnumFacing facing,
                                             BlockPortalDoor.EnumType type, @Nullable PortalRef mainLink) {
        BlockPos base = findSafeDoorPos(world, prefer, SAFE_SEARCH_RADIUS);
        if (base == null) {
            return null;
        }
        return placeDoorAt(world, base, facing, type, mainLink);
    }

    @Nullable
    public static BlockPos placeDoorAt(WorldServer world, BlockPos base, EnumFacing facing,
                                       BlockPortalDoor.EnumType type, @Nullable PortalRef mainLink) {
        if (base.getY() < 1 || base.getY() >= world.getHeight() - 2) {
            return null;
        }

        ensureFoothold(world, base.down());

        IBlockState lower = ModBlocks.PORTAL_DOOR.getDefaultState()
                .withProperty(BlockPortalDoor.FACING, facing)
                .withProperty(BlockPortalDoor.HALF, BlockPortalDoor.EnumHalf.LOWER)
                .withProperty(BlockPortalDoor.TYPE, type);
        IBlockState upper = ModBlocks.PORTAL_DOOR.getDefaultState()
                .withProperty(BlockPortalDoor.FACING, facing)
                .withProperty(BlockPortalDoor.HALF, BlockPortalDoor.EnumHalf.UPPER)
                .withProperty(BlockPortalDoor.TYPE, type);

        world.setBlockState(base, lower, 3);
        world.setBlockState(base.up(), upper, 3);

        TileEntity te = world.getTileEntity(base);
        if (te instanceof TileEntityPortalDoor) {
            TileEntityPortalDoor portal = (TileEntityPortalDoor) te;
            if (type == BlockPortalDoor.EnumType.SUB && mainLink != null) {
                portal.setupAsSub(mainLink.dim, mainLink.pos);
                PortalNetworkData data = PortalNetworkData.get();
                if (data != null) {
                    data.registerSub(mainLink, new PortalRef(world.provider.getDimension(), base));
                }
            }
        }
        return base;
    }

    /** Prefer standing in front of the door; fall back to the door cell if the front is unsafe. */
    public static BlockPos playerStandPos(World world, BlockPos doorLower, EnumFacing facing) {
        BlockPos front = doorLower.offset(facing);
        if (canStandPlayer(world, front)) {
            return front;
        }
        if (canStandPlayer(world, doorLower)) {
            return doorLower;
        }
        return doorLower;
    }

    public static boolean canStandPlayer(World world, BlockPos feet) {
        BlockPos head = feet.up();
        BlockPos ground = feet.down();
        if (isLiquid(world, feet) || isLiquid(world, head) || isLiquid(world, ground)) {
            return false;
        }
        IBlockState groundState = world.getBlockState(ground);
        if (!groundState.isSideSolid(world, ground, EnumFacing.UP) || groundState.getMaterial().isLiquid()) {
            return false;
        }
        if (blocksMovementSolid(world, feet) || blocksMovementSolid(world, head)) {
            return false;
        }
        return true;
    }

    private static boolean blocksMovementSolid(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == ModBlocks.PORTAL_DOOR) {
            return false;
        }
        return state.getMaterial().blocksMovement() && !state.getBlock().isPassable(world, pos);
    }

    /**
     * Find nearest position where a 2-high door can stand safely (no block breaking needed).
     */
    @Nullable
    public static BlockPos findSafeDoorPos(WorldServer world, BlockPos prefer, int radius) {
        world.getChunkFromBlockCoords(prefer);

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
                    int preferY = MathHelper.clamp(prefer.getY(), minY, maxY);
                    BlockPos found = scanColumn(world, x, z, preferY, minY, maxY);
                    if (found == null) {
                        continue;
                    }
                    double dist = found.distanceSq(prefer);
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
    private static BlockPos scanColumn(WorldServer world, int x, int z, int preferY, int minY, int maxY) {
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

    /** Place obsidian-turf foothold when ground is missing or liquid. */
    public static void ensureFoothold(WorldServer world, BlockPos ground) {
        IBlockState g = world.getBlockState(ground);
        boolean need = !g.isSideSolid(world, ground, EnumFacing.UP) || g.getMaterial().isLiquid();
        if (need) {
            world.setBlockState(ground, ModBlocks.OBSIDIAN_TURF.getDefaultState(), 3);
        }
    }

    /** Diagnose why a portal cannot be placed safely at {@code pos} (lower half). */
    public static PlaceHazard diagnose(World world, BlockPos pos) {
        if (pos.getY() < verticalMin(world) || pos.getY() > verticalMax(world)) {
            return PlaceHazard.OUT_OF_BOUNDS;
        }
        BlockPos head = pos.up();
        BlockPos ground = pos.down();
        if (isLava(world, pos) || isLava(world, head) || isLava(world, ground)) {
            return PlaceHazard.LAVA;
        }
        if (isWater(world, pos) || isWater(world, head)) {
            return PlaceHazard.FLOODED;
        }
        Material gm = world.getBlockState(ground).getMaterial();
        if (gm == Material.FIRE || gm == Material.CACTUS) {
            return PlaceHazard.FIRE;
        }
        if (!isFreeForPortal(world, pos) || !isFreeForPortal(world, head)) {
            return PlaceHazard.WALL;
        }
        if (!world.getBlockState(ground).isSideSolid(world, ground, EnumFacing.UP)
                || world.getBlockState(ground).getMaterial().isLiquid()) {
            return PlaceHazard.NONE;
        }
        return PlaceHazard.NONE;
    }

    public static boolean canPlaceDoorSafely(World world, BlockPos pos) {
        return diagnose(world, pos) == PlaceHazard.NONE;
    }

    /**
     * Force-place by clearing the two portal cells (does not build a floor).
     */
    @Nullable
    public static BlockPos placeDoorForced(WorldServer world, BlockPos exact, EnumFacing facing,
                                           BlockPortalDoor.EnumType type, @Nullable PortalRef mainLink) {
        if (exact.getY() < 1 || exact.getY() >= world.getHeight() - 2) {
            return null;
        }
        world.getChunkFromBlockCoords(exact);
        world.setBlockToAir(exact);
        world.setBlockToAir(exact.up());
        return placeDoorAt(world, exact, facing, type, mainLink);
    }

    private static boolean isWater(World world, BlockPos pos) {
        Material m = world.getBlockState(pos).getMaterial();
        return m == Material.WATER || (m.isLiquid() && m != Material.LAVA);
    }

    private static boolean isLava(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.LAVA;
    }

    private static boolean isFreeForPortal(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (isLiquid(world, pos)) {
            return false;
        }
        return state.getBlock().isReplaceable(world, pos);
    }

    private static boolean isLiquid(World world, BlockPos pos) {
        Material m = world.getBlockState(pos).getMaterial();
        return m.isLiquid() || m == Material.WATER || m == Material.LAVA;
    }

    private static int verticalMin(World world) {
        if (world.provider.getDimension() == -1) {
            return 32;
        }
        return 1;
    }

    private static int verticalMax(World world) {
        if (world.provider.getDimension() == -1) {
            return Math.min(world.getActualHeight() - 3, 120);
        }
        if (world.provider.getDimension() == 1) {
            return Math.min(world.getActualHeight() - 3, 200);
        }
        return Math.min(world.getActualHeight() - 3, 254);
    }
}
