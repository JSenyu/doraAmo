package com.doraamo.destination;

import com.doraamo.util.DimUtil;
import net.minecraft.nbt.CompoundTag;

public class DestinationSettings {

    public enum Mode {
        SCALED,
        COORDS,
        BIOME,
        STRUCTURE
    }

    public String dimension = DimUtil.OVERWORLD;
    public Mode mode = Mode.SCALED;
    public String biomeKey = "minecraft:plains";
    public String structureName = "Village";
    public int x;
    public int y = 64;
    public int z;
    public boolean forceUnsafe;

    public CompoundTag writeToNBT(CompoundTag tag) {
        tag.putString("Dim", dimension == null ? "" : dimension);
        tag.putString("Mode", mode.name());
        tag.putString("Biome", biomeKey == null ? "minecraft:plains" : biomeKey);
        tag.putString("Structure", structureName == null ? "" : structureName);
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putInt("Z", z);
        tag.putBoolean("Force", forceUnsafe);
        return tag;
    }

    public static DestinationSettings fromNBT(CompoundTag tag) {
        DestinationSettings s = new DestinationSettings();
        if (tag == null) {
            return s;
        }
        if (tag.contains("Dim")) {
            if (tag.get("Dim").getId() == CompoundTag.TAG_STRING) {
                s.dimension = DimUtil.normalize(tag.getString("Dim"));
            } else {
                s.dimension = DimUtil.fromLegacyInt(tag.getInt("Dim"));
            }
        } else if (tag.contains("TargetDim")) {
            int dim = tag.getInt("TargetDim");
            s.dimension = dim == Integer.MIN_VALUE ? "" : DimUtil.fromLegacyInt(dim);
        }
        try {
            s.mode = Mode.valueOf(tag.getString("Mode"));
        } catch (Exception e) {
            s.mode = Mode.SCALED;
        }
        if (tag.contains("Biome")) {
            if (tag.get("Biome").getId() == CompoundTag.TAG_STRING) {
                s.biomeKey = tag.getString("Biome");
            } else {
                int biomeId = tag.getInt("Biome");
                s.biomeKey = biomeKeyFromLegacyId(biomeId);
            }
        }
        s.structureName = tag.getString("Structure");
        if (s.structureName == null || s.structureName.isEmpty()) {
            s.structureName = "Village";
        }
        s.x = tag.getInt("X");
        s.y = tag.contains("Y") ? tag.getInt("Y") : 64;
        s.z = tag.getInt("Z");
        s.forceUnsafe = tag.getBoolean("Force");
        return s;
    }

    public static DestinationSettings scaled(String dimension) {
        DestinationSettings s = new DestinationSettings();
        s.dimension = DimUtil.normalize(dimension);
        s.mode = Mode.SCALED;
        return s;
    }

    public net.minecraft.core.BlockPos getCoordPos() {
        return new net.minecraft.core.BlockPos(x, y, z);
    }

    public DestinationSettings copy() {
        return fromNBT(writeToNBT(new CompoundTag()));
    }

    private static String biomeKeyFromLegacyId(int id) {
        return switch (id) {
            case 0 -> "minecraft:plains";
            case 1 -> "minecraft:desert";
            case 2 -> "minecraft:mountains";
            case 3 -> "minecraft:forest";
            case 4 -> "minecraft:taiga";
            case 5 -> "minecraft:swamp";
            case 6 -> "minecraft:river";
            case 7 -> "minecraft:nether_wastes";
            case 8 -> "minecraft:the_end";
            default -> "minecraft:plains";
        };
    }
}
