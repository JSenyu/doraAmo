package com.doraamo.destination;

import com.doraamo.portal.ChunkPrep;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class DestinationLocator {

    public static final List<String> STRUCTURES_OVERWORLD = Arrays.asList(
            "Village", "Monument", "Mansion", "Temple", "Mineshaft", "Stronghold"
    );
    public static final List<String> STRUCTURES_NETHER = Arrays.asList("Fortress");
    public static final List<String> STRUCTURES_END = Arrays.asList("EndCity");

    private static final Set<String> UNDERGROUND = new HashSet<String>(Arrays.asList(
            "Mineshaft", "Stronghold", "Fortress", "Temple"
    ));
    private static final Set<String> WATER_OR_FLOAT = new HashSet<String>(Arrays.asList(
            "Monument", "EndCity"
    ));

    private static final Set<Block> STRUCTURE_FLOORS = new HashSet<Block>(Arrays.asList(
            Blocks.NETHER_BRICK, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS,
            Blocks.STONEBRICK, Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_COBBLESTONE,
            Blocks.COBBLESTONE, Blocks.PLANKS, Blocks.BRICK_BLOCK,
            Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_STAIRS,
            Blocks.END_STONE, Blocks.END_BRICKS, Blocks.PRISMARINE, Blocks.SEA_LANTERN,
            Blocks.SANDSTONE, Blocks.RED_SANDSTONE
    ));

    public static final class ValidateResult {
        public final boolean found;
        @Nullable
        public final BlockPos pos;

        public ValidateResult(boolean found, @Nullable BlockPos pos) {
            this.found = found;
            this.pos = pos;
        }

        public static ValidateResult ok(BlockPos pos) {
            return new ValidateResult(true, pos);
        }

        public static ValidateResult fail() {
            return new ValidateResult(false, null);
        }
    }

    private DestinationLocator() {
    }

    public static List<String> structuresForDim(int dim) {
        if (dim == -1) {
            return STRUCTURES_NETHER;
        }
        if (dim == 1) {
            return STRUCTURES_END;
        }
        return STRUCTURES_OVERWORLD;
    }

    public static boolean isUndergroundStructure(String name) {
        return UNDERGROUND.contains(name);
    }

    public static boolean isWaterOrFloatStructure(String name) {
        return WATER_OR_FLOAT.contains(name);
    }

    public static BlockPos resolve(WorldServer world, BlockPos scaledPortalPos, DestinationSettings settings) {
        if (settings == null) {
            return scaledPortalPos;
        }
        switch (settings.mode) {
            case COORDS:
                return settings.getCoordPos();
            case BIOME: {
                BlockPos found = findBiomeStrict(world, scaledPortalPos, settings.biomeId);
                return found != null ? found : scaledPortalPos;
            }
            case STRUCTURE: {
                BlockPos found = findStructureStrict(world, scaledPortalPos, settings.structureName);
                return found != null ? found : scaledPortalPos;
            }
            case SCALED:
            default:
                return scaledPortalPos;
        }
    }

    public static ValidateResult validate(WorldServer world, BlockPos near, DestinationSettings settings) {
        if (settings == null || world == null) {
            return ValidateResult.fail();
        }
        switch (settings.mode) {
            case COORDS: {
                BlockPos p = settings.getCoordPos();
                if (p.getY() < 0 || p.getY() >= world.getHeight() - 1) {
                    return ValidateResult.fail();
                }
                ChunkPrep.forcePopulateAround(world, p, 1);
                return ValidateResult.ok(p);
            }
            case BIOME: {
                BlockPos found = findBiomeStrict(world, near, settings.biomeId);
                return found != null ? ValidateResult.ok(found) : ValidateResult.fail();
            }
            case STRUCTURE: {
                BlockPos found = findStructureStrict(world, near, settings.structureName);
                return found != null ? ValidateResult.ok(found) : ValidateResult.fail();
            }
            case SCALED:
            default:
                return ValidateResult.ok(near);
        }
    }

    @Nullable
    private static BlockPos findBiomeStrict(WorldServer world, BlockPos near, int biomeId) {
        Biome target = Biome.getBiome(biomeId);
        if (target == null) {
            target = Biomes.PLAINS;
        }
        List<Biome> list = new ArrayList<Biome>();
        list.add(target);
        BlockPos found = world.getBiomeProvider().findBiomePosition(
                near.getX(), near.getZ(), 640, list, new Random(near.toLong()));
        if (found == null) {
            found = world.getBiomeProvider().findBiomePosition(
                    near.getX(), near.getZ(), 2560, list, new Random(near.toLong() ^ 31L));
        }
        if (found == null) {
            return null;
        }
        ChunkPrep.forcePopulateAround(world, found, 1);
        int y = world.getHeight(found).getY();
        return new BlockPos(found.getX(), Math.max(1, y), found.getZ());
    }

    @Nullable
    private static BlockPos findStructureStrict(WorldServer world, BlockPos near, String name) {
        BlockPos origin = invokeNearestStructure(world, name, near);
        if (origin == null) {
            return null;
        }
        ChunkPrep.forcePopulateAround(world, origin, 3);

        if (isUndergroundStructure(name)) {
            BlockPos interior = findInteriorLanding(world, origin, name);
            return interior;
        }
        if (isWaterOrFloatStructure(name)) {
            BlockPos air = findAirLanding(world, origin);
            return air != null ? air : null;
        }
        int y = world.getHeight(origin).getY();
        return new BlockPos(origin.getX(), Math.max(1, y), origin.getZ());
    }

    /**
     * Prefer standable spots that are actually inside the structure bounding volume,
     * with structure-material floors — not nearby caves.
     */
    @Nullable
    private static BlockPos findInteriorLanding(WorldServer world, BlockPos origin, String name) {
        int minY = world.provider.getDimension() == -1 ? 32 : 5;
        int maxY = world.provider.getDimension() == -1
                ? 120
                : Math.min(world.getActualHeight() - 3, 120);
        int preferY = origin.getY() > 5 ? MathHelper.clamp(origin.getY(), minY, maxY)
                : (world.provider.getDimension() == -1 ? 64 : 40);

        BlockPos bestInside = null;
        BlockPos bestInsideStructFloor = null;
        double bestInsideDist = Double.MAX_VALUE;
        double bestFloorDist = Double.MAX_VALUE;

        for (int r = 0; r <= 32; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    for (int dy = 0; dy <= (maxY - minY); dy++) {
                        int[] ys = dy == 0 ? new int[] { preferY } : new int[] { preferY + dy, preferY - dy };
                        for (int y : ys) {
                            if (y < minY || y > maxY) {
                                continue;
                            }
                            BlockPos feet = new BlockPos(x, y, z);
                            if (!isTwoHighAir(world, feet) || !hasSolidFloor(world, feet)) {
                                continue;
                            }
                            if (canQueryStructure(world, name)) {
                                if (!isPositionInStructure(world, name, feet)) {
                                    continue;
                                }
                            } else if (!hasCover(world, feet)) {
                                continue;
                            }
                            double dist = feet.distanceSq(origin);
                            if (isStructureFloor(world, feet.down())) {
                                if (dist < bestFloorDist) {
                                    bestFloorDist = dist;
                                    bestInsideStructFloor = feet;
                                }
                            } else if (dist < bestInsideDist) {
                                bestInsideDist = dist;
                                bestInside = feet;
                            }
                        }
                    }
                }
            }
            if (bestInsideStructFloor != null && r >= 4) {
                return bestInsideStructFloor;
            }
            if (bestInside != null && r >= 8) {
                return bestInside;
            }
        }
        return bestInsideStructFloor != null ? bestInsideStructFloor : bestInside;
    }

    @Nullable
    private static BlockPos findAirLanding(WorldServer world, BlockPos origin) {
        int minY = 1;
        int maxY = Math.min(world.getActualHeight() - 3, 200);
        int preferY = MathHelper.clamp(origin.getY() > 0 ? origin.getY() : 64, minY, maxY);

        BlockPos bestSolid = null;
        BlockPos bestAir = null;
        double bestSolidDist = Double.MAX_VALUE;
        double bestAirDist = Double.MAX_VALUE;

        for (int r = 0; r <= 20; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    for (int dy = 0; dy <= 32; dy++) {
                        int[] ys = new int[] { preferY + dy, preferY - dy };
                        for (int y : ys) {
                            if (y < minY || y > maxY) {
                                continue;
                            }
                            BlockPos feet = new BlockPos(x, y, z);
                            if (!isTwoHighAir(world, feet)) {
                                continue;
                            }
                            double dist = feet.distanceSq(origin);
                            if (hasSolidFloor(world, feet)) {
                                if (dist < bestSolidDist) {
                                    bestSolidDist = dist;
                                    bestSolid = feet;
                                }
                            } else if (dist < bestAirDist) {
                                bestAirDist = dist;
                                bestAir = feet;
                            }
                        }
                    }
                }
            }
            if (bestSolid != null && r >= 4) {
                return bestSolid;
            }
        }
        return bestSolid != null ? bestSolid : bestAir;
    }

    private static boolean hasCover(WorldServer world, BlockPos feet) {
        BlockPos above = feet.up(3);
        IBlockState ceiling = world.getBlockState(above);
        return ceiling.getMaterial().blocksMovement() || !world.canBlockSeeSky(feet);
    }

    private static boolean isStructureFloor(WorldServer world, BlockPos ground) {
        return STRUCTURE_FLOORS.contains(world.getBlockState(ground).getBlock());
    }

    private static boolean isTwoHighAir(WorldServer world, BlockPos feet) {
        IBlockState a = world.getBlockState(feet);
        IBlockState b = world.getBlockState(feet.up());
        if (a.getMaterial().blocksMovement() || b.getMaterial().blocksMovement()) {
            return false;
        }
        if (a.getMaterial().isLiquid() || b.getMaterial().isLiquid()) {
            return false;
        }
        return a.getBlock().isAir(a, world, feet) || a.getBlock().isReplaceable(world, feet);
    }

    private static boolean hasSolidFloor(WorldServer world, BlockPos feet) {
        BlockPos ground = feet.down();
        IBlockState g = world.getBlockState(ground);
        return g.isSideSolid(world, ground, EnumFacing.UP) && !g.getMaterial().isLiquid();
    }

    private static boolean canQueryStructure(WorldServer world, String name) {
        return getChunkGenerator(world) != null;
    }

    private static boolean isPositionInStructure(WorldServer world, String name, BlockPos pos) {
        IChunkGenerator gen = getChunkGenerator(world);
        if (gen == null) {
            return false;
        }
        try {
            Method m = findMethod(gen.getClass(), "isInsideStructure",
                    World.class, String.class, BlockPos.class);
            if (m != null) {
                m.setAccessible(true);
                Object r = m.invoke(gen, world, name, pos);
                return r instanceof Boolean && (Boolean) r;
            }
        } catch (Exception ignored) {
        }
        Object structureGen = getStructureGenerator(world, name);
        if (structureGen == null) {
            return false;
        }
        try {
            Method m = findMethod(structureGen.getClass(), "isPositionInStructure", World.class, BlockPos.class);
            if (m == null) {
                m = findMethod(structureGen.getClass(), "isInsideStructure", BlockPos.class);
                if (m != null) {
                    m.setAccessible(true);
                    Object r = m.invoke(structureGen, pos);
                    return r instanceof Boolean && (Boolean) r;
                }
                return false;
            }
            m.setAccessible(true);
            Object r = m.invoke(structureGen, world, pos);
            return r instanceof Boolean && (Boolean) r;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private static IChunkGenerator getChunkGenerator(WorldServer world) {
        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof ChunkProviderServer) {
            return ((ChunkProviderServer) provider).chunkGenerator;
        }
        return null;
    }

    @Nullable
    private static Object getStructureGenerator(WorldServer world, String name) {
        try {
            IChunkGenerator chunkGen = getChunkGenerator(world);
            if (chunkGen == null) {
                return null;
            }
            String[] fieldNames;
            if ("Fortress".equals(name)) {
                fieldNames = new String[] { "genNetherBridge", "field_185953_o" };
            } else if ("Stronghold".equals(name)) {
                fieldNames = new String[] { "strongholdGenerator", "field_186004_w" };
            } else if ("Mineshaft".equals(name)) {
                fieldNames = new String[] { "mineshaftGenerator", "field_186005_x" };
            } else if ("Village".equals(name)) {
                fieldNames = new String[] { "villageGenerator", "field_186006_y" };
            } else if ("Temple".equals(name)) {
                fieldNames = new String[] { "scatteredFeatureGenerator", "field_186007_z" };
            } else if ("Monument".equals(name)) {
                fieldNames = new String[] { "oceanMonumentGenerator", "field_185980_A" };
            } else if ("Mansion".equals(name)) {
                fieldNames = new String[] { "woodlandMansionGenerator", "field_191060_C" };
            } else if ("EndCity".equals(name)) {
                fieldNames = new String[] { "endCityGen", "field_185965_v" };
            } else {
                fieldNames = new String[0];
            }
            for (String fn : fieldNames) {
                Field f = findField(chunkGen.getClass(), fn);
                if (f != null) {
                    f.setAccessible(true);
                    Object v = f.get(chunkGen);
                    if (v != null) {
                        return v;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private static BlockPos invokeNearestStructure(WorldServer world, String name, BlockPos near) {
        Object structureGen = getStructureGenerator(world, name);
        if (structureGen != null) {
            BlockPos direct = invokeGetNearest(structureGen, world, near, false);
            if (direct == null) {
                direct = invokeGetNearest(structureGen, world, near, true);
            }
            if (direct != null) {
                return direct;
            }
        }

        try {
            IChunkProvider provider = world.getChunkProvider();
            if (!(provider instanceof ChunkProviderServer)) {
                return null;
            }
            IChunkGenerator gen = ((ChunkProviderServer) provider).chunkGenerator;
            Method m = findMethod(gen.getClass(), "getNearestStructurePos",
                    World.class, String.class, BlockPos.class, boolean.class);
            if (m == null) {
                return null;
            }
            m.setAccessible(true);
            Object result = m.invoke(gen, world, name, near, false);
            if (!(result instanceof BlockPos)) {
                result = m.invoke(gen, world, name, near, true);
            }
            return result instanceof BlockPos ? (BlockPos) result : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static BlockPos invokeGetNearest(Object structureGen, World world, BlockPos near, boolean findUnexplored) {
        try {
            Method m = findMethod(structureGen.getClass(), "getNearestStructurePos",
                    World.class, BlockPos.class, boolean.class);
            if (m == null) {
                return null;
            }
            m.setAccessible(true);
            Object r = m.invoke(structureGen, world, near, findUnexplored);
            return r instanceof BlockPos ? (BlockPos) r : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    @Nullable
    private static Field findField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    public static List<Biome> allBiomes() {
        List<Biome> list = new ArrayList<Biome>();
        for (Biome b : ForgeRegistries.BIOMES) {
            if (b != null) {
                list.add(b);
            }
        }
        return list;
    }
}
