package com.doraamo.client;

import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.util.LangKeys;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.biome.Biome;

public final class BiomeNames {

    private BiomeNames() {
    }

    public static String localize(Biome biome) {
        if (biome == null) {
            return I18n.get(LangKeys.BIOME_UNKNOWN);
        }
        return DisplayCatalog.displayBiome(biome);
    }
}
