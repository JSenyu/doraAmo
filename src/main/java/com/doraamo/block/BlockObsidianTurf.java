package com.doraamo.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockObsidianTurf extends Block {

    public BlockObsidianTurf(Properties properties) {
        super(properties);
    }

    @Override
    public void playerDestroy(World world, PlayerEntity player, BlockPos pos, BlockState state,
                              @Nullable TileEntity te, ItemStack stack) {
        super.playerDestroy(world, player, pos, state, te, stack);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0) {
            popResource(world, pos, new ItemStack(this));
        }
    }
}
