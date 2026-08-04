package com.doraamo.portal;

import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;
import java.util.Locale;
import java.util.Objects;

/** Lower-half portal position in a dimension. */
public final class PortalRef {

    /** Dimension as {@link net.minecraft.util.ResourceLocation} string. */
    public final String dim;
    public final BlockPos pos;

    public PortalRef(String dim, BlockPos pos) {
        this.dim = DimUtil.normalize(dim);
        this.pos = pos.immutable();
    }

    public PortalRef(int legacyDim, BlockPos pos) {
        this(DimUtil.fromLegacyInt(legacyDim), pos);
    }

    public String key() {
        return dim + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    public static PortalRef parse(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Bad portal key: " + key);
        }
        if (key.contains("|")) {
            String[] parts = key.split("\\|", 4);
            if (parts.length != 4) {
                throw new IllegalArgumentException("Bad portal key: " + key);
            }
            return new PortalRef(parts[0],
                    new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        }
        // Legacy 1.12 format: dim:x:y:z (dim is int)
        String[] legacy = key.split(":");
        if (legacy.length == 4) {
            try {
                int dim = Integer.parseInt(legacy[0]);
                return new PortalRef(dim, new BlockPos(
                        Integer.parseInt(legacy[1]), Integer.parseInt(legacy[2]), Integer.parseInt(legacy[3])));
            } catch (NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("Bad portal key: " + key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalRef)) return false;
        PortalRef portalRef = (PortalRef) o;
        return dim.equals(portalRef.dim) && pos.equals(portalRef.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dim, pos);
    }

    @Override
    public String toString() {
        return key();
    }
}
