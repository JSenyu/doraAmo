package com.doraamo.teleport;

import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

public class PortalDoorTeleporter implements ITeleporter {

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

    @Nullable
    private final BlockPos absoluteLanding;

    public PortalDoorTeleporter(ServerLevel world) {
        this.absoluteLanding = null;
    }

    public PortalDoorTeleporter(ServerLevel world, BlockPos absoluteLanding) {
        this.absoluteLanding = absoluteLanding;
    }

    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        BlockPos land = absoluteLanding;
        if (land == null) {
            int x = Mth.floor(entity.getX());
            int z = Mth.floor(entity.getZ());
            int y = Mth.floor(entity.getY());
            land = PortalDoorPlacer.findSafeDoorPos(destWorld, new BlockPos(x, y, z), PortalDoorPlacer.SAFE_SEARCH_RADIUS);
            if (land == null) {
                land = new BlockPos(x, Mth.clamp(y, 1, destWorld.getMaxBuildHeight() - 3), z);
            }
        }
        return new PortalInfo(
                new Vec3(land.getX() + 0.5D, land.getY(), land.getZ() + 0.5D),
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot());
    }

    @Override
    public boolean isVanilla() {
        return false;
    }
}
