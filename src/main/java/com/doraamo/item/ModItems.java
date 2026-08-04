package com.doraamo.item;

import com.doraamo.DoraAmo;
import com.doraamo.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DoraAmo.MODID);

    public static final RegistryObject<Item> PORTAL_DOOR = ITEMS.register("portal_door",
            () -> new ItemPortalDoor(ModBlocks.PORTAL_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> PORTAL_TUNER = ITEMS.register("portal_tuner",
            () -> new ItemPortalTuner(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> OBSIDIAN_TURF = ITEMS.register("obsidian_turf",
            () -> new BlockItem(ModBlocks.OBSIDIAN_TURF.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
