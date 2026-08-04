package com.doraamo.client;

import com.doraamo.block.ModBlocks;
import com.doraamo.network.PacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.RenderType;

public class DoraAmoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PORTAL_DOOR, RenderType.translucent());
        PacketHandler.initClient();
        ClientTickEvents.END_CLIENT_TICK.register(ClientPortalOverlay::onClientTick);
        ClientSetup.refreshLanguagePreference();
    }
}
