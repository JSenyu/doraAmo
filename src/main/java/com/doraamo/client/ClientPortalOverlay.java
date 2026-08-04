package com.doraamo.client;

import com.doraamo.DoraAmo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

@Mod.EventBusSubscriber(modid = DoraAmo.MODID, value = Dist.CLIENT)
public class ClientPortalOverlay {

    private static boolean touchedThisTick;
    private static int chargeTicks;

    private ClientPortalOverlay() {
    }

    public static void continueCharging(PlayerEntity player) {
        if (player != Minecraft.getInstance().player) {
            return;
        }
        if (getPortalCooldown(player) > 0) {
            return;
        }
        touchedThisTick = true;
    }

    private static int getPortalCooldown(PlayerEntity player) {
        return ObfuscationReflectionHelper.getPrivateValue(PlayerEntity.class, player, "portalCooldown");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientPlayerEntity player = Minecraft.getInstance().player;
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
            ObfuscationReflectionHelper.setPrivateValue(PlayerEntity.class, player, progress, "portalTime");
            ObfuscationReflectionHelper.setPrivateValue(PlayerEntity.class, player, prev, "oPortalTime");
        } else {
            chargeTicks = 0;
            float current = ObfuscationReflectionHelper.getPrivateValue(PlayerEntity.class, player, "portalTime");
            if (current > 0.0F) {
                ObfuscationReflectionHelper.setPrivateValue(PlayerEntity.class, player, current, "oPortalTime");
                ObfuscationReflectionHelper.setPrivateValue(PlayerEntity.class, player, Math.max(0.0F, current - 0.05F), "portalTime");
            }
        }

        touchedThisTick = false;
    }
}
