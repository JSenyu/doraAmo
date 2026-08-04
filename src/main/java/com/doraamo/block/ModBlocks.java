package com.doraamo.block;

import com.doraamo.DoraAmo;
import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DoraAmo.MODID);

    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, DoraAmo.MODID);

    public static final RegistryObject<BlockPortalDoor> PORTAL_DOOR = BLOCKS.register("portal_door",
            BlockPortalDoor::new);

    public static final RegistryObject<BlockObsidianTurf> OBSIDIAN_TURF = BLOCKS.register("obsidian_turf",
            () -> new BlockObsidianTurf(Block.Properties.of(Material.GRASS, MaterialColor.COLOR_PURPLE)
                    .strength(0.2F, 0.2F)
                    .noOcclusion()));

    public static final RegistryObject<TileEntityType<TileEntityPortalDoor>> PORTAL_DOOR_TILE =
            TILE_ENTITIES.register("portal_door",
                    () -> TileEntityType.Builder.of(TileEntityPortalDoor::new, PORTAL_DOOR.get()).build(null));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        TILE_ENTITIES.register(modBus);
    }
}
