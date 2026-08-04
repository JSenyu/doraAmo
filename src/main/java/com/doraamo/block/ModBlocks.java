package com.doraamo.block;

import com.doraamo.DoraAmo;
import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DoraAmo.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DoraAmo.MODID);

    public static final RegistryObject<BlockPortalDoor> PORTAL_DOOR = BLOCKS.register("portal_door",
            BlockPortalDoor::new);

    public static final RegistryObject<BlockObsidianTurf> OBSIDIAN_TURF = BLOCKS.register("obsidian_turf",
            () -> new BlockObsidianTurf(Block.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2F, 0.2F)
                    .noOcclusion()));

    public static final RegistryObject<BlockEntityType<TileEntityPortalDoor>> PORTAL_DOOR_TILE =
            BLOCK_ENTITIES.register("portal_door",
                    () -> BlockEntityType.Builder.of(TileEntityPortalDoor::new, PORTAL_DOOR.get()).build(null));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
