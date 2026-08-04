package com.doraamo.client;

import com.doraamo.network.PacketHandler;
import com.doraamo.network.PacketSaveTuner;
import com.doraamo.network.PacketValidateResult;
import com.doraamo.network.PacketValidateTuner;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

@Environment(EnvType.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.VALIDATE_RESULT, (client, handler, buf, responseSender) -> {
            PacketValidateResult msg = PacketValidateResult.decode(buf);
            client.execute(() -> ClientHooks.handleValidateResult(
                    msg.found, msg.x, msg.y, msg.z, msg.hazardOrdinal));
        });
    }

    public static void sendToServer(PacketSaveTuner msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketSaveTuner.encode(msg, buf);
        ClientPlayNetworking.send(PacketHandler.SAVE_TUNER, buf);
    }

    public static void sendToServer(PacketValidateTuner msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketValidateTuner.encode(msg, buf);
        ClientPlayNetworking.send(PacketHandler.VALIDATE_TUNER, buf);
    }
}
