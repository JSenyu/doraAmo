package com.doraamo.network;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.item.ItemPortalTuner;
import com.doraamo.item.ModItems;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveTunerPayload(
        InteractionHand hand,
        DestinationSettings settings,
        BlockPos portalPos,
        boolean force) implements CustomPacketPayload {

    public static final Type<SaveTunerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(com.doraamo.DoraAmo.MODID, "save_tuner"));

    public static final StreamCodec<FriendlyByteBuf, SaveTunerPayload> STREAM_CODEC = StreamCodec.of(
            SaveTunerPayload::write,
            SaveTunerPayload::read);

    private static void write(FriendlyByteBuf buf, SaveTunerPayload msg) {
        buf.writeEnum(msg.hand);
        buf.writeUtf(msg.settings.dimension == null ? "" : msg.settings.dimension);
        buf.writeEnum(msg.settings.mode);
        buf.writeUtf(msg.settings.biomeKey == null ? "minecraft:plains" : msg.settings.biomeKey);
        buf.writeUtf(msg.settings.structureName == null ? "Village" : msg.settings.structureName);
        buf.writeInt(msg.settings.x);
        buf.writeInt(msg.settings.y);
        buf.writeInt(msg.settings.z);
        buf.writeBlockPos(msg.portalPos);
        buf.writeBoolean(msg.force);
    }

    private static SaveTunerPayload read(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        DestinationSettings settings = new DestinationSettings();
        settings.dimension = DimUtil.normalize(buf.readUtf());
        settings.mode = buf.readEnum(DestinationSettings.Mode.class);
        settings.biomeKey = buf.readUtf();
        settings.structureName = buf.readUtf();
        settings.x = buf.readInt();
        settings.y = buf.readInt();
        settings.z = buf.readInt();
        BlockPos portalPos = buf.readBlockPos();
        boolean force = buf.readBoolean();
        settings.forceUnsafe = force;
        return new SaveTunerPayload(hand, settings, portalPos, force);
    }

    public static void handle(SaveTunerPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ItemStack stack = player.getItemInHand(msg.hand);
            if (stack.getItem() != ModItems.PORTAL_TUNER.get()) {
                return;
            }
            msg.settings.forceUnsafe = msg.force;
            ItemPortalTuner.setSettings(stack, msg.settings);

            if (player.level().getBlockState(msg.portalPos).getBlock() != ModBlocks.PORTAL_DOOR.get()) {
                return;
            }
            TileEntityPortalDoor te = BlockPortalDoor.getTile(player.level(), msg.portalPos,
                    player.level().getBlockState(msg.portalPos));
            if (te == null || te.isSubGate()) {
                player.displayClientMessage(Component.translatable(LangKeys.TUNER_SUB_LOCKED), true);
                return;
            }
            boolean hadBinding = te.getDestination() != null;
            te.setDestination(msg.settings);
            if (hadBinding) {
                player.displayClientMessage(Component.translatable(LangKeys.TUNER_OVERWRITE), true);
            } else {
                player.displayClientMessage(Component.translatable(LangKeys.TUNER_APPLIED), true);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
