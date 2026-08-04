package com.doraamo.item;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.client.GuiPortalTuner;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalFinder;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ItemPortalTuner extends Item {

    public ItemPortalTuner(Properties properties) {
        super(properties);
    }

    public static DestinationSettings getSettings(ItemStack stack) {
        if (!stack.hasTag()) {
            return new DestinationSettings();
        }
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains("Dest")) {
            return DestinationSettings.fromNBT(tag.getCompound("Dest"));
        }
        return new DestinationSettings();
    }

    public static void setSettings(ItemStack stack, DestinationSettings settings) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.put("Dest", settings.writeToNBT(new CompoundNBT()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResultType useOn(net.minecraft.item.ItemUseContext context) {
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (world.getBlockState(pos).getBlock() != ModBlocks.PORTAL_DOOR.get()) {
            return ActionResultType.PASS;
        }
        ItemStack stack = context.getItemInHand();
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResultType.PASS;
        }
        Hand hand = context.getHand();
        TileEntityPortalDoor te = BlockPortalDoor.getTile(world, pos, world.getBlockState(pos));
        if (te == null) {
            return ActionResultType.FAIL;
        }
        if (te.isSubGate() || BlockPortalDoor.isSub(world.getBlockState(pos))) {
            if (!world.isClientSide) {
                player.displayClientMessage(new TranslationTextComponent(LangKeys.TUNER_SUB_LOCKED), true);
            }
            return ActionResultType.FAIL;
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
                draft.dimension = player.level.dimension().location().toString();
            }
            openGui(hand, stack, base, draft, bound != null);
        }
        return ActionResultType.SUCCESS;
    }

    private static void openGui(Hand hand, ItemStack stack, BlockPos portalPos,
                                DestinationSettings draft, boolean hasExistingBinding) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new GuiPortalTuner(hand, draft, portalPos, hasExistingBinding)));
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            PortalFinder.NearestPortal nearest = PortalFinder.findNearest(world, player);
            if (nearest == null) {
                player.displayClientMessage(new TranslationTextComponent(LangKeys.TUNER_NO_NEARBY), true);
            } else {
                String dirKey = PortalFinder.directionKey(player, nearest.pos);
                String typeKey = nearest.subGate ? LangKeys.TUNER_TYPE_SUB : LangKeys.TUNER_TYPE_MAIN;
                player.displayClientMessage(new TranslationTextComponent(LangKeys.TUNER_NEAREST,
                        new TranslationTextComponent(typeKey),
                        new TranslationTextComponent(dirKey),
                        (int) Math.round(nearest.distance)), true);
            }
        }
        return ActionResult.success(stack);
    }
}
