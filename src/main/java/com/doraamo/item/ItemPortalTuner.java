package com.doraamo.item;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.client.ClientHooks;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalFinder;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.LangKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ItemPortalTuner extends Item {

    public ItemPortalTuner(Properties properties) {
        super(properties);
    }

    public static DestinationSettings getSettings(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Dest")) {
            return DestinationSettings.fromNBT(tag.getCompound("Dest"));
        }
        return new DestinationSettings();
    }

    public static void setSettings(ItemStack stack, DestinationSettings settings) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("Dest", settings.writeToNBT(new CompoundTag()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (world.getBlockState(pos).getBlock() != ModBlocks.PORTAL_DOOR.get()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        InteractionHand hand = context.getHand();
        TileEntityPortalDoor te = BlockPortalDoor.getTile(world, pos, world.getBlockState(pos));
        if (te == null) {
            return InteractionResult.FAIL;
        }
        if (te.isSubGate() || BlockPortalDoor.isSub(world.getBlockState(pos))) {
            if (!world.isClientSide) {
                player.displayClientMessage(Component.translatable(LangKeys.TUNER_SUB_LOCKED), true);
            }
            return InteractionResult.FAIL;
        }
        if (world.isClientSide) {
            BlockPos base = te.getBlockPos();
            DestinationSettings bound = te.getDestination();
            DestinationSettings draft = bound != null ? bound.copy() : getSettings(stack).copy();
            if (draft.mode == DestinationSettings.Mode.SCALED) {
                draft.mode = DestinationSettings.Mode.COORDS;
                draft.x = (int) Math.floor(player.getX());
                draft.y = (int) Math.floor(player.getY());
                draft.z = (int) Math.floor(player.getZ());
                draft.dimension = player.level().dimension().location().toString();
            }
            final InteractionHand openHand = hand;
            final DestinationSettings openDraft = draft;
            final BlockPos openPos = base;
            final boolean hasBinding = bound != null;
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientHooks.openTunerGui(openHand, openDraft, openPos, hasBinding));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            PortalFinder.NearestPortal nearest = PortalFinder.findNearest(world, player);
            if (nearest == null) {
                player.displayClientMessage(Component.translatable(LangKeys.TUNER_NO_NEARBY), true);
            } else {
                String dirKey = PortalFinder.directionKey(player, nearest.pos);
                String typeKey = nearest.subGate ? LangKeys.TUNER_TYPE_SUB : LangKeys.TUNER_TYPE_MAIN;
                player.displayClientMessage(Component.translatable(LangKeys.TUNER_NEAREST,
                        Component.translatable(typeKey),
                        Component.translatable(dirKey),
                        (int) Math.round(nearest.distance)), true);
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
