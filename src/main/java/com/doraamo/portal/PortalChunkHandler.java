package com.doraamo.portal;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = "doraamo")
public class PortalChunkHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level world)) {
            return;
        }
        if (world.isClientSide()) {
            return;
        }
        PortalNetworkData data = PortalNetworkData.get();
        if (data == null) {
            return;
        }
        ChunkPos pos = event.getChunk().getPos();
        data.onChunkLoad(world, pos);
    }
}
