package com.doraamo.network;

import com.doraamo.destination.DestinationLocator;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketValidateTuner {

    private DestinationSettings settings = new DestinationSettings();

    public PacketValidateTuner() {
    }

    public PacketValidateTuner(DestinationSettings settings) {
        this.settings = settings;
    }

    public static void encode(PacketValidateTuner msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.settings.dimension == null ? "" : msg.settings.dimension);
        buf.writeEnum(msg.settings.mode);
        buf.writeUtf(msg.settings.biomeKey == null ? "minecraft:plains" : msg.settings.biomeKey);
        buf.writeUtf(msg.settings.structureName == null ? "Village" : msg.settings.structureName);
        buf.writeInt(msg.settings.x);
        buf.writeInt(msg.settings.y);
        buf.writeInt(msg.settings.z);
    }

    public static PacketValidateTuner decode(FriendlyByteBuf buf) {
        PacketValidateTuner msg = new PacketValidateTuner();
        msg.settings.dimension = DimUtil.normalize(buf.readUtf());
        msg.settings.mode = buf.readEnum(DestinationSettings.Mode.class);
        msg.settings.biomeKey = buf.readUtf();
        msg.settings.structureName = buf.readUtf();
        msg.settings.x = buf.readInt();
        msg.settings.y = buf.readInt();
        msg.settings.z = buf.readInt();
        return msg;
    }

    public static void handle(PacketValidateTuner msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            DestinationSettings s = msg.settings;
            ServerLevel world = DimUtil.getLevel(player.getServer(), s.dimension);
            if (world == null) {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), PacketValidateResult.notFound());
                return;
            }
            BlockPos near = player.blockPosition();
            if (!player.level().dimension().location().toString().equals(s.dimension)) {
                double scale = DimUtil.coordinateScale(player.level()) / DimUtil.coordinateScale(world);
                int y = (int) player.getY();
                if (DimUtil.isNether(s.dimension)) {
                    y = Math.max(32, Math.min(120, y));
                }
                near = new BlockPos(
                        (int) Math.floor(player.getX() * scale),
                        y,
                        (int) Math.floor(player.getZ() * scale));
            } else if (DimUtil.isNether(s.dimension)) {
                near = new BlockPos(near.getX(), Math.max(32, Math.min(120, near.getY())), near.getZ());
            }
            DestinationLocator.ValidateResult result = DestinationLocator.validate(world, near, s);
            if (!result.found || result.pos == null) {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), PacketValidateResult.notFound());
                return;
            }
            BlockPos placePos = result.pos;
            if (s.mode == DestinationSettings.Mode.BIOME
                    || (s.mode == DestinationSettings.Mode.STRUCTURE
                    && !DestinationLocator.isUndergroundStructure(s.structureName)
                    && !DestinationLocator.isWaterOrFloatStructure(s.structureName))) {
                BlockPos safe = PortalDoorPlacer.findSafeDoorPos(world, result.pos, PortalDoorPlacer.SAFE_SEARCH_RADIUS);
                if (safe != null) {
                    placePos = safe;
                }
            }
            PortalDoorPlacer.PlaceHazard hazard = PortalDoorPlacer.diagnose(world, placePos);
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new PacketValidateResult(true, placePos.getX(), placePos.getY(), placePos.getZ(), hazard.ordinal()));
        });
        ctx.get().setPacketHandled(true);
    }
}
