package com.doraamo.item;

import com.doraamo.DoraAmo;
import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalFinder;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.LangKeys;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPortalTuner extends Item {

    public ItemPortalTuner() {
        setRegistryName(DoraAmo.MODID, "portal_tuner");
        setUnlocalizedName(DoraAmo.MODID + ".portal_tuner");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    public static DestinationSettings getSettings(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return new DestinationSettings();
        }
        return DestinationSettings.fromNBT(stack.getTagCompound().getCompoundTag("Dest"));
    }

    public static void setSettings(ItemStack stack, DestinationSettings settings) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setTag("Dest", settings.writeToNBT(new NBTTagCompound()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.getBlockState(pos).getBlock() != ModBlocks.PORTAL_DOOR) {
            return EnumActionResult.PASS;
        }
        ItemStack stack = player.getHeldItem(hand);
        TileEntityPortalDoor te = BlockPortalDoor.getTile(worldIn, pos, worldIn.getBlockState(pos));
        if (te == null) {
            return EnumActionResult.FAIL;
        }
        if (te.isSubGate() || BlockPortalDoor.isSub(worldIn.getBlockState(pos))) {
            if (!worldIn.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation(LangKeys.TUNER_SUB_LOCKED), true);
            }
            return EnumActionResult.FAIL;
        }
        if (worldIn.isRemote) {
            BlockPos base = te.getPos();
            DestinationSettings bound = te.getDestination();
            DestinationSettings draft = bound != null ? bound.copy() : getSettings(stack).copy();
            if (draft.mode == DestinationSettings.Mode.SCALED) {
                draft.mode = DestinationSettings.Mode.COORDS;
                draft.x = (int) Math.floor(player.posX);
                draft.y = (int) Math.floor(player.posY);
                draft.z = (int) Math.floor(player.posZ);
                draft.dimensionId = player.dimension;
            }
            DoraAmo.proxy.openTunerGui(hand, stack, base, draft, bound != null);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote) {
            PortalFinder.NearestPortal nearest = PortalFinder.findNearest(worldIn, playerIn);
            if (nearest == null) {
                playerIn.sendStatusMessage(new TextComponentTranslation(LangKeys.TUNER_NO_NEARBY), true);
            } else {
                String dirKey = PortalFinder.directionKey(playerIn, nearest.pos);
                String typeKey = nearest.subGate ? LangKeys.TUNER_TYPE_SUB : LangKeys.TUNER_TYPE_MAIN;
                playerIn.sendStatusMessage(new TextComponentTranslation(LangKeys.TUNER_NEAREST,
                        new TextComponentTranslation(typeKey),
                        new TextComponentTranslation(dirKey),
                        Integer.valueOf((int) Math.round(nearest.distance))), true);
            }
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
