package com.doraamo.network;

import com.doraamo.DoraAmo;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class PacketHandler {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(DoraAmo.MODID);

    private static int id;

    private PacketHandler() {
    }

    public static void init() {
        CHANNEL.registerMessage(PacketSaveTuner.Handler.class, PacketSaveTuner.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketValidateTuner.Handler.class, PacketValidateTuner.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketValidateResult.Handler.class, PacketValidateResult.class, id++, Side.CLIENT);
    }
}
