package com.doraamo.portal;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "doraamo")
public class PortalChunkHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof World)) {
            return;
        }
        World world = (World) event.getWorld();
        if (world.isClientSide) {
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
