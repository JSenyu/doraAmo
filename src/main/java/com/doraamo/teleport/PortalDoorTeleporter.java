package com.doraamo.teleport;

import com.doraamo.portal.PortalDoorPlacer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;

/**
 * Places the player after dimension change.
 * Landing positions are absolute; coordinate scaling is done by callers via {@link #scalePortalPos}.
 */
public class PortalDoorTeleporter implements ITeleporter {

    /**
     * Resolve destination XZ from a source portal block using vanilla movement factors.
     */
    public static BlockPos scalePortalPos(WorldServer from, WorldServer to, BlockPos portalPos) {
        if (to.provider.getDimension() == 1) {
            BlockPos spawn = to.provider.getSpawnCoordinate();
            return spawn != null ? spawn : new BlockPos(100, 50, 0);
        }
        double scale = from.provider.getMovementFactor() / to.provider.getMovementFactor();
        int x = MathHelper.floor(portalPos.getX() * scale);
        int z = MathHelper.floor(portalPos.getZ() * scale);
        int y = MathHelper.clamp(portalPos.getY(), 1, to.getHeight() - 3);
        if (to.provider.getDimension() == -1) {
            y = MathHelper.clamp(y, 32, 120);
        }
        return new BlockPos(x, y, z);
    }

    /**
     * Find a safe door-anchor near the given coordinate. Never destroys blocks or builds platforms.
     */
    @Nullable
    public static BlockPos findLandingFromPortal(WorldServer world, BlockPos scaledPortalPos) {
        return PortalDoorPlacer.findSafeDoorPos(world, scaledPortalPos, PortalDoorPlacer.SAFE_SEARCH_RADIUS);
    }

    @Nullable
    private final BlockPos absoluteLanding;
    private BlockPos lastLandingPos;

    public PortalDoorTeleporter(WorldServer world) {
        this.absoluteLanding = null;
    }

    public PortalDoorTeleporter(WorldServer world, BlockPos absoluteLanding) {
        this.absoluteLanding = absoluteLanding;
    }

    public BlockPos getLastLandingPos() {
        return lastLandingPos;
    }

    @Override
    public void placeEntity(World worldIn, Entity entity, float yaw) {
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;

        if (absoluteLanding != null) {
            BlockPos land = absoluteLanding;
            entity.setPositionAndUpdate(land.getX() + 0.5D, land.getY(), land.getZ() + 0.5D);
            lastLandingPos = land;
            return;
        }

        int x = MathHelper.floor(entity.posX);
        int z = MathHelper.floor(entity.posZ);
        int y = MathHelper.floor(entity.posY);
        BlockPos found = PortalDoorPlacer.findSafeDoorPos(
                (WorldServer) worldIn, new BlockPos(x, y, z), PortalDoorPlacer.SAFE_SEARCH_RADIUS);
        if (found == null) {
            found = new BlockPos(x, MathHelper.clamp(y, 1, worldIn.getHeight() - 3), z);
        }
        lastLandingPos = found;
        entity.setPositionAndUpdate(found.getX() + 0.5D, found.getY(), found.getZ() + 0.5D);
    }
}
