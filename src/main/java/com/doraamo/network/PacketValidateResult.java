package com.doraamo.network;

import com.doraamo.client.GuiPortalTuner;
import com.doraamo.portal.PortalDoorPlacer;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketValidateResult implements IMessage {

    private boolean found;
    private int x;
    private int y;
    private int z;
    private int hazardOrdinal;

    public PacketValidateResult() {
    }

    public PacketValidateResult(boolean found, int x, int y, int z, int hazardOrdinal) {
        this.found = found;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hazardOrdinal = hazardOrdinal;
    }

    public static PacketValidateResult notFound() {
        return new PacketValidateResult(false, 0, 0, 0, PortalDoorPlacer.PlaceHazard.NONE.ordinal());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        found = buf.readBoolean();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        hazardOrdinal = buf.readByte() & 0xFF;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(found);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(hazardOrdinal);
    }

    public static class Handler implements IMessageHandler<PacketValidateResult, IMessage> {
        @Override
        public IMessage onMessage(final PacketValidateResult message, final MessageContext ctx) {
            handleClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static void handleClient(final PacketValidateResult message) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                    if (screen instanceof GuiPortalTuner) {
                        PortalDoorPlacer.PlaceHazard[] values = PortalDoorPlacer.PlaceHazard.values();
                        PortalDoorPlacer.PlaceHazard hazard = message.hazardOrdinal < values.length
                                ? values[message.hazardOrdinal]
                                : PortalDoorPlacer.PlaceHazard.WALL;
                        ((GuiPortalTuner) screen).onValidateResult(
                                message.found, message.x, message.y, message.z, hazard);
                    }
                }
            });
        }
    }
}
