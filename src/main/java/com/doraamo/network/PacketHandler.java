package com.doraamo.network;

import com.doraamo.DoraAmo;
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

    public static void sendToPlayer(ServerPlayer player, PacketValidateResult msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketValidateResult.encode(msg, buf);
        ServerPlayNetworking.send(player, VALIDATE_RESULT, buf);
    }
}
