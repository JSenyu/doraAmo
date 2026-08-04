package com.doraamo.util;

import com.doraamo.DoraAmo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Dimension keys as {@link ResourceLocation} strings (1.16+). */
public final class DimUtil {

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";

    private DimUtil() {
    }

    public static boolean isBlank(@Nullable String dim) {
        return dim == null || dim.isEmpty();
    }

    public static String normalize(@Nullable String dim) {
        if (isBlank(dim)) {
            return DoraAmo.BLANK_DIMENSION;
        }
        if (dim.contains(":")) {
            return dim;
        }
        return "minecraft:" + dim;
    }

    public static String fromLegacyInt(int dim) {
        switch (dim) {
            case 0:
                return OVERWORLD;
            case -1:
                return NETHER;
            case 1:
                return END;
            default:
                return Integer.toString(dim);
        }
    }

    public static int toLegacyInt(String dim) {
        if (OVERWORLD.equals(dim)) {
            return 0;
        }
        if (NETHER.equals(dim)) {
            return -1;
        }
        if (END.equals(dim)) {
            return 1;
        }
        try {
            return Integer.parseInt(dim);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static RegistryKey<World> worldKey(String dim) {
        ResourceLocation loc = new ResourceLocation(normalize(dim));
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, loc);
    }

    @Nullable
    public static ServerWorld getLevel(MinecraftServer server, String dim) {
        if (isBlank(dim)) {
            return null;
        }
        return server.getLevel(worldKey(dim));
    }

    public static String levelKey(World world) {
        return world.dimension().location().toString();
    }

    public static double coordinateScale(World world) {
        return world.dimensionType().coordinateScale();
    }

    public static boolean isNether(String dim) {
        return NETHER.equals(normalize(dim));
    }

    public static boolean isEnd(String dim) {
        return END.equals(normalize(dim));
    }

    public static List<String> allLevelKeys(MinecraftServer server) {
        List<String> keys = new ArrayList<>();
        for (ServerWorld level : server.getAllLevels()) {
            keys.add(level.dimension().location().toString());
        }
        return keys;
    }
}
