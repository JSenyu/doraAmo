package com.doraamo.portal;

import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Objects;

/** Lower-half portal position in a dimension. */
public final class PortalRef {

    public final int dim;
    public final BlockPos pos;

    public PortalRef(int dim, BlockPos pos) {
        this.dim = dim;
        this.pos = pos.toImmutable();
    }

    public String key() {
        return dim + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    public static PortalRef parse(String key) {
        String[] p = key.split(":");
        if (p.length != 4) {
            throw new IllegalArgumentException("Bad portal key: " + key);
        }
        return new PortalRef(
                Integer.parseInt(p[0]),
                new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalRef)) return false;
        PortalRef portalRef = (PortalRef) o;
        return dim == portalRef.dim && pos.equals(portalRef.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dim, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public String toString() {
        return key();
    }
}
