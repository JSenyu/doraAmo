package com.doraamo.network;

import com.doraamo.DoraAmo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = DoraAmo.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModPayloads {

    private static final String PROTOCOL = "1";

    private ModPayloads() {
    }

    public static void init() {
        // Payload handlers register via RegisterPayloadHandlersEvent.
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToServer(
                SaveTunerPayload.TYPE,
                SaveTunerPayload.STREAM_CODEC,
                SaveTunerPayload::handle);
        registrar.playToServer(
                ValidateTunerPayload.TYPE,
                ValidateTunerPayload.STREAM_CODEC,
                ValidateTunerPayload::handle);
        registrar.playToClient(
                ValidateResultPayload.TYPE,
                ValidateResultPayload.STREAM_CODEC,
                ValidateResultPayload::handle);
    }
}
