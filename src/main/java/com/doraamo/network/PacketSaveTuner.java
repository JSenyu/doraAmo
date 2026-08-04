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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSaveTuner {

    private InteractionHand hand;
    private DestinationSettings settings = new DestinationSettings();
    private BlockPos portalPos = BlockPos.ZERO;
    private boolean force;

    public PacketSaveTuner() {
    }

    public PacketSaveTuner(InteractionHand hand, DestinationSettings settings, BlockPos portalPos, boolean force) {
        this.hand = hand;
        this.settings = settings;
        this.portalPos = portalPos;
        this.force = force;
    }

    public static void encode(PacketSaveTuner msg, FriendlyByteBuf buf) {
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

    public static PacketSaveTuner decode(FriendlyByteBuf buf) {
        PacketSaveTuner msg = new PacketSaveTuner();
        msg.hand = buf.readEnum(InteractionHand.class);
        msg.settings.dimension = DimUtil.normalize(buf.readUtf());
        msg.settings.mode = buf.readEnum(DestinationSettings.Mode.class);
        msg.settings.biomeKey = buf.readUtf();
        msg.settings.structureName = buf.readUtf();
        msg.settings.x = buf.readInt();
        msg.settings.y = buf.readInt();
        msg.settings.z = buf.readInt();
        msg.portalPos = buf.readBlockPos();
        msg.force = buf.readBoolean();
        msg.settings.forceUnsafe = msg.force;
        return msg;
    }

    public static void handle(PacketSaveTuner msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
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
        ctx.get().setPacketHandled(true);
    }
}
