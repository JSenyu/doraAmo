package com.doraamo.util;

import com.doraamo.DoraAmo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

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

    public static ResourceKey<Level> worldKey(String dim) {
        ResourceLocation loc = ResourceLocation.tryParse(normalize(dim));
        if (loc == null) {
            loc = new ResourceLocation(normalize(dim));
        }
        return ResourceKey.create(Registries.DIMENSION, loc);
    }

    @Nullable
    public static ServerLevel getLevel(MinecraftServer server, String dim) {
        if (isBlank(dim)) {
            return null;
        }
        return server.getLevel(worldKey(dim));
    }

    public static String levelKey(Level world) {
        return world.dimension().location().toString();
    }

    public static double coordinateScale(Level world) {
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
        for (ServerLevel level : server.getAllLevels()) {
            keys.add(level.dimension().location().toString());
        }
        return keys;
    }
}
