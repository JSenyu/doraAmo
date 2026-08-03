package com.doraamo.registry;

import com.doraamo.DoraAmo;
import com.doraamo.item.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = DoraAmo.MODID, value = Side.CLIENT)
public class ClientRegistries {

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ModItems.PORTAL_DOOR, 0,
                new ModelResourceLocation(DoraAmo.MODID + ":portal_door", "inventory"));
        ModelLoader.setCustomModelResourceLocation(ModItems.PORTAL_TUNER, 0,
                new ModelResourceLocation(DoraAmo.MODID + ":portal_tuner", "inventory"));
        ModelLoader.setCustomModelResourceLocation(ModItems.OBSIDIAN_TURF, 0,
                new ModelResourceLocation(DoraAmo.MODID + ":obsidian_turf", "inventory"));
    }
}
