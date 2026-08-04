package com.doraamo.item;

import com.doraamo.DoraAmo;
import com.doraamo.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(DoraAmo.MODID);

    public static final DeferredItem<ItemPortalDoor> PORTAL_DOOR = ITEMS.register("portal_door",
            () -> new ItemPortalDoor(ModBlocks.PORTAL_DOOR.get(), new Item.Properties()));

    public static final DeferredItem<ItemPortalTuner> PORTAL_TUNER = ITEMS.register("portal_tuner",
            () -> new ItemPortalTuner(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> OBSIDIAN_TURF = ITEMS.register("obsidian_turf",
            () -> new BlockItem(ModBlocks.OBSIDIAN_TURF.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
