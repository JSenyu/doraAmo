package com.doraamo.destination;

import com.doraamo.portal.ChunkPrep;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.doraamo.util.RegistryHelper;
import net.minecraft.world.level.levelgen.Heightmap;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    public static BlockPos resolve(ServerLevel world, BlockPos scaledPortalPos, DestinationSettings settings) {
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

    public static ValidateResult validate(ServerLevel world, BlockPos near, DestinationSettings settings) {
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
    private static BlockPos findBiomeStrict(ServerLevel world, BlockPos near, String biomeKey) {
        ResourceLocation key = ResourceLocation.tryParse(biomeKey.contains(":") ? biomeKey : "minecraft:" + biomeKey);
        if (key == null) {
            key = new ResourceLocation("minecraft", "plains");
        }
        ResourceKey<Biome> biomeKeyObj = ResourceKey.create(Registries.BIOME, key);
        Optional<Holder.Reference<Biome>> targetHolder = world.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeKeyObj);
        if (targetHolder.isEmpty()) {
            targetHolder = world.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", "plains")));
        }
        if (targetHolder.isEmpty()) {
            return null;
        }
        Holder<Biome> biomeTarget = targetHolder.get();
        ResourceKey<Biome> biomeKeyRef = biomeKeyObj;
        var pair = world.findClosestBiome3d(holder -> holder.is(biomeKeyRef), near, 640, 8, 8);
        BlockPos found = pair != null ? pair.getFirst() : null;
        if (found == null) {
            pair = world.findClosestBiome3d(holder -> holder.is(biomeKeyRef), near, 2560, 8, 8);
            found = pair != null ? pair.getFirst() : null;
        }
        if (found == null) {
            return null;
        }
        ChunkPrep.forcePopulateAround(world, found, 1);
        int y = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found).getY();
        return new BlockPos(found.getX(), Math.max(1, y), found.getZ());
    }

    @Nullable
    private static BlockPos findStructureStrict(ServerLevel world, BlockPos near, String name) {
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
        int y = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin).getY();
        return new BlockPos(origin.getX(), Math.max(1, y), origin.getZ());
    }

    @Nullable
    private static BlockPos findInteriorLanding(ServerLevel world, BlockPos origin, String name) {
        boolean nether = DimUtil.isNether(DimUtil.levelKey(world));
        int minY = nether ? 32 : 5;
        int maxY = nether ? 120 : Math.min(world.getMaxBuildHeight() - 3, 120);
        int preferY = origin.getY() > 5 ? Mth.clamp(origin.getY(), minY, maxY) : (nether ? 64 : 40);

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
    private static BlockPos findAirLanding(ServerLevel world, BlockPos origin) {
        int minY = 1;
        int maxY = Math.min(world.getMaxBuildHeight() - 3, 200);
        int preferY = Mth.clamp(origin.getY() > 0 ? origin.getY() : 64, minY, maxY);

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

    private static boolean hasCover(ServerLevel world, BlockPos feet) {
        BlockPos above = feet.above(3);
        BlockState ceiling = world.getBlockState(above);
        return ceiling.blocksMotion() || !world.canSeeSky(feet);
    }

    private static boolean isStructureFloor(ServerLevel world, BlockPos ground) {
        return STRUCTURE_FLOORS.contains(world.getBlockState(ground).getBlock());
    }

    private static boolean isTwoHighAir(ServerLevel world, BlockPos feet) {
        BlockState a = world.getBlockState(feet);
        BlockState b = world.getBlockState(feet.above());
        if (a.blocksMotion() || b.blocksMotion()) {
            return false;
        }
        if (!a.getFluidState().isEmpty() || !b.getFluidState().isEmpty()) {
            return false;
        }
        return a.isAir() || a.canBeReplaced();
    }

    private static boolean hasSolidFloor(ServerLevel world, BlockPos feet) {
        BlockPos ground = feet.below();
        BlockState g = world.getBlockState(ground);
        return g.isFaceSturdy(world, ground, net.minecraft.core.Direction.UP) && g.getFluidState().isEmpty();
    }

    private static boolean canQueryStructure(ServerLevel world, String name) {
        return structureKeyForName(name) != null;
    }

    private static boolean isPositionInStructure(ServerLevel world, String name, BlockPos pos) {
        BlockPos origin = findNearestStructure(world, name, pos);
        if (origin == null) {
            return false;
        }
        return pos.distSqr(origin) < 48 * 48;
    }

    @Nullable
    private static BlockPos findNearestStructure(ServerLevel world, String name, BlockPos near) {
        ResourceKey<Structure> structureKey = structureKeyForName(name);
        if (structureKey == null) {
            return null;
        }
        var registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder = registry.getHolder(structureKey);
        if (holder.isEmpty()) {
            return null;
        }
        var result = world.getChunkSource().getGenerator().findNearestMapStructure(
                world, HolderSet.direct(holder.get()), near, 640, false);
        if (result == null) {
            result = world.getChunkSource().getGenerator().findNearestMapStructure(
                    world, HolderSet.direct(holder.get()), near, 640, true);
        }
        return result != null ? result.getFirst() : null;
    }

    @Nullable
    private static ResourceKey<Structure> structureKeyForName(String name) {
        if (name == null) {
            return null;
        }
        String id;
        switch (name) {
            case "Village":
                id = "village";
                break;
            case "Monument":
                id = "monument";
                break;
            case "Mansion":
                id = "mansion";
                break;
            case "Temple":
                id = "desert_pyramid";
                break;
            case "Mineshaft":
                id = "mineshaft";
                break;
            case "Stronghold":
                id = "stronghold";
                break;
            case "Fortress":
                id = "fortress";
                break;
            case "EndCity":
                id = "end_city";
                break;
            default:
                id = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name.toLowerCase();
                ResourceLocation loc = ResourceLocation.tryParse(name.contains(":") ? name : "minecraft:" + id);
                if (loc != null) {
                    return ResourceKey.create(Registries.STRUCTURE, loc);
                }
                return null;
        }
        return ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", id));
    }

    public static List<Biome> allBiomes() {
        return RegistryHelper.allBiomes();
    }
}
