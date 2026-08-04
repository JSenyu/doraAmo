package com.doraamo.block;

import com.doraamo.config.DimensionConfig;
import com.doraamo.portal.PortalNetworkData;
import com.doraamo.portal.PortalRef;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;
import java.util.Locale;

public class BlockPortalDoor extends BaseEntityBlock {

    public static final MapCodec<BlockPortalDoor> CODEC = simpleCodec(BlockPortalDoor::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final EnumProperty<DoorType> TYPE = EnumProperty.create("type", DoorType.class);

    public static boolean suppressSubRepair = false;

    private static final VoxelShape NS_SHAPE = Block.box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);
    private static final VoxelShape EW_SHAPE = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);

    public BlockPortalDoor(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.LOWER)
                .setValue(TYPE, DoorType.MAIN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static boolean isSub(BlockState state) {
        return state.getValue(TYPE) == DoorType.SUB;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (isSub(state) && !player.getAbilities().instabuild) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return !isSub(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return (facing == Direction.EAST || facing == Direction.WEST) ? EW_SHAPE : NS_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.isPassenger() || entity.isVehicle() || !entity.canUsePortal(true)) {
            return;
        }
        VoxelShape shape = getShape(state, level, pos, CollisionContext.of(entity));
        if (!shape.isEmpty()) {
            AABB box = shape.bounds().move(pos);
            if (!entity.getBoundingBox().intersects(box)) {
                return;
            }
        }

        TileEntityPortalDoor te = getTile(level, pos, state);
        if (te == null || !te.canTeleportFixed(entity)) {
            return;
        }

        if (level.isClientSide) {
            if (entity instanceof Player player) {
                com.doraamo.client.ClientPortalOverlay.continueCharging(player);
            }
            return;
        }

        if (entity instanceof ServerPlayer player) {
            te.tryTeleportPlayer(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (isSub(state)) {
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        TileEntityPortalDoor te = getTile(level, pos, state);
        if (te == null || te.isSubGate()) {
            return InteractionResult.SUCCESS;
        }

        String next = DimensionConfig.nextDimension(te.getTargetDimension());
        te.setTargetDimension(next);

        ServerPlayer mp = (ServerPlayer) player;
        Component dimName = DimensionConfig.getDisplayComponent(next);
        Component subtitle = DimUtil.isBlank(next)
                ? dimName
                : Component.translatable(LangKeys.PORTAL_DEST_SCALED, dimName);
        sendSubtitle(mp, subtitle);
        return InteractionResult.SUCCESS;
    }

    private static void sendSubtitle(ServerPlayer player, Component text) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(text));
    }

    @Nullable
    public static TileEntityPortalDoor getTile(Level level, BlockPos pos, BlockState state) {
        BlockPos base = state.getValue(HALF) == Half.UPPER ? pos.below() : pos;
        BlockEntity te = level.getBlockEntity(base);
        return te instanceof TileEntityPortalDoor portal ? portal : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == Half.LOWER ? ModBlocks.PORTAL_DOOR_TILE.create(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(HALF) != Half.LOWER) {
            return null;
        }
        return createTickerHelper(type, ModBlocks.PORTAL_DOOR_TILE, TileEntityPortalDoor::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (state.getValue(HALF) == Half.LOWER && !isSub(state)) {
                PortalNetworkData data = PortalNetworkData.get();
                if (data != null && level.getServer() != null) {
                    data.destroySubsForMain(new PortalRef(DimUtil.levelKey(level), pos), level.getServer());
                }
            }
            if (state.getValue(HALF) == Half.LOWER && isSub(state)) {
                PortalNetworkData data = PortalNetworkData.get();
                if (data != null) {
                    data.unregisterSub(new PortalRef(DimUtil.levelKey(level), pos));
                }
            }
            if (state.getValue(HALF) == Half.LOWER) {
                level.removeBlockEntity(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!isSub(level.getBlockState(pos))) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (isSub(state)) {
            if (suppressSubRepair) {
                return;
            }
            Half half = state.getValue(HALF);
            BlockPos other = half == Half.LOWER ? pos.above() : pos.below();
            if (level.getBlockState(other).getBlock() != this) {
                if (!level.isClientSide) {
                    Direction facing = state.getValue(FACING);
                    Half restoreHalf = half == Half.LOWER ? Half.UPPER : Half.LOWER;
                    level.setBlock(other, defaultBlockState()
                            .setValue(FACING, facing)
                            .setValue(HALF, restoreHalf)
                            .setValue(TYPE, DoorType.SUB), 3);
                }
            }
            return;
        }

        Half half = state.getValue(HALF);
        if (half == Half.LOWER) {
            if (level.getBlockState(pos.above()).getBlock() != this) {
                level.removeBlock(pos, false);
            }
        } else if (level.getBlockState(pos.below()).getBlock() != this) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (isSub(state) && !player.getAbilities().instabuild) {
            return state;
        }
        Half half = state.getValue(HALF);
        BlockPos other = half == Half.LOWER ? pos.above() : pos.below();
        if (level.getBlockState(other).getBlock() == this) {
            boolean wasSuppress = suppressSubRepair;
            if (isSub(state)) {
                suppressSubRepair = true;
            }
            try {
                level.removeBlock(other, false);
            } finally {
                suppressSubRepair = wasSuppress;
            }
        }
        if (!isSub(state) && !level.isClientSide && !player.getAbilities().instabuild && half == Half.UPPER) {
            popResource(level, pos, new ItemStack(this));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected ItemStack getCloneItemStack(Level level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
        if (!isSub(state) && state.getValue(HALF) == Half.LOWER) {
            super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, TYPE);
    }

    public enum Half implements StringRepresentable {
        LOWER, UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum DoorType implements StringRepresentable {
        MAIN, SUB;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
