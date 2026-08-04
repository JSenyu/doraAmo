package com.doraamo.client;

import com.doraamo.network.PacketHandler;
import com.doraamo.network.SaveTunerPayload;
import com.doraamo.network.ValidateResultPayload;
import com.doraamo.network.ValidateTunerPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void init() {
        PacketHandler.registerPayloads();
        ClientPlayNetworking.registerGlobalReceiver(ValidateResultPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientHooks.handleValidateResult(
                    payload.found(), payload.x(), payload.y(), payload.z(), payload.hazardOrdinal()));
        });
    }

    public static void sendToServer(SaveTunerPayload msg) {
        ClientPlayNetworking.send(msg);
    }

    public static void sendToServer(ValidateTunerPayload msg) {
        ClientPlayNetworking.send(msg);
    }
}
