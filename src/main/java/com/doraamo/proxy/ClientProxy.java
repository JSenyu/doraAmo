package com.doraamo.proxy;

import com.doraamo.client.GuiPortalTuner;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.destination.DestinationSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Language;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientProxy extends CommonProxy {

    @SideOnly(Side.CLIENT)
    @Override
    public void postInit() {
        refreshLanguagePreference();
    }

    @SideOnly(Side.CLIENT)
    public static void refreshLanguagePreference() {
        try {
            Language lang = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage();
            String code = lang != null ? lang.getLanguageCode() : "en_us";
            DisplayCatalog.setPreferChinese(code != null && code.toLowerCase().startsWith("zh"));
        } catch (Throwable ignored) {
            DisplayCatalog.setPreferChinese(false);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void openTunerGui(EnumHand hand, ItemStack stack, BlockPos portalPos,
                             DestinationSettings draft, boolean hasExistingBinding) {
        refreshLanguagePreference();
        Minecraft.getMinecraft().displayGuiScreen(
                new GuiPortalTuner(hand, draft, portalPos, hasExistingBinding));
    }
}
