package com.doraamo.client;

import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalDoorPlacer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

@Environment(EnvType.CLIENT)
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openTunerGui(InteractionHand hand, DestinationSettings draft, BlockPos portalPos,
                                    boolean hasExistingBinding) {
        Minecraft.getInstance().setScreen(new GuiPortalTuner(hand, draft, portalPos, hasExistingBinding));
    }

    public static void handleValidateResult(boolean found, int x, int y, int z, int hazardOrdinal) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof GuiPortalTuner tuner) {
            PortalDoorPlacer.PlaceHazard[] values = PortalDoorPlacer.PlaceHazard.values();
            PortalDoorPlacer.PlaceHazard hazard = hazardOrdinal < values.length
                    ? values[hazardOrdinal]
                    : PortalDoorPlacer.PlaceHazard.WALL;
            tuner.onValidateResult(found, x, y, z, hazard);
        }
    }
}
