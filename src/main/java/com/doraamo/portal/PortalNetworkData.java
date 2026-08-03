package com.doraamo.portal;

import com.doraamo.DoraAmo;
import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Global portal graph: main ↔ subs, plus pending sub breaks for unloaded chunks.
 * Stored on the overworld map storage so it survives restarts.
 */
public class PortalNetworkData extends WorldSavedData {

    public static final String DATA_NAME = DoraAmo.MODID + "_portals";

    private final Map<String, Set<String>> mainToSubs = new HashMap<String, Set<String>>();
    private final Map<String, String> subToMain = new HashMap<String, String>();
    private final Set<String> pendingBreak = new HashSet<String>();

    public PortalNetworkData() {
        super(DATA_NAME);
    }

    public PortalNetworkData(String name) {
        super(name);
    }

    public static PortalNetworkData get(MinecraftServer server) {
        WorldServer overworld = server.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        PortalNetworkData data = (PortalNetworkData) storage.getOrLoadData(PortalNetworkData.class, DATA_NAME);
        if (data == null) {
            data = new PortalNetworkData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public static PortalNetworkData get() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return null;
        }
        return get(server);
    }

    /** One sub portal per dimension per main; replaces any previous binding in that dimension. */
    public void registerSub(PortalRef main, PortalRef sub) {
        String mk = main.key();
        String sk = sub.key();
        Set<String> set = mainToSubs.get(mk);
        if (set == null) {
            set = new HashSet<String>();
            mainToSubs.put(mk, set);
        }

        java.util.List<String> replaced = new java.util.ArrayList<String>();
        for (String existing : set) {
            PortalRef other = PortalRef.parse(existing);
            if (other.dim == sub.dim && !existing.equals(sk)) {
                replaced.add(existing);
            }
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (String oldSk : replaced) {
            set.remove(oldSk);
            subToMain.remove(oldSk);
            pendingBreak.add(oldSk);
            if (server != null) {
                tryBreakNow(PortalRef.parse(oldSk), server);
            }
        }

        set.add(sk);
        subToMain.put(sk, mk);
        pendingBreak.remove(sk);
        markDirty();
    }

    public boolean isValidSubDoor(WorldServer world, PortalRef sub, PortalRef expectedMain) {
        if (!world.isBlockLoaded(sub.pos)) {
            world.getChunkFromBlockCoords(sub.pos);
        }
        IBlockState state = world.getBlockState(sub.pos);
        if (state.getBlock() != ModBlocks.PORTAL_DOOR) {
            return false;
        }
        if (state.getValue(BlockPortalDoor.TYPE) != BlockPortalDoor.EnumType.SUB) {
            return false;
        }
        if (state.getValue(BlockPortalDoor.HALF) != BlockPortalDoor.EnumHalf.LOWER) {
            return false;
        }
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(sub.pos);
        if (!(te instanceof com.doraamo.tileentity.TileEntityPortalDoor)) {
            return false;
        }
        com.doraamo.tileentity.TileEntityPortalDoor portal = (com.doraamo.tileentity.TileEntityPortalDoor) te;
        return portal.isSubGate() && expectedMain.equals(portal.getMainRef());
    }

    public void unregisterSub(PortalRef sub) {
        String sk = sub.key();
        String mk = subToMain.remove(sk);
        if (mk != null) {
            Set<String> set = mainToSubs.get(mk);
            if (set != null) {
                set.remove(sk);
                if (set.isEmpty()) {
                    mainToSubs.remove(mk);
                }
            }
        }
        pendingBreak.remove(sk);
        markDirty();
    }

    public PortalRef getMainForSub(PortalRef sub) {
        String mk = subToMain.get(sub.key());
        return mk == null ? null : PortalRef.parse(mk);
    }

    public PortalRef findSubInDimension(PortalRef main, int dim) {
        Set<String> set = mainToSubs.get(main.key());
        if (set == null) {
            return null;
        }
        for (String sk : set) {
            PortalRef sub = PortalRef.parse(sk);
            if (sub.dim == dim && !pendingBreak.contains(sk)) {
                return sub;
            }
        }
        return null;
    }

    public Set<String> getSubs(PortalRef main) {
        Set<String> set = mainToSubs.get(main.key());
        if (set == null) {
            return Collections.emptySet();
        }
        return new HashSet<String>(set);
    }

    /**
     * Queue all bound subs for destruction; destroy immediately if chunk is loaded.
     */
    public void destroySubsForMain(PortalRef main, MinecraftServer server) {
        Set<String> set = mainToSubs.remove(main.key());
        if (set == null || set.isEmpty()) {
            markDirty();
            return;
        }
        for (String sk : set) {
            subToMain.remove(sk);
            pendingBreak.add(sk);
            tryBreakNow(PortalRef.parse(sk), server);
        }
        markDirty();
    }

    public void tryBreakNow(PortalRef sub, MinecraftServer server) {
        if (!DimensionManager.isDimensionRegistered(sub.dim)) {
            return;
        }
        WorldServer world = server.getWorld(sub.dim);
        if (world == null) {
            return;
        }
        if (!world.isBlockLoaded(sub.pos)) {
            return;
        }
        breakSubDoor(world, sub.pos);
        pendingBreak.remove(sub.key());
        markDirty();
    }

    public void onChunkLoad(World world, ChunkPos chunk) {
        if (world.isRemote || pendingBreak.isEmpty()) {
            return;
        }
        java.util.List<String> toBreak = new java.util.ArrayList<String>();
        for (String sk : pendingBreak) {
            PortalRef ref = PortalRef.parse(sk);
            if (ref.dim != world.provider.getDimension()) {
                continue;
            }
            if ((ref.pos.getX() >> 4) != chunk.x || (ref.pos.getZ() >> 4) != chunk.z) {
                continue;
            }
            toBreak.add(sk);
        }
        if (toBreak.isEmpty()) {
            return;
        }
        for (String sk : toBreak) {
            PortalRef ref = PortalRef.parse(sk);
            breakSubDoor(world, ref.pos);
            pendingBreak.remove(sk);
            subToMain.remove(sk);
        }
        markDirty();
    }

    public static void breakSubDoor(World world, BlockPos lower) {
        IBlockState state = world.getBlockState(lower);
        boolean wasSuppress = BlockPortalDoor.suppressSubRepair;
        BlockPortalDoor.suppressSubRepair = true;
        try {
            if (state.getBlock() == ModBlocks.PORTAL_DOOR) {
                world.setBlockToAir(lower.up());
                world.setBlockToAir(lower);
                world.removeTileEntity(lower);
            } else {
                IBlockState up = world.getBlockState(lower.up());
                if (up.getBlock() == ModBlocks.PORTAL_DOOR) {
                    world.setBlockToAir(lower.up());
                }
            }
        } finally {
            BlockPortalDoor.suppressSubRepair = wasSuppress;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        mainToSubs.clear();
        subToMain.clear();
        pendingBreak.clear();

        NBTTagList mains = nbt.getTagList("Mains", 10);
        for (int i = 0; i < mains.tagCount(); i++) {
            NBTTagCompound tag = mains.getCompoundTagAt(i);
            String mk = tag.getString("Main");
            Set<String> set = new HashSet<String>();
            NBTTagList subs = tag.getTagList("Subs", 8);
            for (int j = 0; j < subs.tagCount(); j++) {
                String sk = subs.getStringTagAt(j);
                set.add(sk);
                subToMain.put(sk, mk);
            }
            if (!set.isEmpty()) {
                mainToSubs.put(mk, set);
            }
        }

        NBTTagList pending = nbt.getTagList("PendingBreak", 8);
        for (int i = 0; i < pending.tagCount(); i++) {
            pendingBreak.add(pending.getStringTagAt(i));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList mains = new NBTTagList();
        for (Map.Entry<String, Set<String>> e : mainToSubs.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Main", e.getKey());
            NBTTagList subs = new NBTTagList();
            for (String sk : e.getValue()) {
                subs.appendTag(new NBTTagString(sk));
            }
            tag.setTag("Subs", subs);
            mains.appendTag(tag);
        }
        compound.setTag("Mains", mains);

        NBTTagList pending = new NBTTagList();
        for (String sk : pendingBreak) {
            pending.appendTag(new NBTTagString(sk));
        }
        compound.setTag("PendingBreak", pending);
        return compound;
    }
}
