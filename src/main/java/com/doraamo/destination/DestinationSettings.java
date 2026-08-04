package com.doraamo.destination;

import com.doraamo.util.DimUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

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

    public CompoundNBT writeToNBT(CompoundNBT tag) {
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

    public static DestinationSettings fromNBT(CompoundNBT tag) {
        DestinationSettings s = new DestinationSettings();
        if (tag == null) {
            return s;
        }
        if (tag.contains("Dim")) {
            if (tag.get("Dim").getId() == 8) {
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
            if (tag.get("Biome").getId() == 8) {
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

    public BlockPos getCoordPos() {
        return new BlockPos(x, y, z);
    }

    public DestinationSettings copy() {
        return fromNBT(writeToNBT(new CompoundNBT()));
    }

    private static String biomeKeyFromLegacyId(int id) {
        int index = 0;
        for (net.minecraft.world.biome.Biome biome : ForgeRegistries.BIOMES.getValues()) {
            if (index++ == id) {
                ResourceLocation key = ForgeRegistries.BIOMES.getKey(biome);
                return key != null ? key.toString() : "minecraft:plains";
            }
        }
        return "minecraft:plains";
    }
}
