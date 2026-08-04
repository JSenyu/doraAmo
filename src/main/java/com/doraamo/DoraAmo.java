package com.doraamo;

import com.doraamo.block.ModBlocks;
import com.doraamo.config.DimensionConfig;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.item.ModCreativeTabs;
import com.doraamo.item.ModItems;
import com.doraamo.network.PacketHandler;
import com.doraamo.portal.PortalChunkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class DoraAmo implements ModInitializer {

    public static final String MODID = "doraamo";
    public static final String NAME = "DoraAmo";
    public static final String VERSION = "1.0.0";

    /** Sentinel: portal has no destination. */
    public static final String BLANK_DIMENSION = "";

    /** Portal charge time in ticks (same for survival and creative). */
    public static final int PORTAL_CHARGE_TICKS = 80;

    public static Logger logger;

    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        logger = LogManager.getLogger();
        ModBlocks.register();
        ModItems.register();
        ModCreativeTabs.register();
        PacketHandler.initServer();
        PortalChunkHandler.register();

        Path configRoot = FabricLoader.getInstance().getConfigDir();
        File modConfigDir = configRoot.resolve(MODID).toFile();
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }
        DimensionConfig.init(configRoot.resolve(MODID + ".cfg").toFile());
        DisplayCatalog.init(new File(modConfigDir, "catalog"));

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            DisplayCatalog.syncFromGame();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
    }

    public static MinecraftServer getServer() {
        return server;
    }
}
