package com.doraamo.client;

import com.doraamo.DoraAmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

@Mod.EventBusSubscriber(modid = DoraAmo.MODID, value = Dist.CLIENT)
public class ClientPortalOverlay {

    private static boolean touchedThisTick;
    private static int chargeTicks;

    private ClientPortalOverlay() {
    }

    public static void continueCharging(Player player) {
        if (player != Minecraft.getInstance().player) {
            return;
        }
        if (getPortalCooldown(player) > 0) {
            return;
        }
        touchedThisTick = true;
    }

    private static int getPortalCooldown(Player player) {
        return ObfuscationReflectionHelper.getPrivateValue(Player.class, player, "portalCooldown");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            touchedThisTick = false;
            chargeTicks = 0;
            return;
        }

        if (touchedThisTick && getPortalCooldown(player) <= 0) {
            chargeTicks++;
            if (chargeTicks > DoraAmo.PORTAL_CHARGE_TICKS) {
                chargeTicks = DoraAmo.PORTAL_CHARGE_TICKS;
            }
            float progress = chargeTicks / (float) DoraAmo.PORTAL_CHARGE_TICKS;
            float prev = Math.max(0.0F, (chargeTicks - 1) / (float) DoraAmo.PORTAL_CHARGE_TICKS);
            ObfuscationReflectionHelper.setPrivateValue(Player.class, player, progress, "portalTime");
            ObfuscationReflectionHelper.setPrivateValue(Player.class, player, prev, "oPortalTime");
        } else {
            chargeTicks = 0;
            float current = ObfuscationReflectionHelper.getPrivateValue(Player.class, player, "portalTime");
            if (current > 0.0F) {
                ObfuscationReflectionHelper.setPrivateValue(Player.class, player, current, "oPortalTime");
                ObfuscationReflectionHelper.setPrivateValue(Player.class, player, Math.max(0.0F, current - 0.05F), "portalTime");
            }
        }

        touchedThisTick = false;
    }
}
