package com.doraamo.item;

import com.doraamo.DoraAmo;
import com.doraamo.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final Item PORTAL_DOOR = registerItem("portal_door",
            new ItemPortalDoor(ModBlocks.PORTAL_DOOR, new Item.Properties()));

    public static final Item PORTAL_TUNER = registerItem("portal_tuner",
            new ItemPortalTuner(new Item.Properties().stacksTo(1)));

    public static final Item OBSIDIAN_TURF = registerItem("obsidian_turf",
            new BlockItem(ModBlocks.OBSIDIAN_TURF, new Item.Properties()));

    private ModItems() {
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(DoraAmo.MODID, name), item);
    }

    public static void register() {
        // Items registered via static initializers above.
    }
}
