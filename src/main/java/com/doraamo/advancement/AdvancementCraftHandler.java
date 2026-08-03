package com.doraamo.advancement;

import com.doraamo.DoraAmo;
import com.doraamo.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Grants craft-only advancements (impossible criteria) when the matching item is crafted.
 */
@Mod.EventBusSubscriber(modid = DoraAmo.MODID)
public final class AdvancementCraftHandler {

    private AdvancementCraftHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Item item = event.crafting.getItem();
        if (item == ModItems.PORTAL_DOOR) {
            grant(player, "anydoor", "crafted");
        } else if (item == ModItems.PORTAL_TUNER) {
            grant(player, "true_anydoor", "crafted");
        }
    }

    private static void grant(EntityPlayerMP player, String path, String criterion) {
        if (player.getServer() == null) {
            return;
        }
        Advancement adv = player.getServer().getAdvancementManager()
                .getAdvancement(new ResourceLocation(DoraAmo.MODID, path));
        if (adv == null) {
            return;
        }
        PlayerAdvancements pa = player.getAdvancements();
        pa.grantCriterion(adv, criterion);
    }
}
