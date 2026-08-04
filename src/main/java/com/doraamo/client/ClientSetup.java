package com.doraamo.client;

import com.doraamo.DoraAmo;
import com.doraamo.block.ModBlocks;
import com.doraamo.config.catalog.DisplayCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DoraAmo.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PORTAL_DOOR.get(), RenderType.translucent()));
        refreshLanguagePreference();
    }

    public static void refreshLanguagePreference() {
        try {
            String code = Minecraft.getInstance().getLanguageManager().getSelected();
            DisplayCatalog.setPreferChinese(code != null && code.toLowerCase().startsWith("zh"));
        } catch (Throwable ignored) {
            DisplayCatalog.setPreferChinese(false);
        }
    }
}
