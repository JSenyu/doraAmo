package com.doraamo.item;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemPortalDoor extends Item {

    private final Block block;

    public ItemPortalDoor(Block block, Properties properties) {
        super(properties);
        this.block = block;
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World world = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return ActionResultType.FAIL;
        }

        BlockPos pos = context.getClickedPos();
        BlockState ground = world.getBlockState(pos);
        if (!ground.canBeReplaced(new BlockItemUseContext(context))) {
            pos = pos.above();
        }

        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack)
                && !player.mayUseItemAt(pos.above(), context.getClickedFace(), stack)) {
            return ActionResultType.FAIL;
        }

        if (!world.getBlockState(pos).canBeReplaced(new BlockItemUseContext(context))
                || !world.getBlockState(pos.above()).canBeReplaced(new BlockItemUseContext(context))) {
            return ActionResultType.FAIL;
        }

        Direction doorFacing = context.getHorizontalDirection();
        BlockState lower = ModBlocks.PORTAL_DOOR.get().defaultBlockState()
                .setValue(BlockPortalDoor.FACING, doorFacing)
                .setValue(BlockPortalDoor.HALF, BlockPortalDoor.Half.LOWER)
                .setValue(BlockPortalDoor.TYPE, BlockPortalDoor.DoorType.MAIN);
        BlockState upper = ModBlocks.PORTAL_DOOR.get().defaultBlockState()
                .setValue(BlockPortalDoor.FACING, doorFacing)
                .setValue(BlockPortalDoor.HALF, BlockPortalDoor.Half.UPPER)
                .setValue(BlockPortalDoor.TYPE, BlockPortalDoor.DoorType.MAIN);

        if (!world.isClientSide) {
            world.setBlock(pos, lower, 3);
            world.setBlock(pos.above(), upper, 3);
            world.updateNeighborsAt(pos, this.block);
            world.updateNeighborsAt(pos.above(), this.block);
        }

        SoundType sound = lower.getSoundType(world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (player == null || !player.abilities.instabuild) {
            stack.shrink(1);
        }
        return ActionResultType.SUCCESS;
    }
}
