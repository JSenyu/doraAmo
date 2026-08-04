package com.doraamo.item;

import com.doraamo.DoraAmo;
import com.doraamo.block.ModBlocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DoraAmo.MODID);

    public static final RegistryObject<Item> PORTAL_DOOR = ITEMS.register("portal_door",
            () -> new ItemPortalDoor(ModBlocks.PORTAL_DOOR.get(),
                    new Item.Properties().tab(ItemGroup.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> PORTAL_TUNER = ITEMS.register("portal_tuner",
            () -> new ItemPortalTuner(new Item.Properties().tab(ItemGroup.TAB_TOOLS).stacksTo(1)));

    public static final RegistryObject<Item> OBSIDIAN_TURF = ITEMS.register("obsidian_turf",
            () -> new BlockItem(ModBlocks.OBSIDIAN_TURF.get(),
                    new Item.Properties().tab(ItemGroup.TAB_BUILDING_BLOCKS)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
