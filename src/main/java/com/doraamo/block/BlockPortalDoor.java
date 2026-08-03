package com.doraamo.block;

import com.doraamo.DoraAmo;
import com.doraamo.config.DimensionConfig;
import com.doraamo.portal.PortalNetworkData;
import com.doraamo.portal.PortalRef;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.LangKeys;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Random;

public class BlockPortalDoor extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyEnum<EnumHalf> HALF = PropertyEnum.create("half", EnumHalf.class);
    public static final PropertyEnum<EnumType> TYPE = PropertyEnum.create("type", EnumType.class);

    /** When true, sub doors will not auto-repair missing halves (used while sync-destroying). */
    public static boolean suppressSubRepair = false;

    /** Centered thin portal plane (matches block model 7–9/16). */
    private static final AxisAlignedBB NS_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.4375D, 1.0D, 1.0D, 0.5625D);
    private static final AxisAlignedBB EW_AABB = new AxisAlignedBB(0.4375D, 0.0D, 0.0D, 0.5625D, 1.0D, 1.0D);

    public BlockPortalDoor() {
        super(Material.PORTAL);
        setRegistryName(DoraAmo.MODID, "portal_door");
        setUnlocalizedName(DoraAmo.MODID + ".portal_door");
        setHardness(0.5F);
        setResistance(3.0F);
        setSoundType(SoundType.GLASS);
        setLightLevel(0.875F);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(HALF, EnumHalf.LOWER)
                .withProperty(TYPE, EnumType.MAIN));
    }

    public static boolean isSub(IBlockState state) {
        return state.getValue(TYPE) == EnumType.SUB;
    }

    @Override
    public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
        return isSub(blockState) ? -1.0F : this.blockHardness;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (isSub(state) && !player.capabilities.isCreativeMode) {
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return !isSub(state);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    /** Keep translucent faces visible; models already omit the upper/lower join faces. */
    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        return (facing == EnumFacing.EAST || facing == EnumFacing.WEST) ? EW_AABB : NS_AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (entityIn.isRiding() || entityIn.isBeingRidden() || !entityIn.isNonBoss()) {
            return;
        }
        AxisAlignedBB portalBox = getBoundingBox(state, worldIn, pos).offset(pos);
        if (!entityIn.getEntityBoundingBox().intersects(portalBox)) {
            return;
        }

        TileEntityPortalDoor te = getTile(worldIn, pos, state);
        if (te == null || !te.canTeleportFixed(entityIn)) {
            return;
        }

        if (worldIn.isRemote) {
            if (entityIn instanceof EntityPlayer) {
                com.doraamo.client.ClientPortalOverlay.continueCharging((EntityPlayer) entityIn);
            }
            return;
        }

        if (entityIn instanceof EntityPlayerMP) {
            te.tryTeleportPlayer((EntityPlayerMP) entityIn);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return false;
        }
        if (!playerIn.getHeldItem(hand).isEmpty()) {
            return false;
        }
        if (isSub(state)) {
            return true;
        }

        if (worldIn.isRemote) {
            return true;
        }

        TileEntityPortalDoor te = getTile(worldIn, pos, state);
        if (te == null || te.isSubGate()) {
            return true;
        }

        int next = DimensionConfig.nextDimensionId(te.getTargetDimensionId());
        te.setTargetDimensionId(next);

        EntityPlayerMP mp = (EntityPlayerMP) playerIn;
        ITextComponent dimName = DimensionConfig.getDisplayComponent(next);
        ITextComponent subtitle = next == DoraAmo.BLANK_DIMENSION
                ? dimName
                : new TextComponentTranslation(LangKeys.PORTAL_DEST_SCALED, dimName);
        sendSubtitle(mp, subtitle);
        return true;
    }

    private static void sendSubtitle(EntityPlayerMP player, ITextComponent text) {
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TIMES, null, 10, 40, 10));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE, new TextComponentString("")));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE, text));
    }

    @Nullable
    public static TileEntityPortalDoor getTile(World world, BlockPos pos, IBlockState state) {
        BlockPos base = state.getValue(HALF) == EnumHalf.UPPER ? pos.down() : pos;
        TileEntity te = world.getTileEntity(base);
        if (te instanceof TileEntityPortalDoor) {
            return (TileEntityPortalDoor) te;
        }
        return null;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(HALF) == EnumHalf.LOWER;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return state.getValue(HALF) == EnumHalf.LOWER ? new TileEntityPortalDoor() : null;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return createTileEntity(worldIn, getStateFromMeta(meta));
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote && state.getValue(HALF) == EnumHalf.LOWER && !isSub(state)) {
            PortalNetworkData data = PortalNetworkData.get();
            if (data != null && worldIn.getMinecraftServer() != null) {
                data.destroySubsForMain(new PortalRef(worldIn.provider.getDimension(), pos),
                        worldIn.getMinecraftServer());
            }
        }
        if (!worldIn.isRemote && state.getValue(HALF) == EnumHalf.LOWER && isSub(state)) {
            PortalNetworkData data = PortalNetworkData.get();
            if (data != null) {
                data.unregisterSub(new PortalRef(worldIn.provider.getDimension(), pos));
            }
        }
        if (state.getValue(HALF) == EnumHalf.LOWER) {
            worldIn.removeTileEntity(pos);
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, net.minecraft.world.Explosion explosion) {
        IBlockState state = world.getBlockState(pos);
        if (isSub(state)) {
            return;
        }
        super.onBlockExploded(world, pos, explosion);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (isSub(state)) {
            if (suppressSubRepair) {
                return;
            }
            EnumHalf half = state.getValue(HALF);
            BlockPos other = half == EnumHalf.LOWER ? pos.up() : pos.down();
            IBlockState otherState = worldIn.getBlockState(other);
            if (otherState.getBlock() != this) {
                if (!worldIn.isRemote) {
                    EnumFacing facing = state.getValue(FACING);
                    EnumHalf restoreHalf = half == EnumHalf.LOWER ? EnumHalf.UPPER : EnumHalf.LOWER;
                    worldIn.setBlockState(other, getDefaultState()
                            .withProperty(FACING, facing)
                            .withProperty(HALF, restoreHalf)
                            .withProperty(TYPE, EnumType.SUB), 3);
                }
            }
            return;
        }

        EnumHalf half = state.getValue(HALF);
        if (half == EnumHalf.LOWER) {
            if (worldIn.getBlockState(pos.up()).getBlock() != this) {
                worldIn.setBlockToAir(pos);
            }
        } else {
            if (worldIn.getBlockState(pos.down()).getBlock() != this) {
                worldIn.setBlockToAir(pos);
            }
        }
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (isSub(state) && !player.capabilities.isCreativeMode) {
            return;
        }
        EnumHalf half = state.getValue(HALF);
        BlockPos other = half == EnumHalf.LOWER ? pos.up() : pos.down();
        IBlockState otherState = worldIn.getBlockState(other);
        if (otherState.getBlock() == this) {
            boolean wasSuppress = suppressSubRepair;
            if (isSub(state)) {
                suppressSubRepair = true;
            }
            try {
                worldIn.setBlockToAir(other);
            } finally {
                suppressSubRepair = wasSuppress;
            }
        }
        if (!isSub(state) && !worldIn.isRemote && !player.capabilities.isCreativeMode && half == EnumHalf.UPPER) {
            spawnAsEntity(worldIn, pos, new ItemStack(this));
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        IBlockState state = world.getBlockState(pos);
        if (isSub(state)) {
            return player.capabilities.isCreativeMode;
        }
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (isSub(state) || state.getValue(HALF) != EnumHalf.LOWER) {
            return null;
        }
        return Item.getItemFromBlock(this);
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!isSub(state) && state.getValue(HALF) == EnumHalf.LOWER) {
            super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getHorizontal(meta & 3);
        EnumHalf half = (meta & 4) != 0 ? EnumHalf.UPPER : EnumHalf.LOWER;
        EnumType type = (meta & 8) != 0 ? EnumType.SUB : EnumType.MAIN;
        return getDefaultState().withProperty(FACING, facing).withProperty(HALF, half).withProperty(TYPE, type);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(HALF) == EnumHalf.UPPER) {
            meta |= 4;
        }
        if (state.getValue(TYPE) == EnumType.SUB) {
            meta |= 8;
        }
        return meta;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HALF, TYPE);
    }

    public enum EnumHalf implements IStringSerializable {
        LOWER, UPPER;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum EnumType implements IStringSerializable {
        MAIN, SUB;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
