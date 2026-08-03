package com.doraamo.client;

import com.doraamo.DoraAmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Drives the vanilla portal overlay ({@code timeInPortal}) at a fixed rate,
 * including creative mode (vanilla would fill the overlay in 1 tick).
 */
@Mod.EventBusSubscriber(modid = DoraAmo.MODID, value = Side.CLIENT)
public class ClientPortalOverlay {

    private static boolean touchedThisTick;
    private static int chargeTicks;

    private ClientPortalOverlay() {
    }

    public static void continueCharging(EntityPlayer player) {
        if (player != Minecraft.getMinecraft().player) {
            return;
        }
        if (player.timeUntilPortal > 0) {
            return;
        }
        touchedThisTick = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            touchedThisTick = false;
            chargeTicks = 0;
            return;
        }

        if (touchedThisTick && player.timeUntilPortal <= 0) {
            chargeTicks++;
            if (chargeTicks > DoraAmo.PORTAL_CHARGE_TICKS) {
                chargeTicks = DoraAmo.PORTAL_CHARGE_TICKS;
            }
            float progress = chargeTicks / (float) DoraAmo.PORTAL_CHARGE_TICKS;
            float prev = Math.max(0.0F, (chargeTicks - 1) / (float) DoraAmo.PORTAL_CHARGE_TICKS);
            player.timeInPortal = progress;
            player.prevTimeInPortal = prev;
        } else {
            chargeTicks = 0;
            if (player.timeInPortal > 0.0F) {
                player.prevTimeInPortal = player.timeInPortal;
                player.timeInPortal = Math.max(0.0F, player.timeInPortal - 0.05F);
            }
        }

        touchedThisTick = false;
    }
}
