package com.doraamo.client;

import com.doraamo.DoraAmo;
import com.doraamo.block.ModBlocks;
import com.doraamo.config.catalog.DisplayCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Language;
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
                net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
                        ModBlocks.PORTAL_DOOR.get(), net.minecraft.client.renderer.RenderType.translucent()));
        refreshLanguagePreference();
    }

    public static void refreshLanguagePreference() {
        try {
            Language lang = Minecraft.getInstance().getLanguageManager().getSelected();
            String code = lang != null ? lang.getCode() : "en_us";
            DisplayCatalog.setPreferChinese(code != null && code.toLowerCase().startsWith("zh"));
        } catch (Throwable ignored) {
            DisplayCatalog.setPreferChinese(false);
        }
    }
}
