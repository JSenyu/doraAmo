package com.doraamo.portal;

import com.doraamo.DoraAmo;
import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PortalNetworkData extends WorldSavedData {

    public static final String DATA_NAME = DoraAmo.MODID + "_portals";

    private final Map<String, Set<String>> mainToSubs = new HashMap<>();
    private final Map<String, String> subToMain = new HashMap<>();
    private final Set<String> pendingBreak = new HashSet<>();

    public PortalNetworkData() {
        super(DATA_NAME);
    }

    public static PortalNetworkData get(MinecraftServer server) {
        ServerWorld overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(PortalNetworkData::new, DATA_NAME);
    }

    public static PortalNetworkData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : get(server);
    }

    @Override
    public void load(CompoundNBT nbt) {
        readFromNbt(nbt);
    }

    public void registerSub(PortalRef main, PortalRef sub) {
        String mk = main.key();
        String sk = sub.key();
        Set<String> set = mainToSubs.computeIfAbsent(mk, k -> new HashSet<>());

        java.util.List<String> replaced = new java.util.ArrayList<>();
        for (String existing : set) {
            PortalRef other = PortalRef.parse(existing);
            if (other.dim.equals(sub.dim) && !existing.equals(sk)) {
                replaced.add(existing);
            }
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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
        setDirty();
    }

    public boolean isValidSubDoor(ServerWorld world, PortalRef sub, PortalRef expectedMain) {
        if (!world.isLoaded(sub.pos)) {
            world.getChunk(sub.pos);
        }
        BlockState state = world.getBlockState(sub.pos);
        if (state.getBlock() != ModBlocks.PORTAL_DOOR.get()) {
            return false;
        }
        if (state.getValue(BlockPortalDoor.TYPE) != BlockPortalDoor.DoorType.SUB) {
            return false;
        }
        if (state.getValue(BlockPortalDoor.HALF) != BlockPortalDoor.Half.LOWER) {
            return false;
        }
        net.minecraft.tileentity.TileEntity te = world.getBlockEntity(sub.pos);
        if (!(te instanceof TileEntityPortalDoor)) {
            return false;
        }
        TileEntityPortalDoor portal = (TileEntityPortalDoor) te;
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
        setDirty();
    }

    public PortalRef findSubInDimension(PortalRef main, String dim) {
        Set<String> set = mainToSubs.get(main.key());
        if (set == null) {
            return null;
        }
        String norm = DimUtil.normalize(dim);
        for (String sk : set) {
            PortalRef sub = PortalRef.parse(sk);
            if (sub.dim.equals(norm) && !pendingBreak.contains(sk)) {
                return sub;
            }
        }
        return null;
    }

    public void destroySubsForMain(PortalRef main, MinecraftServer server) {
        Set<String> set = mainToSubs.remove(main.key());
        if (set == null || set.isEmpty()) {
            setDirty();
            return;
        }
        for (String sk : set) {
            subToMain.remove(sk);
            pendingBreak.add(sk);
            tryBreakNow(PortalRef.parse(sk), server);
        }
        setDirty();
    }

    public void tryBreakNow(PortalRef sub, MinecraftServer server) {
        ServerWorld world = DimUtil.getLevel(server, sub.dim);
        if (world == null || !world.isLoaded(sub.pos)) {
            return;
        }
        breakSubDoor(world, sub.pos);
        pendingBreak.remove(sub.key());
        setDirty();
    }

    public void onChunkLoad(World world, ChunkPos chunk) {
        if (world.isClientSide || pendingBreak.isEmpty()) {
            return;
        }
        String worldDim = DimUtil.levelKey(world);
        java.util.List<String> toBreak = new java.util.ArrayList<>();
        for (String sk : pendingBreak) {
            PortalRef ref = PortalRef.parse(sk);
            if (!ref.dim.equals(worldDim)) {
                continue;
            }
            if ((ref.pos.getX() >> 4) != chunk.x || (ref.pos.getZ() >> 4) != chunk.z) {
                continue;
            }
            toBreak.add(sk);
        }
        for (String sk : toBreak) {
            PortalRef ref = PortalRef.parse(sk);
            breakSubDoor(world, ref.pos);
            pendingBreak.remove(sk);
            subToMain.remove(sk);
        }
        if (!toBreak.isEmpty()) {
            setDirty();
        }
    }

    public static void breakSubDoor(World world, BlockPos lower) {
        BlockState state = world.getBlockState(lower);
        boolean wasSuppress = BlockPortalDoor.suppressSubRepair;
        BlockPortalDoor.suppressSubRepair = true;
        try {
            if (state.getBlock() == ModBlocks.PORTAL_DOOR.get()) {
                world.removeBlock(lower.above(), false);
                world.removeBlock(lower, false);
                world.removeBlockEntity(lower);
            } else {
                BlockState up = world.getBlockState(lower.above());
                if (up.getBlock() == ModBlocks.PORTAL_DOOR.get()) {
                    world.removeBlock(lower.above(), false);
                }
            }
        } finally {
            BlockPortalDoor.suppressSubRepair = wasSuppress;
        }
    }

    public void readFromNbt(CompoundNBT nbt) {
        mainToSubs.clear();
        subToMain.clear();
        pendingBreak.clear();

        ListNBT mains = nbt.getList("Mains", 10);
        for (int i = 0; i < mains.size(); i++) {
            CompoundNBT tag = mains.getCompound(i);
            String mk = tag.getString("Main");
            Set<String> set = new HashSet<>();
            ListNBT subs = tag.getList("Subs", 8);
            for (int j = 0; j < subs.size(); j++) {
                String sk = subs.getString(j);
                set.add(sk);
                subToMain.put(sk, mk);
            }
            if (!set.isEmpty()) {
                mainToSubs.put(mk, set);
            }
        }

        ListNBT pending = nbt.getList("PendingBreak", 8);
        for (int i = 0; i < pending.size(); i++) {
            pendingBreak.add(pending.getString(i));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        ListNBT mains = new ListNBT();
        for (Map.Entry<String, Set<String>> e : mainToSubs.entrySet()) {
            CompoundNBT tag = new CompoundNBT();
            tag.putString("Main", e.getKey());
            ListNBT subs = new ListNBT();
            for (String sk : e.getValue()) {
                subs.add(StringNBT.valueOf(sk));
            }
            tag.put("Subs", subs);
            mains.add(tag);
        }
        compound.put("Mains", mains);

        ListNBT pending = new ListNBT();
        for (String sk : pendingBreak) {
            pending.add(StringNBT.valueOf(sk));
        }
        compound.put("PendingBreak", pending);
        return compound;
    }
}
