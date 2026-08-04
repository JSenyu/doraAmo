package com.doraamo.item;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPortalDoor extends Item {

    private final Block block;

    public ItemPortalDoor(Block block, Properties properties) {
        super(properties);
        this.block = block;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos();
        BlockState ground = level.getBlockState(pos);
        if (!ground.canBeReplaced(new BlockPlaceContext(context))) {
            pos = pos.above();
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack)
                && !player.mayUseItemAt(pos.above(), context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }

        if (!level.getBlockState(pos).canBeReplaced(new BlockPlaceContext(context))
                || !level.getBlockState(pos.above()).canBeReplaced(new BlockPlaceContext(context))) {
            return InteractionResult.FAIL;
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

        if (!level.isClientSide) {
            level.setBlock(pos, lower, 3);
            level.setBlock(pos.above(), upper, 3);
            level.updateNeighborsAt(pos, this.block);
            level.updateNeighborsAt(pos.above(), this.block);
        }

        SoundType sound = lower.getSoundType(level, pos, player);
        level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
