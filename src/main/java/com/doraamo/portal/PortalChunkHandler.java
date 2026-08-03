package com.doraamo.portal;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "doraamo")
public class PortalChunkHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) {
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
