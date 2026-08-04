package com.doraamo.client;

import com.doraamo.DoraAmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

public class ClientPortalOverlay {

    private static boolean touchedThisTick;
    private static int chargeTicks;

    private static Field portalCooldownField;
    private static Field portalTimeField;

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
        try {
            return portalCooldownField.getInt(player);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private static void setPortalTime(Player player, int ticks) {
        try {
            portalTimeField.setInt(player, ticks);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static int getPortalTime(Player player) {
        try {
            return portalTimeField.getInt(player);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    public static void onClientTick(Minecraft client) {
        ensureFields();

        LocalPlayer player = client.player;
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
            setPortalTime(player, chargeTicks);
        } else {
            chargeTicks = 0;
            int current = getPortalTime(player);
            if (current > 0) {
                setPortalTime(player, Math.max(0, current - 1));
            }
        }

        touchedThisTick = false;
    }

    private static void ensureFields() {
        if (portalCooldownField != null) {
            return;
        }
        try {
            portalCooldownField = Entity.class.getDeclaredField("portalCooldown");
            portalCooldownField.setAccessible(true);
            portalTimeField = Entity.class.getDeclaredField("portalTime");
            portalTimeField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            DoraAmo.logger.warn("Could not access portal overlay fields: {}", e.toString());
        }
    }
}
