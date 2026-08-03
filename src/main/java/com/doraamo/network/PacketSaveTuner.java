package com.doraamo.network;

import com.doraamo.block.BlockPortalDoor;
import com.doraamo.block.ModBlocks;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.item.ItemPortalTuner;
import com.doraamo.item.ModItems;
import com.doraamo.tileentity.TileEntityPortalDoor;
import com.doraamo.util.LangKeys;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSaveTuner implements IMessage {

    private int hand;
    private DestinationSettings settings = new DestinationSettings();
    private int portalX;
    private int portalY;
    private int portalZ;
    private boolean force;

    public PacketSaveTuner() {
    }

    public PacketSaveTuner(EnumHand hand, DestinationSettings settings, BlockPos portalPos, boolean force) {
        this.hand = hand == EnumHand.OFF_HAND ? 1 : 0;
        this.settings = settings;
        this.portalX = portalPos.getX();
        this.portalY = portalPos.getY();
        this.portalZ = portalPos.getZ();
        this.force = force;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        hand = buf.readByte();
        settings.dimensionId = buf.readInt();
        settings.mode = DestinationSettings.Mode.values()[buf.readByte()];
        settings.biomeId = buf.readInt();
        settings.structureName = ByteBufUtils.readUTF8String(buf);
        settings.x = buf.readInt();
        settings.y = buf.readInt();
        settings.z = buf.readInt();
        portalX = buf.readInt();
        portalY = buf.readInt();
        portalZ = buf.readInt();
        force = buf.readBoolean();
        settings.forceUnsafe = force;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(hand);
        buf.writeInt(settings.dimensionId);
        buf.writeByte(settings.mode.ordinal());
        buf.writeInt(settings.biomeId);
        ByteBufUtils.writeUTF8String(buf, settings.structureName == null ? "Village" : settings.structureName);
        buf.writeInt(settings.x);
        buf.writeInt(settings.y);
        buf.writeInt(settings.z);
        buf.writeInt(portalX);
        buf.writeInt(portalY);
        buf.writeInt(portalZ);
        buf.writeBoolean(force);
    }

    public static class Handler implements IMessageHandler<PacketSaveTuner, IMessage> {
        @Override
        public IMessage onMessage(final PacketSaveTuner message, final MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    EnumHand h = message.hand == 1 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
                    ItemStack stack = player.getHeldItem(h);
                    if (stack.getItem() != ModItems.PORTAL_TUNER) {
                        return;
                    }
                    message.settings.forceUnsafe = message.force;
                    ItemPortalTuner.setSettings(stack, message.settings);

                    BlockPos portalPos = new BlockPos(message.portalX, message.portalY, message.portalZ);
                    if (player.world.getBlockState(portalPos).getBlock() != ModBlocks.PORTAL_DOOR) {
                        return;
                    }
                    TileEntityPortalDoor te = BlockPortalDoor.getTile(player.world, portalPos,
                            player.world.getBlockState(portalPos));
                    if (te == null || te.isSubGate()) {
                        player.sendStatusMessage(new TextComponentTranslation(LangKeys.TUNER_SUB_LOCKED), true);
                        return;
                    }
                    boolean hadBinding = te.getDestination() != null;
                    te.setDestination(message.settings);
                    if (hadBinding) {
                        player.sendStatusMessage(new TextComponentTranslation(LangKeys.TUNER_OVERWRITE), true);
                    } else {
                        player.sendStatusMessage(new TextComponentTranslation(LangKeys.TUNER_APPLIED), true);
                    }
                }
            });
            return null;
        }
    }
}
