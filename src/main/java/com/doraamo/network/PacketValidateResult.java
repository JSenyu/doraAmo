package com.doraamo.network;

import com.doraamo.portal.PortalDoorPlacer;
import net.minecraft.network.FriendlyByteBuf;

public class PacketValidateResult {

    public boolean found;
    public int x;
    public int y;
    public int z;
    public int hazardOrdinal;

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
}
