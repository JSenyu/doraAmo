package com.doraamo.block;

import com.doraamo.DoraAmo;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Soft foothold pad under portals. Breakable by hand with no drop;
 * Silk Touch yields the block item.
 */
public class BlockObsidianTurf extends Block {

    public BlockObsidianTurf() {
        super(Material.GRASS);
        setRegistryName(DoraAmo.MODID, "obsidian_turf");
        setUnlocalizedName(DoraAmo.MODID + ".obsidian_turf");
        setHardness(0.2F);
        setResistance(0.2F);
        setSoundType(SoundType.GROUND);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, net.minecraft.world.IBlockAccess world, BlockPos pos,
                         IBlockState state, int fortune) {
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    /** Always allow harvest path so bare-hand break works and silk touch on any tool drops. */
    @Override
    public boolean canHarvestBlock(net.minecraft.world.IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity te, ItemStack stack) {
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
        if (!stack.isEmpty() && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0) {
            ItemStack drop = new ItemStack(this);
            if (!drop.isEmpty()) {
                spawnAsEntity(worldIn, pos, drop);
            }
        }
    }
}
