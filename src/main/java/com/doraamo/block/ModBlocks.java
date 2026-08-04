package com.doraamo.block;

import com.doraamo.DoraAmo;
import com.doraamo.tileentity.TileEntityPortalDoor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

    public static final BlockPortalDoor PORTAL_DOOR = registerBlock("portal_door", new BlockPortalDoor(Block.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(0.5F, 3.0F)
            .sound(SoundType.GLASS)
            .lightLevel(state -> 14)
            .noOcclusion()));

    public static final BlockObsidianTurf OBSIDIAN_TURF = registerBlock("obsidian_turf",
            new BlockObsidianTurf(Block.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2F, 0.2F)
                    .noOcclusion()));

    public static BlockEntityType<TileEntityPortalDoor> PORTAL_DOOR_TILE;

    private ModBlocks() {
    }

    private static <T extends Block> T registerBlock(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(DoraAmo.MODID, name), block);
    }

    public static void register() {
        PORTAL_DOOR_TILE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(DoraAmo.MODID, "portal_door"),
                BlockEntityType.Builder.of(TileEntityPortalDoor::new, PORTAL_DOOR).build(null));
    }
}
