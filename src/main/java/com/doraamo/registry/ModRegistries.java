package com.doraamo.registry;

import com.doraamo.DoraAmo;
import com.doraamo.block.BlockObsidianTurf;
import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.item.ItemPortalDoor;
import com.doraamo.item.ItemPortalTuner;
import com.doraamo.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class ModRegistries {

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        ModBlocks.PORTAL_DOOR = new BlockPortalDoor();
        ModBlocks.OBSIDIAN_TURF = new BlockObsidianTurf();
        event.getRegistry().register(ModBlocks.PORTAL_DOOR);
        event.getRegistry().register(ModBlocks.OBSIDIAN_TURF);
        ModBlocks.registerTileEntities();
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        ModItems.PORTAL_DOOR = new ItemPortalDoor(ModBlocks.PORTAL_DOOR)
                .setRegistryName(ModBlocks.PORTAL_DOOR.getRegistryName())
                .setUnlocalizedName(ModBlocks.PORTAL_DOOR.getUnlocalizedName().substring(5))
                .setCreativeTab(CreativeTabs.TRANSPORTATION);
        event.getRegistry().register(ModItems.PORTAL_DOOR);

        ModItems.PORTAL_TUNER = new ItemPortalTuner();
        event.getRegistry().register(ModItems.PORTAL_TUNER);

        ModItems.OBSIDIAN_TURF = new ItemBlock(ModBlocks.OBSIDIAN_TURF)
                .setRegistryName(ModBlocks.OBSIDIAN_TURF.getRegistryName())
                .setUnlocalizedName(ModBlocks.OBSIDIAN_TURF.getUnlocalizedName().substring(5))
                .setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        event.getRegistry().register(ModItems.OBSIDIAN_TURF);
    }

    @SubscribeEvent
    public static void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {
        Ingredient obs = Ingredient.fromItem(Item.getItemFromBlock(Blocks.OBSIDIAN));
        Ingredient diamond = Ingredient.fromItem(Items.DIAMOND);
        Ingredient dragonHead = Ingredient.fromStacks(new ItemStack(Items.SKULL, 1, 5));
        Ingredient diamondBlock = Ingredient.fromItem(Item.getItemFromBlock(Blocks.DIAMOND_BLOCK));
        Ingredient netherStar = Ingredient.fromItem(Items.NETHER_STAR);

        NonNullList<Ingredient> doorIng = NonNullList.create();
        doorIng.add(diamond);
        doorIng.add(obs);
        doorIng.add(diamond);
        doorIng.add(obs);
        doorIng.add(dragonHead);
        doorIng.add(obs);
        doorIng.add(diamond);
        doorIng.add(obs);
        doorIng.add(diamond);
        ShapedRecipes doorRecipe = new ShapedRecipes(DoraAmo.MODID, 3, 3, doorIng, new ItemStack(ModItems.PORTAL_DOOR));
        doorRecipe.setRegistryName(new ResourceLocation(DoraAmo.MODID, "portal_door"));
        event.getRegistry().register(doorRecipe);

        NonNullList<Ingredient> tunerIng = NonNullList.create();
        tunerIng.add(diamondBlock);
        tunerIng.add(netherStar);
        tunerIng.add(obs);
        ShapedRecipes tunerRecipe = new ShapedRecipes(DoraAmo.MODID, 1, 3, tunerIng, new ItemStack(ModItems.PORTAL_TUNER));
        tunerRecipe.setRegistryName(new ResourceLocation(DoraAmo.MODID, "portal_tuner"));
        event.getRegistry().register(tunerRecipe);
    }
}
