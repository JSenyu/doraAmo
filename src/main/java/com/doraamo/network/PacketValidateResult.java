package com.doraamo.network;

import com.doraamo.client.ClientHooks;
import com.doraamo.portal.PortalDoorPlacer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

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

    public static void encode(PacketValidateResult msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.found);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        buf.writeInt(msg.z);
        buf.writeByte(msg.hazardOrdinal);
    }

    public static PacketValidateResult decode(FriendlyByteBuf buf) {
        PacketValidateResult msg = new PacketValidateResult();
        msg.found = buf.readBoolean();
        msg.x = buf.readInt();
        msg.y = buf.readInt();
        msg.z = buf.readInt();
        msg.hazardOrdinal = buf.readByte() & 0xFF;
        return msg;
    }

    public static void handle(PacketValidateResult msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHooks.handleValidateResult(msg.found, msg.x, msg.y, msg.z, msg.hazardOrdinal)));
        ctx.get().setPacketHandled(true);
    }
}
