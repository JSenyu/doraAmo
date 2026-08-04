package com.doraamo.teleport;

import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PortalDoorTeleporter {

    public static BlockPos scalePortalPos(ServerLevel from, ServerLevel to, BlockPos portalPos) {
        if (DimUtil.isEnd(DimUtil.levelKey(to))) {
            BlockPos spawn = to.getSharedSpawnPos();
            return spawn != null ? spawn : new BlockPos(100, 50, 0);
        }
        double scale = DimUtil.coordinateScale(from) / DimUtil.coordinateScale(to);
        int x = Mth.floor(portalPos.getX() * scale);
        int z = Mth.floor(portalPos.getZ() * scale);
        int y = Mth.clamp(portalPos.getY(), 1, to.getMaxBuildHeight() - 3);
        if (DimUtil.isNether(DimUtil.levelKey(to))) {
            y = Mth.clamp(y, 32, 120);
        }
        return new BlockPos(x, y, z);
    }

    @Nullable
    public static BlockPos findLandingFromPortal(ServerLevel world, BlockPos scaledPortalPos) {
        return PortalDoorPlacer.findSafeDoorPos(world, scaledPortalPos, PortalDoorPlacer.SAFE_SEARCH_RADIUS);
    }

    public static DimensionTransition createTransition(ServerLevel destWorld, BlockPos land, Entity entity) {
        return new DimensionTransition(
                destWorld,
                new Vec3(land.getX() + 0.5D, land.getY(), land.getZ() + 0.5D),
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.DO_NOTHING);
    }

    private PortalDoorTeleporter() {
    }
}
