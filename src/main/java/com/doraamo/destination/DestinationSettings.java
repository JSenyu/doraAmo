package com.doraamo.destination;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/** Destination configuration stored on tuner item and main portal TE. */
public class DestinationSettings {

    public enum Mode {
        /** Use portal-block coordinate scaling only (OW↔Nether movement factors). */
        SCALED,
        COORDS,
        BIOME,
        STRUCTURE
    }

    public int dimensionId = 0;
    public Mode mode = Mode.SCALED;
    public int biomeId = 1;
    public String structureName = "Village";
    public int x;
    public int y = 64;
    public int z;
    /** When true, teleport may force-clear cells to place the sub door. */
    public boolean forceUnsafe;

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Dim", dimensionId);
        tag.setString("Mode", mode.name());
        tag.setInteger("Biome", biomeId);
        tag.setString("Structure", structureName == null ? "" : structureName);
        tag.setInteger("X", x);
        tag.setInteger("Y", y);
        tag.setInteger("Z", z);
        tag.setBoolean("Force", forceUnsafe);
        return tag;
    }

    public static DestinationSettings fromNBT(NBTTagCompound tag) {
        DestinationSettings s = new DestinationSettings();
        if (tag == null) {
            return s;
        }
        s.dimensionId = tag.getInteger("Dim");
        try {
            s.mode = Mode.valueOf(tag.getString("Mode"));
        } catch (Exception e) {
            s.mode = Mode.SCALED;
        }
        s.biomeId = tag.getInteger("Biome");
        s.structureName = tag.getString("Structure");
        if (s.structureName == null || s.structureName.isEmpty()) {
            s.structureName = "Village";
        }
        s.x = tag.getInteger("X");
        s.y = tag.hasKey("Y") ? tag.getInteger("Y") : 64;
        s.z = tag.getInteger("Z");
        s.forceUnsafe = tag.getBoolean("Force");
        return s;
    }

    /** Legacy: only a dimension id was stored on the TE. */
    public static DestinationSettings scaled(int dimensionId) {
        DestinationSettings s = new DestinationSettings();
        s.dimensionId = dimensionId;
        s.mode = Mode.SCALED;
        return s;
    }

    public BlockPos getCoordPos() {
        return new BlockPos(x, y, z);
    }

    public DestinationSettings copy() {
        return fromNBT(writeToNBT(new NBTTagCompound()));
    }
}
