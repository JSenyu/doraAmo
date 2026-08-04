package com.doraamo.util;

import com.doraamo.DoraAmo;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RegistryHelper {

    private RegistryHelper() {
    }

    public static Registry<Biome> biomeRegistry() {
        MinecraftServer server = DoraAmo.getServer();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registries.BIOME);
        }
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                return level.registryAccess().registryOrThrow(Registries.BIOME);
            }
        }
        throw new IllegalStateException("Biome registry not available");
    }

    public static List<Biome> allBiomes() {
        List<Biome> list = new ArrayList<>();
        for (Biome biome : biomeRegistry()) {
            if (biome != null) {
                list.add(biome);
            }
        }
        return list;
    }

    public static ResourceLocation biomeKey(Biome biome) {
        return biomeRegistry().getKey(biome);
    }

    public static Biome biomeByKey(ResourceLocation location) {
        return biomeRegistry().get(location);
    }

    public static Set<ResourceLocation> biomeKeys() {
        return biomeRegistry().keySet();
    }
}
