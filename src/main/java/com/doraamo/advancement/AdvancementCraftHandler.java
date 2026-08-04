package com.doraamo.advancement;

import com.doraamo.DoraAmo;
import com.doraamo.item.ModItems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = DoraAmo.MODID)
public final class AdvancementCraftHandler {

    private AdvancementCraftHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Item item = event.getCrafting().getItem();
        if (item == ModItems.PORTAL_DOOR.get()) {
            grant(player, "anydoor", "crafted");
        } else if (item == ModItems.PORTAL_TUNER.get()) {
            grant(player, "true_anydoor", "crafted");
        }
    }

    private static void grant(ServerPlayer player, String path, String criterion) {
        if (player.getServer() == null) {
            return;
        }
        AdvancementHolder adv = player.getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(DoraAmo.MODID, path));
        if (adv == null) {
            return;
        }
        player.getAdvancements().award(adv, criterion);
    }
}
