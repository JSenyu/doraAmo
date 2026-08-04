package com.doraamo.teleport;

import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import net.minecraft.block.PortalInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

public class PortalDoorTeleporter implements ITeleporter {

    public static BlockPos scalePortalPos(ServerWorld from, ServerWorld to, BlockPos portalPos) {
        if (DimUtil.isEnd(DimUtil.levelKey(to))) {
            BlockPos spawn = to.getSharedSpawnPos();
            return spawn != null ? spawn : new BlockPos(100, 50, 0);
        }
        double scale = DimUtil.coordinateScale(from) / DimUtil.coordinateScale(to);
        int x = MathHelper.floor(portalPos.getX() * scale);
        int z = MathHelper.floor(portalPos.getZ() * scale);
        int y = MathHelper.clamp(portalPos.getY(), 1, to.getMaxBuildHeight() - 3);
        if (DimUtil.isNether(DimUtil.levelKey(to))) {
            y = MathHelper.clamp(y, 32, 120);
        }
        return new BlockPos(x, y, z);
    }

    @Nullable
    public static BlockPos findLandingFromPortal(ServerWorld world, BlockPos scaledPortalPos) {
        return PortalDoorPlacer.findSafeDoorPos(world, scaledPortalPos, PortalDoorPlacer.SAFE_SEARCH_RADIUS);
    }

    @Nullable
    private final BlockPos absoluteLanding;

    public PortalDoorTeleporter(ServerWorld world) {
        this.absoluteLanding = null;
    }

    public PortalDoorTeleporter(ServerWorld world, BlockPos absoluteLanding) {
        this.absoluteLanding = absoluteLanding;
    }

    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerWorld destWorld,
                                    Function<ServerWorld, PortalInfo> defaultPortalInfo) {
        BlockPos land = absoluteLanding;
        if (land == null) {
            int x = MathHelper.floor(entity.getX());
            int z = MathHelper.floor(entity.getZ());
            int y = MathHelper.floor(entity.getY());
            land = PortalDoorPlacer.findSafeDoorPos(destWorld, new BlockPos(x, y, z), PortalDoorPlacer.SAFE_SEARCH_RADIUS);
            if (land == null) {
                land = new BlockPos(x, MathHelper.clamp(y, 1, destWorld.getMaxBuildHeight() - 3), z);
            }
        }
        return new PortalInfo(
                new Vector3d(land.getX() + 0.5D, land.getY(), land.getZ() + 0.5D),
                Vector3d.ZERO,
                entity.yRot,
                entity.xRot);
    }

    @Override
    public boolean isVanilla() {
        return false;
    }
}
