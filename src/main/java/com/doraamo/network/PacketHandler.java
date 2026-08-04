package com.doraamo.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class PacketHandler {

    private PacketHandler() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(SaveTunerPayload.TYPE, SaveTunerPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ValidateTunerPayload.TYPE, ValidateTunerPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ValidateResultPayload.TYPE, ValidateResultPayload.STREAM_CODEC);
    }

    public static void initServer() {
        registerPayloads();
        ServerPlayNetworking.registerGlobalReceiver(SaveTunerPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> SaveTunerPayload.handle(payload, (ServerPlayer) context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ValidateTunerPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ValidateTunerPayload.handle(payload, (ServerPlayer) context.player()));
        });
    }

    public static void initClient() {
        registerPayloads();
        ClientPlayNetworking.registerGlobalReceiver(ValidateResultPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ValidateResultPayload.handleClient(payload));
        });
    }

    public static void sendToServer(SaveTunerPayload msg) {
        ClientPlayNetworking.send(msg);
    }

    public static void sendToServer(ValidateTunerPayload msg) {
        ClientPlayNetworking.send(msg);
    }

    public static void sendToPlayer(ServerPlayer player, ValidateResultPayload msg) {
        ServerPlayNetworking.send(player, msg);
    }
}
