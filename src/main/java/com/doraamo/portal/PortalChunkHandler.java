package com.doraamo.portal;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "doraamo")
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
