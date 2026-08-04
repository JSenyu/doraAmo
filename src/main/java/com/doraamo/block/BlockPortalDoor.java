package com.doraamo.block;

import com.doraamo.config.DimensionConfig;
import com.doraamo.portal.PortalNetworkData;
import com.doraamo.portal.PortalRef;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Locale;

public class BlockPortalDoor extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final EnumProperty<DoorType> TYPE = EnumProperty.create("type", DoorType.class);

    public static boolean suppressSubRepair = false;

    private static final VoxelShape NS_SHAPE = Block.box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);
    private static final VoxelShape EW_SHAPE = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);

    public BlockPortalDoor() {
        super(AbstractBlock.Properties.of(Material.PORTAL, MaterialColor.COLOR_LIGHT_BLUE)
                .strength(0.5F, 3.0F)
                .sound(net.minecraft.block.SoundType.GLASS)
                .lightLevel(state -> 14)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.LOWER)
                .setValue(TYPE, DoorType.MAIN));
    }

    public static boolean isSub(BlockState state) {
        return state.getValue(TYPE) == DoorType.SUB;
    }

    @Override
    public float getDestroyProgress(BlockState state, PlayerEntity player, IBlockReader world, BlockPos pos) {
        if (isSub(state) && !player.abilities.instabuild) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, world, pos);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, IBlockReader world, BlockPos pos, Entity entity) {
        return !isSub(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        Direction facing = state.getValue(FACING);
        return (facing == Direction.EAST || facing == Direction.WEST) ? EW_SHAPE : NS_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public void entityInside(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity.isPassenger() || entity.isVehicle() || !entity.canChangeDimensions()) {
            return;
        }
        VoxelShape shape = getShape(state, world, pos, ISelectionContext.of(entity));
        if (!shape.isEmpty()) {
            AxisAlignedBB box = shape.bounds().move(pos);
            if (!entity.getBoundingBox().intersects(box)) {
                return;
            }
        }

        TileEntityPortalDoor te = getTile(world, pos, state);
        if (te == null || !te.canTeleportFixed(entity)) {
            return;
        }

        if (world.isClientSide) {
            if (entity instanceof PlayerEntity) {
                com.doraamo.client.ClientPortalOverlay.continueCharging((PlayerEntity) entity);
            }
            return;
        }

        if (entity instanceof ServerPlayerEntity) {
            te.tryTeleportPlayer((ServerPlayerEntity) entity);
        }
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (hand != Hand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return ActionResultType.PASS;
        }
        if (isSub(state)) {
            return ActionResultType.SUCCESS;
        }
        if (world.isClientSide) {
            return ActionResultType.SUCCESS;
        }

        TileEntityPortalDoor te = getTile(world, pos, state);
        if (te == null || te.isSubGate()) {
            return ActionResultType.SUCCESS;
        }

        String next = DimensionConfig.nextDimension(te.getTargetDimension());
        te.setTargetDimension(next);

        ServerPlayerEntity mp = (ServerPlayerEntity) player;
        ITextComponent dimName = DimensionConfig.getDisplayComponent(next);
        ITextComponent subtitle = DimUtil.isBlank(next)
                ? dimName
                : new TranslationTextComponent(LangKeys.PORTAL_DEST_SCALED, dimName);
        sendSubtitle(mp, subtitle);
        return ActionResultType.SUCCESS;
    }

    private static void sendSubtitle(ServerPlayerEntity player, ITextComponent text) {
        player.connection.send(new STitlePacket(STitlePacket.Type.TIMES, null, 10, 40, 10));
        player.connection.send(new STitlePacket(STitlePacket.Type.TITLE, new StringTextComponent("")));
        player.connection.send(new STitlePacket(STitlePacket.Type.SUBTITLE, text));
    }

    @Nullable
    public static TileEntityPortalDoor getTile(World world, BlockPos pos, BlockState state) {
        BlockPos base = state.getValue(HALF) == Half.UPPER ? pos.below() : pos;
        TileEntity te = world.getBlockEntity(base);
        return te instanceof TileEntityPortalDoor ? (TileEntityPortalDoor) te : null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return state.getValue(HALF) == Half.LOWER;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return state.getValue(HALF) == Half.LOWER ? ModBlocks.PORTAL_DOOR_TILE.get().create() : null;
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide) {
            if (state.getValue(HALF) == Half.LOWER && !isSub(state)) {
                PortalNetworkData data = PortalNetworkData.get();
                if (data != null && world.getServer() != null) {
                    data.destroySubsForMain(new PortalRef(DimUtil.levelKey(world), pos), world.getServer());
                }
            }
            if (state.getValue(HALF) == Half.LOWER && isSub(state)) {
                PortalNetworkData data = PortalNetworkData.get();
                if (data != null) {
                    data.unregisterSub(new PortalRef(DimUtil.levelKey(world), pos));
                }
            }
            if (state.getValue(HALF) == Half.LOWER) {
                world.removeBlockEntity(pos);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public void wasExploded(World world, BlockPos pos, Explosion explosion) {
        if (!isSub(world.getBlockState(pos))) {
            super.wasExploded(world, pos, explosion);
        }
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (isSub(state)) {
            if (suppressSubRepair) {
                return;
            }
            Half half = state.getValue(HALF);
            BlockPos other = half == Half.LOWER ? pos.above() : pos.below();
            if (world.getBlockState(other).getBlock() != this) {
                if (!world.isClientSide) {
                    Direction facing = state.getValue(FACING);
                    Half restoreHalf = half == Half.LOWER ? Half.UPPER : Half.LOWER;
                    world.setBlock(other, defaultBlockState()
                            .setValue(FACING, facing)
                            .setValue(HALF, restoreHalf)
                            .setValue(TYPE, DoorType.SUB), 3);
                }
            }
            return;
        }

        Half half = state.getValue(HALF);
        if (half == Half.LOWER) {
            if (world.getBlockState(pos.above()).getBlock() != this) {
                world.removeBlock(pos, false);
            }
        } else if (world.getBlockState(pos.below()).getBlock() != this) {
            world.removeBlock(pos, false);
        }
    }

    @Override
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (isSub(state) && !player.abilities.instabuild) {
            return;
        }
        Half half = state.getValue(HALF);
        BlockPos other = half == Half.LOWER ? pos.above() : pos.below();
        if (world.getBlockState(other).getBlock() == this) {
            boolean wasSuppress = suppressSubRepair;
            if (isSub(state)) {
                suppressSubRepair = true;
            }
            try {
                world.removeBlock(other, false);
            } finally {
                suppressSubRepair = wasSuppress;
            }
        }
        if (!isSub(state) && !world.isClientSide && !player.abilities.instabuild && half == Half.UPPER) {
            popResource(world, pos, new ItemStack(this));
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockReader world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerWorld world, BlockPos pos, ItemStack stack) {
        if (!isSub(state) && state.getValue(HALF) == Half.LOWER) {
            super.spawnAfterBreak(state, world, pos, stack);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, TYPE);
    }

    public enum Half implements IStringSerializable {
        LOWER, UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum DoorType implements IStringSerializable {
        MAIN, SUB;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
