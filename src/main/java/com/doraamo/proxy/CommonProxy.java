package com.doraamo.proxy;

import com.doraamo.destination.DestinationSettings;
import com.doraamo.network.PacketHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public class CommonProxy {

    public void preInit() {
        PacketHandler.init();
    }

    public void init() {
    }

    public void postInit() {
    }

    public void openTunerGui(EnumHand hand, ItemStack stack, BlockPos portalPos,
                             DestinationSettings draft, boolean hasExistingBinding) {
    }
}
