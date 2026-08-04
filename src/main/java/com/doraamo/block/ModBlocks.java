package com.doraamo.block;

import com.doraamo.DoraAmo;
import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(DoraAmo.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DoraAmo.MODID);

    public static final DeferredBlock<BlockPortalDoor> PORTAL_DOOR = BLOCKS.register("portal_door",
            () -> new BlockPortalDoor(Block.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.5F, 3.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 14)
                    .noOcclusion()));

    public static final DeferredBlock<BlockObsidianTurf> OBSIDIAN_TURF = BLOCKS.register("obsidian_turf",
            () -> new BlockObsidianTurf(Block.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2F, 0.2F)
                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntityPortalDoor>> PORTAL_DOOR_TILE =
            BLOCK_ENTITIES.register("portal_door",
                    () -> BlockEntityType.Builder.of(TileEntityPortalDoor::new, PORTAL_DOOR.get()).build(null));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
