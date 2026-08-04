package com.doraamo.destination;

import com.doraamo.portal.ChunkPrep;
import com.doraamo.util.DimUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private static final Set<String> UNDERGROUND = new HashSet<>(Arrays.asList(
            "Mineshaft", "Stronghold", "Fortress", "Temple"
    ));
    private static final Set<String> WATER_OR_FLOAT = new HashSet<>(Arrays.asList(
            "Monument", "EndCity"
    ));

    private static final Set<Block> STRUCTURE_FLOORS = new HashSet<>(Arrays.asList(
            Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS,
            Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_COBBLESTONE,
            Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.BRICKS,
            Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PURPUR_STAIRS,
            Blocks.END_STONE, Blocks.END_STONE_BRICKS, Blocks.PRISMARINE, Blocks.SEA_LANTERN,
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

    public static List<String> structuresForDim(String dim) {
        if (DimUtil.isNether(dim)) {
            return STRUCTURES_NETHER;
        }
        if (DimUtil.isEnd(dim)) {
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

    public static BlockPos resolve(ServerWorld world, BlockPos scaledPortalPos, DestinationSettings settings) {
        if (settings == null) {
            return scaledPortalPos;
        }
        switch (settings.mode) {
            case COORDS:
                return settings.getCoordPos();
            case BIOME: {
                BlockPos found = findBiomeStrict(world, scaledPortalPos, settings.biomeKey);
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

    public static ValidateResult validate(ServerWorld world, BlockPos near, DestinationSettings settings) {
        if (settings == null || world == null) {
            return ValidateResult.fail();
        }
        switch (settings.mode) {
            case COORDS: {
                BlockPos p = settings.getCoordPos();
                if (p.getY() < 0 || p.getY() >= world.getMaxBuildHeight() - 1) {
                    return ValidateResult.fail();
                }
                ChunkPrep.forcePopulateAround(world, p, 1);
                return ValidateResult.ok(p);
            }
            case BIOME: {
                BlockPos found = findBiomeStrict(world, near, settings.biomeKey);
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
    private static BlockPos findBiomeStrict(ServerWorld world, BlockPos near, String biomeKey) {
        ResourceLocation key = new ResourceLocation(biomeKey.contains(":") ? biomeKey : "minecraft:" + biomeKey);
        Biome target = ForgeRegistries.BIOMES.getValue(key);
        if (target == null) {
            target = ForgeRegistries.BIOMES.getValue(new ResourceLocation("minecraft:plains"));
        }
        final Biome biomeTarget = target;
        BlockPos found = world.findNearestBiome(biomeTarget, near, 640, 8);
        if (found == null) {
            found = world.findNearestBiome(biomeTarget, near, 2560, 8);
        }
        if (found == null) {
            return null;
        }
        ChunkPrep.forcePopulateAround(world, found, 1);
        int y = world.getHeightmapPos(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, found).getY();
        return new BlockPos(found.getX(), Math.max(1, y), found.getZ());
    }

    @Nullable
    private static BlockPos findStructureStrict(ServerWorld world, BlockPos near, String name) {
        BlockPos origin = findNearestStructure(world, name, near);
        if (origin == null) {
            return null;
        }
        ChunkPrep.forcePopulateAround(world, origin, 3);

        if (isUndergroundStructure(name)) {
            return findInteriorLanding(world, origin, name);
        }
        if (isWaterOrFloatStructure(name)) {
            return findAirLanding(world, origin);
        }
        int y = world.getHeightmapPos(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, origin).getY();
        return new BlockPos(origin.getX(), Math.max(1, y), origin.getZ());
    }

    @Nullable
    private static BlockPos findInteriorLanding(ServerWorld world, BlockPos origin, String name) {
        boolean nether = DimUtil.isNether(DimUtil.levelKey(world));
        int minY = nether ? 32 : 5;
        int maxY = nether ? 120 : Math.min(world.getMaxBuildHeight() - 3, 120);
        int preferY = origin.getY() > 5 ? MathHelper.clamp(origin.getY(), minY, maxY) : (nether ? 64 : 40);

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
                            double dist = feet.distSqr(origin);
                            if (isStructureFloor(world, feet.below())) {
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
    private static BlockPos findAirLanding(ServerWorld world, BlockPos origin) {
        int minY = 1;
        int maxY = Math.min(world.getMaxBuildHeight() - 3, 200);
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
                            double dist = feet.distSqr(origin);
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

    private static boolean hasCover(ServerWorld world, BlockPos feet) {
        BlockPos above = feet.above(3);
        BlockState ceiling = world.getBlockState(above);
        return ceiling.getMaterial().blocksMotion() || !world.canSeeSky(feet);
    }

    private static boolean isStructureFloor(ServerWorld world, BlockPos ground) {
        return STRUCTURE_FLOORS.contains(world.getBlockState(ground).getBlock());
    }

    private static boolean isTwoHighAir(ServerWorld world, BlockPos feet) {
        BlockState a = world.getBlockState(feet);
        BlockState b = world.getBlockState(feet.above());
        if (a.getMaterial().blocksMotion() || b.getMaterial().blocksMotion()) {
            return false;
        }
        if (a.getMaterial().isLiquid() || b.getMaterial().isLiquid()) {
            return false;
        }
        return a.isAir(world, feet) || a.getMaterial().isReplaceable();
    }

    private static boolean hasSolidFloor(ServerWorld world, BlockPos feet) {
        BlockPos ground = feet.below();
        BlockState g = world.getBlockState(ground);
        return g.isFaceSturdy(world, ground, Direction.UP) && !g.getMaterial().isLiquid();
    }

    private static boolean canQueryStructure(ServerWorld world, String name) {
        return structureForName(name) != null;
    }

    private static boolean isPositionInStructure(ServerWorld world, String name, BlockPos pos) {
        BlockPos origin = findNearestStructure(world, name, pos);
        if (origin == null) {
            return false;
        }
        return pos.distSqr(origin) < 48 * 48;
    }

    @Nullable
    private static BlockPos findNearestStructure(ServerWorld world, String name, BlockPos near) {
        Structure<?> structure = structureForName(name);
        if (structure == null) {
            return null;
        }
        BlockPos found = world.findNearestMapFeature(structure, near, 640, false);
        if (found == null) {
            found = world.findNearestMapFeature(structure, near, 640, true);
        }
        return found;
    }

    @Nullable
    private static Structure<?> structureForName(String name) {
        if (name == null) {
            return null;
        }
        switch (name) {
            case "Village":
                return Structure.VILLAGE;
            case "Monument":
                return Structure.OCEAN_MONUMENT;
            case "Mansion":
                return Structure.WOODLAND_MANSION;
            case "Temple":
                return Structure.DESERT_PYRAMID;
            case "Mineshaft":
                return Structure.MINESHAFT;
            case "Stronghold":
                return Structure.STRONGHOLD;
            case "Fortress":
                return Structure.NETHER_BRIDGE;
            case "EndCity":
                return Structure.END_CITY;
            default:
                ResourceLocation id = new ResourceLocation(name.contains(":") ? name : "minecraft:" + name.toLowerCase());
                return ForgeRegistries.STRUCTURE_FEATURES.getValue(id);
        }
    }

    public static List<Biome> allBiomes() {
        List<Biome> list = new ArrayList<>();
        for (Biome b : ForgeRegistries.BIOMES) {
            if (b != null) {
                list.add(b);
            }
        }
        return list;
    }
}
