package com.doraamo.network;

import com.doraamo.destination.DestinationLocator;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ValidateTunerPayload(DestinationSettings settings) implements CustomPacketPayload {

    public static final Type<ValidateTunerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(com.doraamo.DoraAmo.MODID, "validate_tuner"));

    public static final StreamCodec<FriendlyByteBuf, ValidateTunerPayload> STREAM_CODEC = StreamCodec.of(
            ValidateTunerPayload::write,
            ValidateTunerPayload::read);

    private static void write(FriendlyByteBuf buf, ValidateTunerPayload msg) {
        buf.writeUtf(msg.settings.dimension == null ? "" : msg.settings.dimension);
        buf.writeEnum(msg.settings.mode);
        buf.writeUtf(msg.settings.biomeKey == null ? "minecraft:plains" : msg.settings.biomeKey);
        buf.writeUtf(msg.settings.structureName == null ? "Village" : msg.settings.structureName);
        buf.writeInt(msg.settings.x);
        buf.writeInt(msg.settings.y);
        buf.writeInt(msg.settings.z);
    }

    private static ValidateTunerPayload read(FriendlyByteBuf buf) {
        DestinationSettings settings = new DestinationSettings();
        settings.dimension = DimUtil.normalize(buf.readUtf());
        settings.mode = buf.readEnum(DestinationSettings.Mode.class);
        settings.biomeKey = buf.readUtf();
        settings.structureName = buf.readUtf();
        settings.x = buf.readInt();
        settings.y = buf.readInt();
        settings.z = buf.readInt();
        return new ValidateTunerPayload(settings);
    }

    public static void handle(ValidateTunerPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            DestinationSettings s = msg.settings;
            ServerLevel world = DimUtil.getLevel(player.getServer(), s.dimension);
            if (world == null) {
                PacketDistributor.sendToPlayer(player, ValidateResultPayload.notFound());
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
                PacketDistributor.sendToPlayer(player, ValidateResultPayload.notFound());
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
            PacketDistributor.sendToPlayer(player,
                    new ValidateResultPayload(true, placePos.getX(), placePos.getY(), placePos.getZ(), hazard.ordinal()));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
