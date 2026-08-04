package com.doraamo.tileentity;

import com.doraamo.DoraAmo;
import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.destination.DestinationLocator;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.ChunkPrep;
import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.portal.PortalNetworkData;
import com.doraamo.portal.PortalRef;
import com.doraamo.teleport.PortalDoorTeleporter;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TileEntityPortalDoor extends BlockEntity {

    private DestinationSettings destination;
    private boolean subGate;
    private String mainDim = DimUtil.OVERWORLD;
    private BlockPos mainPos = BlockPos.ZERO;

    private final Map<UUID, Integer> portalTimers = new HashMap<>();
    private final Map<UUID, Boolean> touchedThisTick = new HashMap<>();

    public TileEntityPortalDoor(BlockPos pos, BlockState state) {
        super(ModBlocks.PORTAL_DOOR_TILE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPortalDoor be) {
        be.tick();
    }

    public String getTargetDimension() {
        return destination == null ? DoraAmo.BLANK_DIMENSION : destination.dimension;
    }

    public void setTargetDimension(String targetDimension) {
        if (subGate) {
            return;
        }
        if (DimUtil.isBlank(targetDimension)) {
            setDestination(null);
            return;
        }
        setDestination(DestinationSettings.scaled(targetDimension));
    }

    @Nullable
    public DestinationSettings getDestination() {
        return destination == null ? null : destination.copy();
    }

    public void setDestination(@Nullable DestinationSettings settings) {
        if (subGate) {
            return;
        }
        DestinationSettings previous = this.destination;
        this.destination = settings == null ? null : settings.copy();

        if (level != null && !level.isClientSide && level.getServer() != null) {
            MinecraftServer server = level.getServer();
            PortalNetworkData data = PortalNetworkData.get(server);
            PortalRef mainRef = getSelfRef();
            if (previous != null && (settings == null || !previous.dimension.equals(settings.dimension))) {
                invalidateSubInDim(data, mainRef, previous.dimension, server);
            }
            if (settings != null) {
                invalidateSubInDim(data, mainRef, settings.dimension, server);
            }
        }

        setChanged();
        sync();
    }

    private static void invalidateSubInDim(PortalNetworkData data, PortalRef mainRef, String dim, MinecraftServer server) {
        PortalRef existing = data.findSubInDimension(mainRef, dim);
        if (existing != null) {
            data.unregisterSub(existing);
            data.tryBreakNow(existing, server);
        }
    }

    public boolean isSubGate() {
        return subGate;
    }

    public void setupAsSub(String mainDim, BlockPos mainPos) {
        this.subGate = true;
        this.mainDim = DimUtil.normalize(mainDim);
        this.mainPos = mainPos.immutable();
        this.destination = DestinationSettings.scaled(mainDim);
        setChanged();
        sync();
    }

    public PortalRef getMainRef() {
        return new PortalRef(mainDim, mainPos);
    }

    public PortalRef getSelfRef() {
        return new PortalRef(DimUtil.levelKey(level), worldPosition);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public boolean canTeleportFixed(Entity entity) {
        if (subGate) {
            return DimUtil.getLevel(entity.getServer(), mainDim) != null;
        }
        if (destination == null || DimUtil.isBlank(destination.dimension)) {
            return false;
        }
        return DimUtil.getLevel(entity.getServer(), destination.dimension) != null;
    }

    public void tryTeleportPlayer(ServerPlayer player) {
        if (!canTeleportFixed(player)) {
            return;
        }
        if (subGate && player.level().dimension().location().toString().equals(mainDim)
                && player.distanceToSqr(mainPos.getX() + 0.5D, mainPos.getY(), mainPos.getZ() + 0.5D) < 9.0D) {
            return;
        }

        UUID id = player.getUUID();
        if (Boolean.TRUE.equals(touchedThisTick.get(id))) {
            return;
        }
        touchedThisTick.put(id, Boolean.TRUE);

        int required = DoraAmo.PORTAL_CHARGE_TICKS;
        int time = portalTimers.getOrDefault(id, 0) + 1;
        portalTimers.put(id, time);

        if (time >= required) {
            portalTimers.remove(id);
            touchedThisTick.remove(id);
            if (subGate) {
                teleportToMain(player);
            } else {
                teleportFromMain(player);
            }
        }
    }

    private void teleportToMain(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel fromWorld = (ServerLevel) level;
        ServerLevel targetWorld = DimUtil.getLevel(server, mainDim);
        if (targetWorld == null) {
            return;
        }

        BlockPos scaled = PortalDoorTeleporter.scalePortalPos(fromWorld, targetWorld, worldPosition);
        BlockPos land;

        targetWorld.getChunk(mainPos);
        BlockState mainState = targetWorld.isLoaded(mainPos) ? targetWorld.getBlockState(mainPos) : null;
        if (mainState != null && mainState.getBlock() == ModBlocks.PORTAL_DOOR) {
            Direction facing = mainState.getValue(BlockPortalDoor.FACING);
            land = mainPos.relative(facing);
        } else {
            land = PortalDoorTeleporter.findLandingFromPortal(targetWorld, scaled);
            if (land == null) {
                player.displayClientMessage(Component.translatable(LangKeys.PORTAL_NO_SAFE_LANDING), true);
                return;
            }
        }

        if (!player.level().dimension().location().toString().equals(mainDim)) {
            player.changeDimension(PortalDoorTeleporter.createTransition(targetWorld, land, player));
        } else {
            applyLocalTeleport(player, land);
        }
        player.setPortalCooldown(60);
    }

    private void teleportFromMain(ServerPlayer player) {
        if (destination == null) {
            return;
        }
        String dimKey = destination.dimension;
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel fromWorld = (ServerLevel) level;
        ServerLevel targetWorld = DimUtil.getLevel(server, dimKey);
        if (targetWorld == null) {
            DoraAmo.logger.warn("Could not load world for dimension {}", dimKey);
            return;
        }

        PortalRef mainRef = getSelfRef();
        PortalNetworkData data = PortalNetworkData.get(server);

        Direction facing = Direction.NORTH;
        BlockState self = level.getBlockState(worldPosition);
        if (self.getBlock() == ModBlocks.PORTAL_DOOR) {
            facing = self.getValue(BlockPortalDoor.FACING);
        }

        BlockPos scaled = PortalDoorTeleporter.scalePortalPos(fromWorld, targetWorld, worldPosition);
        BlockPos resolved;

        boolean endScaled = DimUtil.isEnd(dimKey) && destination.mode == DestinationSettings.Mode.SCALED;
        if (endScaled) {
            resolved = ChunkPrep.prepareEndPortalSite(targetWorld, facing);
        } else {
            resolved = DestinationLocator.resolve(targetWorld, scaled, destination);
            ChunkPrep.forcePopulateAround(targetWorld, resolved, 2);
            if (destination.mode == DestinationSettings.Mode.STRUCTURE) {
                BlockPos again = DestinationLocator.resolve(targetWorld, scaled, destination);
                if (again != null) {
                    resolved = again;
                }
            }
        }

        PortalRef existingSub = data.findSubInDimension(mainRef, dimKey);
        if (existingSub != null) {
            targetWorld.getChunk(existingSub.pos);
            if (data.isValidSubDoor(targetWorld, existingSub, mainRef)) {
                BlockPos land = PortalDoorPlacer.playerStandPos(targetWorld, existingSub.pos, facing);
                finishTeleport(player, targetWorld, dimKey, land);
                return;
            }
            data.unregisterSub(existingSub);
            data.tryBreakNow(existingSub, server);
        }

        boolean exact = destination.mode == DestinationSettings.Mode.COORDS
                || destination.mode == DestinationSettings.Mode.STRUCTURE
                || endScaled;
        BlockPos subPos;
        if (exact) {
            targetWorld.getChunk(resolved);
            ChunkPrep.forcePopulateAround(targetWorld, resolved, 1);
            if (PortalDoorPlacer.canPlaceDoorSafely(targetWorld, resolved)) {
                subPos = PortalDoorPlacer.placeDoorExact(targetWorld, resolved, facing,
                        BlockPortalDoor.DoorType.SUB, mainRef);
            } else if (destination.mode != DestinationSettings.Mode.COORDS
                    || destination.forceUnsafe
                    || endScaled) {
                subPos = PortalDoorPlacer.placeDoorForced(targetWorld, resolved, facing,
                        BlockPortalDoor.DoorType.SUB, mainRef);
            } else {
                player.displayClientMessage(Component.translatable(LangKeys.PORTAL_TARGET_BLOCKED), true);
                return;
            }
            if (subPos == null) {
                player.displayClientMessage(Component.translatable(LangKeys.PORTAL_TARGET_BLOCKED), true);
                return;
            }
        } else {
            subPos = PortalDoorPlacer.placeDoorSafeNear(targetWorld, resolved, facing,
                    BlockPortalDoor.DoorType.SUB, mainRef);
            if (subPos == null && destination.forceUnsafe) {
                subPos = PortalDoorPlacer.placeDoorForced(targetWorld, resolved, facing,
                        BlockPortalDoor.DoorType.SUB, mainRef);
            }
            if (subPos == null) {
                player.displayClientMessage(Component.translatable(LangKeys.PORTAL_NO_SAFE_LANDING), true);
                return;
            }
        }

        BlockPos playerLand = PortalDoorPlacer.playerStandPos(targetWorld, subPos, facing);
        finishTeleport(player, targetWorld, dimKey, playerLand);
    }

    private static void finishTeleport(ServerPlayer player, ServerLevel targetWorld, String dimKey, BlockPos land) {
        if (!player.level().dimension().location().toString().equals(dimKey)) {
            player.changeDimension(PortalDoorTeleporter.createTransition(targetWorld, land, player));
        } else {
            applyLocalTeleport(player, land);
        }
        player.setPortalCooldown(60);
    }

    private static void applyLocalTeleport(ServerPlayer player, BlockPos land) {
        player.teleportTo(land.getX() + 0.5D, land.getY(), land.getZ() + 0.5D);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> it = portalTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            if (!Boolean.TRUE.equals(touchedThisTick.get(entry.getKey()))) {
                it.remove();
            }
        }
        touchedThisTick.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.saveAdditional(compound, registries);
        compound.putString("TargetDim", getTargetDimension());
        if (destination != null) {
            compound.put("Dest", destination.writeToNBT(new CompoundTag()));
        }
        compound.putBoolean("SubGate", subGate);
        compound.putString("MainDim", mainDim);
        compound.putInt("MainX", mainPos.getX());
        compound.putInt("MainY", mainPos.getY());
        compound.putInt("MainZ", mainPos.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.loadAdditional(compound, registries);
        subGate = compound.getBoolean("SubGate");
        if (compound.contains("MainDim")) {
            if (compound.get("MainDim").getId() == CompoundTag.TAG_STRING) {
                mainDim = DimUtil.normalize(compound.getString("MainDim"));
            } else {
                mainDim = DimUtil.fromLegacyInt(compound.getInt("MainDim"));
            }
        }
        mainPos = new BlockPos(compound.getInt("MainX"), compound.getInt("MainY"), compound.getInt("MainZ"));
        if (compound.contains("Dest")) {
            destination = DestinationSettings.fromNBT(compound.getCompound("Dest"));
        } else if (compound.contains("TargetDim")) {
            String dim = compound.getString("TargetDim");
            if (compound.get("TargetDim").getId() != CompoundTag.TAG_STRING) {
                int legacy = compound.getInt("TargetDim");
                dim = legacy == Integer.MIN_VALUE ? "" : DimUtil.fromLegacyInt(legacy);
            }
            destination = DimUtil.isBlank(dim) ? null : DestinationSettings.scaled(dim);
        } else {
            destination = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket(HolderLookup.Provider registries) {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
