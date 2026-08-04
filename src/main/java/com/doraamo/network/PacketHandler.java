package com.doraamo.network;

import com.doraamo.DoraAmo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PacketHandler {

    public static final ResourceLocation SAVE_TUNER = new ResourceLocation(DoraAmo.MODID, "save_tuner");
    public static final ResourceLocation VALIDATE_TUNER = new ResourceLocation(DoraAmo.MODID, "validate_tuner");
    public static final ResourceLocation VALIDATE_RESULT = new ResourceLocation(DoraAmo.MODID, "validate_result");

    private PacketHandler() {
    }

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(SAVE_TUNER, (server, player, handler, buf, responseSender) -> {
            PacketSaveTuner msg = PacketSaveTuner.decode(buf);
            server.execute(() -> PacketSaveTuner.handle(msg, player));
        });
        ServerPlayNetworking.registerGlobalReceiver(VALIDATE_TUNER, (server, player, handler, buf, responseSender) -> {
            PacketValidateTuner msg = PacketValidateTuner.decode(buf);
            server.execute(() -> PacketValidateTuner.handle(msg, player));
        });
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(VALIDATE_RESULT, (client, handler, buf, responseSender) -> {
            PacketValidateResult msg = PacketValidateResult.decode(buf);
            client.execute(() -> PacketValidateResult.handleClient(msg));
        });
    }

    public static void sendToServer(PacketSaveTuner msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketSaveTuner.encode(msg, buf);
        ClientPlayNetworking.send(SAVE_TUNER, buf);
    }

    public static void sendToServer(PacketValidateTuner msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketValidateTuner.encode(msg, buf);
        ClientPlayNetworking.send(VALIDATE_TUNER, buf);
    }

    public static void sendToPlayer(ServerPlayer player, PacketValidateResult msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketValidateResult.encode(msg, buf);
        ServerPlayNetworking.send(player, VALIDATE_RESULT, buf);
    }
}
