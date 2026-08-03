package com.doraamo.client;

import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.util.LangKeys;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class BiomeNames {

    private BiomeNames() {
    }

    public static String localize(Biome biome) {
        if (biome == null) {
            return I18n.format(LangKeys.BIOME_UNKNOWN);
        }
        return DisplayCatalog.displayBiome(biome);
    }
}
