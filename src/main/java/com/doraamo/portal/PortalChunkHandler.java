package com.doraamo.portal;

import com.doraamo.DoraAmo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class PortalChunkHandler {

    private PortalChunkHandler() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.isClientSide()) {
                return;
            }
            PortalNetworkData data = PortalNetworkData.get();
            if (data == null) {
                return;
            }
            data.onChunkLoad(world, chunk.getPos());
        });
    }
}
