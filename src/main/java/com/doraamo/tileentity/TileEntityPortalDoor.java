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
import com.doraamo.util.LangKeys;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TileEntityPortalDoor extends TileEntity implements ITickable {

    private DestinationSettings destination;
    private boolean subGate;
    private int mainDim;
    private BlockPos mainPos = BlockPos.ORIGIN;

    private final Map<UUID, Integer> portalTimers = new HashMap<UUID, Integer>();
    private final Map<UUID, Boolean> touchedThisTick = new HashMap<UUID, Boolean>();

    public int getTargetDimensionId() {
        return destination == null ? DoraAmo.BLANK_DIMENSION : destination.dimensionId;
    }

    /** Empty-hand cycle: set dimension with portal-scaled landing. */
    public void setTargetDimensionId(int targetDimensionId) {
        if (subGate) {
            return;
        }
        if (targetDimensionId == DoraAmo.BLANK_DIMENSION) {
            setDestination(null);
            return;
        }
        setDestination(DestinationSettings.scaled(targetDimensionId));
    }

    public DestinationSettings getDestination() {
        return destination == null ? null : destination.copy();
    }

    public void setDestination(@Nullable DestinationSettings settings) {
        if (subGate) {
            return;
        }
        DestinationSettings previous = this.destination;
        this.destination = settings == null ? null : settings.copy();

        if (world != null && !world.isRemote && world.getMinecraftServer() != null) {
            MinecraftServer server = world.getMinecraftServer();
            PortalNetworkData data = PortalNetworkData.get(server);
            PortalRef mainRef = getSelfRef();
            if (previous != null && (settings == null || previous.dimensionId != settings.dimensionId)) {
                invalidateSubInDim(data, mainRef, previous.dimensionId, server);
            }
            if (settings != null) {
                invalidateSubInDim(data, mainRef, settings.dimensionId, server);
            }
        }

        markDirty();
        sync();
    }

    private static void invalidateSubInDim(PortalNetworkData data, PortalRef mainRef, int dim, MinecraftServer server) {
        PortalRef existing = data.findSubInDimension(mainRef, dim);
        if (existing != null) {
            data.unregisterSub(existing);
            data.tryBreakNow(existing, server);
        }
    }

    public boolean isSubGate() {
        return subGate;
    }

    public void setupAsSub(int mainDim, BlockPos mainPos) {
        this.subGate = true;
        this.mainDim = mainDim;
        this.mainPos = mainPos.toImmutable();
        this.destination = DestinationSettings.scaled(mainDim);
        markDirty();
        sync();
    }

    public PortalRef getMainRef() {
        return new PortalRef(mainDim, mainPos);
    }

    public PortalRef getSelfRef() {
        return new PortalRef(world.provider.getDimension(), pos);
    }

    private void sync() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    public boolean canTeleportFixed(Entity entity) {
        if (subGate) {
            return DimensionManager.isDimensionRegistered(mainDim);
        }
        if (destination == null) {
            return false;
        }
        return DimensionManager.isDimensionRegistered(destination.dimensionId);
    }

    public void tryTeleportPlayer(EntityPlayerMP player) {
        if (!canTeleportFixed(player)) {
            return;
        }
        if (subGate && player.dimension == mainDim
                && player.getDistanceSq(mainPos.getX() + 0.5D, mainPos.getY(), mainPos.getZ() + 0.5D) < 9.0D) {
            return;
        }

        UUID id = player.getUniqueID();
        if (Boolean.TRUE.equals(touchedThisTick.get(id))) {
            return;
        }
        touchedThisTick.put(id, Boolean.TRUE);

        int required = DoraAmo.PORTAL_CHARGE_TICKS;
        int time = portalTimers.containsKey(id) ? portalTimers.get(id) : 0;
        time++;
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

    private void teleportToMain(EntityPlayerMP player) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        WorldServer fromWorld = (WorldServer) world;
        WorldServer targetWorld = server.getWorld(mainDim);
        if (targetWorld == null) {
            return;
        }

        BlockPos scaled = PortalDoorTeleporter.scalePortalPos(fromWorld, targetWorld, pos);
        BlockPos land;

        targetWorld.getChunkFromBlockCoords(mainPos);
        IBlockState mainState = targetWorld.isBlockLoaded(mainPos) ? targetWorld.getBlockState(mainPos) : null;
        if (mainState != null && mainState.getBlock() == ModBlocks.PORTAL_DOOR) {
            EnumFacing facing = mainState.getValue(BlockPortalDoor.FACING);
            land = mainPos.offset(facing);
        } else {
            land = PortalDoorTeleporter.findLandingFromPortal(targetWorld, scaled);
            if (land == null) {
                player.sendStatusMessage(new TextComponentTranslation(LangKeys.PORTAL_NO_SAFE_LANDING), true);
                return;
            }
        }

        PortalDoorTeleporter teleporter = new PortalDoorTeleporter(targetWorld, land);
        if (player.dimension != mainDim) {
            server.getPlayerList().transferPlayerToDimension(player, mainDim, teleporter);
        } else {
            teleporter.placeEntity(targetWorld, player, player.rotationYaw);
        }
        player.timeUntilPortal = player.getPortalCooldown();
    }

    private void teleportFromMain(EntityPlayerMP player) {
        if (destination == null) {
            return;
        }
        int dimensionId = destination.dimensionId;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        WorldServer fromWorld = (WorldServer) world;
        WorldServer targetWorld = server.getWorld(dimensionId);
        if (targetWorld == null) {
            DoraAmo.logger.warn("Could not load world for dimension {}", dimensionId);
            return;
        }

        PortalRef mainRef = getSelfRef();
        PortalNetworkData data = PortalNetworkData.get(server);

        EnumFacing facing = EnumFacing.NORTH;
        IBlockState self = world.getBlockState(pos);
        if (self.getBlock() == ModBlocks.PORTAL_DOOR) {
            facing = self.getValue(BlockPortalDoor.FACING);
        }

        BlockPos scaled = PortalDoorTeleporter.scalePortalPos(fromWorld, targetWorld, pos);
        BlockPos resolved;

        boolean endScaled = dimensionId == 1 && destination.mode == DestinationSettings.Mode.SCALED;
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

        PortalRef existingSub = data.findSubInDimension(mainRef, dimensionId);
        if (existingSub != null) {
            targetWorld.getChunkFromBlockCoords(existingSub.pos);
            if (data.isValidSubDoor(targetWorld, existingSub, mainRef)) {
                BlockPos land = PortalDoorPlacer.playerStandPos(targetWorld, existingSub.pos, facing);
                finishTeleport(player, server, targetWorld, dimensionId, land);
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
            targetWorld.getChunkFromBlockCoords(resolved);
            ChunkPrep.forcePopulateAround(targetWorld, resolved, 1);
            if (PortalDoorPlacer.canPlaceDoorSafely(targetWorld, resolved)) {
                subPos = PortalDoorPlacer.placeDoorExact(targetWorld, resolved, facing,
                        BlockPortalDoor.EnumType.SUB, mainRef);
            } else if (destination.mode != DestinationSettings.Mode.COORDS
                    || destination.forceUnsafe
                    || endScaled) {
                subPos = PortalDoorPlacer.placeDoorForced(targetWorld, resolved, facing,
                        BlockPortalDoor.EnumType.SUB, mainRef);
            } else {
                player.sendStatusMessage(new TextComponentTranslation(LangKeys.PORTAL_TARGET_BLOCKED), true);
                return;
            }
            if (subPos == null) {
                player.sendStatusMessage(new TextComponentTranslation(LangKeys.PORTAL_TARGET_BLOCKED), true);
                return;
            }
        } else {
            subPos = PortalDoorPlacer.placeDoorSafeNear(targetWorld, resolved, facing,
                    BlockPortalDoor.EnumType.SUB, mainRef);
            if (subPos == null && destination.forceUnsafe) {
                subPos = PortalDoorPlacer.placeDoorForced(targetWorld, resolved, facing,
                        BlockPortalDoor.EnumType.SUB, mainRef);
            }
            if (subPos == null) {
                player.sendStatusMessage(new TextComponentTranslation(LangKeys.PORTAL_NO_SAFE_LANDING), true);
                return;
            }
        }

        BlockPos playerLand = PortalDoorPlacer.playerStandPos(targetWorld, subPos, facing);
        finishTeleport(player, server, targetWorld, dimensionId, playerLand);
    }

    private static void finishTeleport(EntityPlayerMP player, MinecraftServer server,
                                       WorldServer targetWorld, int dimensionId, BlockPos land) {
        PortalDoorTeleporter teleporter = new PortalDoorTeleporter(targetWorld, land);
        if (player.dimension != dimensionId) {
            server.getPlayerList().transferPlayerToDimension(player, dimensionId, teleporter);
        } else {
            teleporter.placeEntity(targetWorld, player, player.rotationYaw);
        }
        player.timeUntilPortal = Math.max(player.getPortalCooldown(), 60);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
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
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("TargetDim", getTargetDimensionId());
        if (destination != null) {
            compound.setTag("Dest", destination.writeToNBT(new NBTTagCompound()));
        }
        compound.setBoolean("SubGate", subGate);
        compound.setInteger("MainDim", mainDim);
        compound.setInteger("MainX", mainPos.getX());
        compound.setInteger("MainY", mainPos.getY());
        compound.setInteger("MainZ", mainPos.getZ());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        subGate = compound.getBoolean("SubGate");
        mainDim = compound.getInteger("MainDim");
        mainPos = new BlockPos(compound.getInteger("MainX"), compound.getInteger("MainY"), compound.getInteger("MainZ"));
        if (compound.hasKey("Dest")) {
            destination = DestinationSettings.fromNBT(compound.getCompoundTag("Dest"));
        } else if (compound.hasKey("TargetDim")) {
            int dim = compound.getInteger("TargetDim");
            destination = dim == DoraAmo.BLANK_DIMENSION ? null : DestinationSettings.scaled(dim);
        } else {
            destination = null;
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
