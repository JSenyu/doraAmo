package com.doraamo.client;

import com.doraamo.config.catalog.DisplayCatalog;
import net.minecraft.client.Minecraft;

public final class ClientSetup {

    private ClientSetup() {
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
