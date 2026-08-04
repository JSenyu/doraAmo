package com.doraamo;

import com.doraamo.block.ModBlocks;
import com.doraamo.config.DimensionConfig;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.item.ModItems;
import com.doraamo.network.ModPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
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

    public DoraAmo(IEventBus modBus, ModContainer modContainer) {
        logger = LogManager.getLogger();
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
        event.enqueueWork(ModPayloads::init); // handlers register via RegisterPayloadHandlersEvent
    }

    private void loadComplete(final FMLLoadCompleteEvent event) {
        DisplayCatalog.syncFromGame();
    }
}
