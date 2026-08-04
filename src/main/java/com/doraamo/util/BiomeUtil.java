package com.doraamo.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BiomeUtil {

    private BiomeUtil() {
    }

    public static Registry<Biome> biomeRegistry(RegistryAccess access) {
        return access.registryOrThrow(Registries.BIOME);
    }

    public static List<Biome> allBiomes(RegistryAccess access) {
        List<Biome> list = new ArrayList<>();
        Registry<Biome> registry = biomeRegistry(access);
        for (ResourceLocation id : registry.keySet()) {
            registry.getHolder(ResourceKey.create(Registries.BIOME, id))
                    .ifPresent(holder -> list.add(holder.value()));
        }
        return list;
    }

    @Nullable
    public static ResourceLocation getKey(RegistryAccess access, Biome biome) {
        return biomeRegistry(access).getResourceKey(biome).map(ResourceKey::location).orElse(null);
    }

    @Nullable
    public static Biome getBiome(RegistryAccess access, ResourceLocation id) {
        return biomeRegistry(access).get(ResourceKey.create(Registries.BIOME, id));
    }
}
