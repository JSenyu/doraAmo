package com.doraamo;

import com.doraamo.block.ModBlocks;
import com.doraamo.config.DimensionConfig;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.item.ModItems;
import com.doraamo.network.PacketHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Mod(DoraAmo.MODID)
public class DoraAmo {

    public static final String MODID = "doraamo";
    public static final String NAME = "DoraAmo";
    public static final String VERSION = "1.0.0";

    /** Sentinel: portal has no destination. */
    public static final String BLANK_DIMENSION = "";

    /** Portal charge time in ticks (same for survival and creative). */
    public static final int PORTAL_CHARGE_TICKS = 80;

    public static Logger logger;

    public DoraAmo() {
        logger = LogManager.getLogger();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);

        Path configRoot = FMLPaths.CONFIGDIR.get();
        File modConfigDir = configRoot.resolve(MODID).toFile();
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }
        DimensionConfig.init(configRoot.resolve(MODID + ".cfg").toFile());
        DisplayCatalog.init(new File(modConfigDir, "catalog"));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::init);
    }

    private void loadComplete(final FMLLoadCompleteEvent event) {
        DisplayCatalog.syncFromGame();
    }
}
