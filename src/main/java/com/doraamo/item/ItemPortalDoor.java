package com.doraamo.item;

import com.doraamo.block.BlockPortalDoor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemPortalDoor extends ItemBlock {

    public ItemPortalDoor(Block block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing != EnumFacing.UP) {
            return EnumActionResult.FAIL;
        }

        IBlockState ground = worldIn.getBlockState(pos);
        Block groundBlock = ground.getBlock();
        if (!groundBlock.isReplaceable(worldIn, pos)) {
            pos = pos.up();
        }

        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, facing, stack) || !player.canPlayerEdit(pos.up(), facing, stack)) {
            return EnumActionResult.FAIL;
        }

        if (!worldIn.getBlockState(pos).getBlock().isReplaceable(worldIn, pos)
                || !worldIn.getBlockState(pos.up()).getBlock().isReplaceable(worldIn, pos.up())) {
            return EnumActionResult.FAIL;
        }

        EnumFacing doorFacing = player.getHorizontalFacing();
        IBlockState lower = this.block.getDefaultState()
                .withProperty(BlockPortalDoor.FACING, doorFacing)
                .withProperty(BlockPortalDoor.HALF, BlockPortalDoor.EnumHalf.LOWER)
                .withProperty(BlockPortalDoor.TYPE, BlockPortalDoor.EnumType.MAIN);
        IBlockState upper = this.block.getDefaultState()
                .withProperty(BlockPortalDoor.FACING, doorFacing)
                .withProperty(BlockPortalDoor.HALF, BlockPortalDoor.EnumHalf.UPPER)
                .withProperty(BlockPortalDoor.TYPE, BlockPortalDoor.EnumType.MAIN);

        if (!worldIn.isRemote) {
            worldIn.setBlockState(pos, lower, 3);
            worldIn.setBlockState(pos.up(), upper, 3);
            worldIn.notifyNeighborsOfStateChange(pos, this.block, false);
            worldIn.notifyNeighborsOfStateChange(pos.up(), this.block, false);
        }

        SoundType sound = this.block.getSoundType(lower, worldIn, pos, player);
        worldIn.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                                EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        return false;
    }
}
