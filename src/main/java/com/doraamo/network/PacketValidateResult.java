package com.doraamo.network;

import com.doraamo.client.GuiPortalTuner;
import com.doraamo.portal.PortalDoorPlacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketValidateResult {

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

    public static void encode(PacketValidateResult msg, PacketBuffer buf) {
        buf.writeBoolean(msg.found);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        buf.writeInt(msg.z);
        buf.writeByte(msg.hazardOrdinal);
    }

    public static PacketValidateResult decode(PacketBuffer buf) {
        PacketValidateResult msg = new PacketValidateResult();
        msg.found = buf.readBoolean();
        msg.x = buf.readInt();
        msg.y = buf.readInt();
        msg.z = buf.readInt();
        msg.hazardOrdinal = buf.readByte() & 0xFF;
        return msg;
    }

    public static void handle(PacketValidateResult msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketValidateResult message) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof GuiPortalTuner) {
            PortalDoorPlacer.PlaceHazard[] values = PortalDoorPlacer.PlaceHazard.values();
            PortalDoorPlacer.PlaceHazard hazard = message.hazardOrdinal < values.length
                    ? values[message.hazardOrdinal]
                    : PortalDoorPlacer.PlaceHazard.WALL;
            ((GuiPortalTuner) screen).onValidateResult(
                    message.found, message.x, message.y, message.z, hazard);
        }
    }
}
