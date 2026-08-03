package com.doraamo.network;

import com.doraamo.destination.DestinationLocator;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalDoorPlacer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketValidateTuner implements IMessage {

    private DestinationSettings settings = new DestinationSettings();

    public PacketValidateTuner() {
    }

    public PacketValidateTuner(DestinationSettings settings) {
        this.settings = settings;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        settings.dimensionId = buf.readInt();
        int modeOrd = buf.readByte() & 0xFF;
        DestinationSettings.Mode[] modes = DestinationSettings.Mode.values();
        settings.mode = modeOrd < modes.length ? modes[modeOrd] : DestinationSettings.Mode.COORDS;
        settings.biomeId = buf.readInt();
        settings.structureName = ByteBufUtils.readUTF8String(buf);
        settings.x = buf.readInt();
        settings.y = buf.readInt();
        settings.z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(settings.dimensionId);
        buf.writeByte(settings.mode.ordinal());
        buf.writeInt(settings.biomeId);
        ByteBufUtils.writeUTF8String(buf, settings.structureName == null ? "Village" : settings.structureName);
        buf.writeInt(settings.x);
        buf.writeInt(settings.y);
        buf.writeInt(settings.z);
    }

    public static class Handler implements IMessageHandler<PacketValidateTuner, IMessage> {
        @Override
        public IMessage onMessage(final PacketValidateTuner message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    DestinationSettings s = message.settings;
                    if (!DimensionManager.isDimensionRegistered(s.dimensionId)) {
                        PacketHandler.CHANNEL.sendTo(PacketValidateResult.notFound(), player);
                        return;
                    }
                    WorldServer world = player.getServer().getWorld(s.dimensionId);
                    if (world == null) {
                        PacketHandler.CHANNEL.sendTo(PacketValidateResult.notFound(), player);
                        return;
                    }
                    BlockPos near = new BlockPos(player.posX, player.posY, player.posZ);
                    if (player.dimension != s.dimensionId) {
                        double scale = player.world.provider.getMovementFactor()
                                / world.provider.getMovementFactor();
                        int y = (int) player.posY;
                        if (s.dimensionId == -1) {
                            y = Math.max(32, Math.min(120, y));
                        }
                        near = new BlockPos(
                                (int) Math.floor(player.posX * scale),
                                y,
                                (int) Math.floor(player.posZ * scale));
                    } else if (s.dimensionId == -1) {
                        near = new BlockPos(near.getX(),
                                Math.max(32, Math.min(120, near.getY())), near.getZ());
                    }
                    DestinationLocator.ValidateResult result = DestinationLocator.validate(world, near, s);
                    if (!result.found || result.pos == null) {
                        PacketHandler.CHANNEL.sendTo(PacketValidateResult.notFound(), player);
                        return;
                    }
                    BlockPos placePos = result.pos;
                    if (s.mode == DestinationSettings.Mode.BIOME
                            || (s.mode == DestinationSettings.Mode.STRUCTURE
                            && !DestinationLocator.isUndergroundStructure(s.structureName)
                            && !DestinationLocator.isWaterOrFloatStructure(s.structureName))) {
                        BlockPos safe = PortalDoorPlacer.findSafeDoorPos(world, result.pos,
                                PortalDoorPlacer.SAFE_SEARCH_RADIUS);
                        if (safe != null) {
                            placePos = safe;
                        }
                    }
                    PortalDoorPlacer.PlaceHazard hazard = PortalDoorPlacer.diagnose(world, placePos);
                    PacketHandler.CHANNEL.sendTo(new PacketValidateResult(true, placePos.getX(),
                            placePos.getY(), placePos.getZ(), hazard.ordinal()), player);
                }
            });
            return null;
        }
    }
}
