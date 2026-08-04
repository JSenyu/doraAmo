package com.doraamo.teleport;

import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;

public final class PortalDoorTeleporter {

    private PortalDoorTeleporter() {
    }

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

    public static void teleportPlayer(ServerPlayer player, ServerLevel targetWorld, BlockPos land) {
        double x = land.getX() + 0.5D;
        double y = land.getY();
        double z = land.getZ() + 0.5D;
        if (player.level() != targetWorld) {
            player.teleportTo(targetWorld, x, y, z, EnumSet.noneOf(RelativeMovement.class), player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(x, y, z);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
        }
    }
}
