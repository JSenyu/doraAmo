package com.doraamo.network;

import com.doraamo.DoraAmo;
import com.doraamo.client.GuiPortalTuner;
import com.doraamo.portal.PortalDoorPlacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ValidateResultPayload(
        boolean found,
        int x,
        int y,
        int z,
        int hazardOrdinal) implements CustomPacketPayload {

    public static final Type<ValidateResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DoraAmo.MODID, "validate_result"));

    public static final StreamCodec<FriendlyByteBuf, ValidateResultPayload> STREAM_CODEC = StreamCodec.of(
            ValidateResultPayload::write,
            ValidateResultPayload::read);

    public static ValidateResultPayload notFound() {
        return new ValidateResultPayload(false, 0, 0, 0, PortalDoorPlacer.PlaceHazard.NONE.ordinal());
    }

    private static void write(FriendlyByteBuf buf, ValidateResultPayload msg) {
        buf.writeBoolean(msg.found);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        buf.writeInt(msg.z);
        buf.writeByte(msg.hazardOrdinal);
    }

    private static ValidateResultPayload read(FriendlyByteBuf buf) {
        return new ValidateResultPayload(
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readByte() & 0xFF);
    }

    public static void handleClient(ValidateResultPayload message) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof GuiPortalTuner tuner) {
            PortalDoorPlacer.PlaceHazard[] values = PortalDoorPlacer.PlaceHazard.values();
            PortalDoorPlacer.PlaceHazard hazard = message.hazardOrdinal < values.length
                    ? values[message.hazardOrdinal]
                    : PortalDoorPlacer.PlaceHazard.WALL;
            tuner.onValidateResult(message.found, message.x, message.y, message.z, hazard);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
