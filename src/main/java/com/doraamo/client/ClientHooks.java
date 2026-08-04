package com.doraamo.client;

import com.doraamo.destination.DestinationSettings;
import com.doraamo.portal.PortalDoorPlacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openTunerGui(Hand hand, DestinationSettings draft, BlockPos portalPos,
                                    boolean hasExistingBinding) {
        Minecraft.getInstance().setScreen(new GuiPortalTuner(hand, draft, portalPos, hasExistingBinding));
    }

    public static void handleValidateResult(boolean found, int x, int y, int z, int hazardOrdinal) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof GuiPortalTuner) {
            PortalDoorPlacer.PlaceHazard[] values = PortalDoorPlacer.PlaceHazard.values();
            PortalDoorPlacer.PlaceHazard hazard = hazardOrdinal < values.length
                    ? values[hazardOrdinal]
                    : PortalDoorPlacer.PlaceHazard.WALL;
            ((GuiPortalTuner) screen).onValidateResult(found, x, y, z, hazard);
        }
    }
}
