package com.doraamo.portal;

import com.doraamo.DoraAmo;
import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PortalNetworkData extends SavedData {

    public static final String DATA_NAME = DoraAmo.MODID + "_portals";

    private final Map<String, Set<String>> mainToSubs = new HashMap<>();
    private final Map<String, String> subToMain = new HashMap<>();
    private final Set<String> pendingBreak = new HashSet<>();

    public PortalNetworkData() {
    }

    public static PortalNetworkData load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        PortalNetworkData data = new PortalNetworkData();
        data.readFromNbt(nbt);
        return data;
    }

    public static PortalNetworkData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PortalNetworkData::new, PortalNetworkData::load, DataFixTypes.SAVED_DATA_MAP_DATA),
                DATA_NAME);
    }

    public static PortalNetworkData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : get(server);
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

    public boolean isValidSubDoor(ServerLevel world, PortalRef sub, PortalRef expectedMain) {
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
        BlockEntity te = world.getBlockEntity(sub.pos);
        if (!(te instanceof TileEntityPortalDoor portal)) {
            return false;
        }
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
        ServerLevel world = DimUtil.getLevel(server, sub.dim);
        if (world == null || !world.isLoaded(sub.pos)) {
            return;
        }
        breakSubDoor(world, sub.pos);
        pendingBreak.remove(sub.key());
        setDirty();
    }

    public void onChunkLoad(Level world, ChunkPos chunk) {
        if (world.isClientSide() || pendingBreak.isEmpty()) {
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

    public static void breakSubDoor(Level world, BlockPos lower) {
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

    public void readFromNbt(CompoundTag nbt) {
        mainToSubs.clear();
        subToMain.clear();
        pendingBreak.clear();

        ListTag mains = nbt.getList("Mains", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < mains.size(); i++) {
            CompoundTag tag = mains.getCompound(i);
            String mk = tag.getString("Main");
            Set<String> set = new HashSet<>();
            ListTag subs = tag.getList("Subs", StringTag.TAG_STRING);
            for (int j = 0; j < subs.size(); j++) {
                String sk = subs.getString(j);
                set.add(sk);
                subToMain.put(sk, mk);
            }
            if (!set.isEmpty()) {
                mainToSubs.put(mk, set);
            }
        }

        ListTag pending = nbt.getList("PendingBreak", StringTag.TAG_STRING);
        for (int i = 0; i < pending.size(); i++) {
            pendingBreak.add(pending.getString(i));
        }
    }

    @Override
    public CompoundTag save(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag mains = new ListTag();
        for (Map.Entry<String, Set<String>> e : mainToSubs.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Main", e.getKey());
            ListTag subs = new ListTag();
            for (String sk : e.getValue()) {
                subs.add(StringTag.valueOf(sk));
            }
            tag.put("Subs", subs);
            mains.add(tag);
        }
        compound.put("Mains", mains);

        ListTag pending = new ListTag();
        for (String sk : pendingBreak) {
            pending.add(StringTag.valueOf(sk));
        }
        compound.put("PendingBreak", pending);
        return compound;
    }
}
