package com.doraamo.block;

import com.doraamo.DoraAmo;
import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static BlockPortalDoor PORTAL_DOOR;
    public static BlockObsidianTurf OBSIDIAN_TURF;

    private ModBlocks() {
    }

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(TileEntityPortalDoor.class,
                new ResourceLocation(DoraAmo.MODID, "portal_door"));
    }
}
