package com.doraamo.network;

import com.doraamo.DoraAmo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DoraAmo.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id;

    private PacketHandler() {
    }

    public static void init() {
        CHANNEL.registerMessage(id++, PacketSaveTuner.class, PacketSaveTuner::encode, PacketSaveTuner::decode, PacketSaveTuner::handle);
        CHANNEL.registerMessage(id++, PacketValidateTuner.class, PacketValidateTuner::encode, PacketValidateTuner::decode, PacketValidateTuner::handle);
        CHANNEL.registerMessage(id++, PacketValidateResult.class, PacketValidateResult::encode, PacketValidateResult::decode, PacketValidateResult::handle);
    }
}
