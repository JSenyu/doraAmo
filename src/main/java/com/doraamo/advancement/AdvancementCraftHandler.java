package com.doraamo.advancement;

import com.doraamo.DoraAmo;
import com.doraamo.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DoraAmo.MODID)
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
        Advancement adv = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(DoraAmo.MODID, path));
        if (adv == null) {
            return;
        }
        PlayerAdvancements pa = player.getAdvancements();
        pa.award(adv, criterion);
    }
}
