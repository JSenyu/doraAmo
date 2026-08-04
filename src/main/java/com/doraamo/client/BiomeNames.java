package com.doraamo.client;

import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.util.LangKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BiomeNames {

    private BiomeNames() {
    }

    public static String localize(Biome biome) {
        if (biome == null) {
            return I18n.get(LangKeys.BIOME_UNKNOWN);
        }
        RegistryAccess access = Minecraft.getInstance().getConnection().registryAccess();
        return DisplayCatalog.displayBiome(access, biome);
    }
}
