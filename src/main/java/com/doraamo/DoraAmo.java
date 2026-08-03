package com.doraamo;

import com.doraamo.config.DimensionConfig;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = DoraAmo.MODID, name = DoraAmo.NAME, version = DoraAmo.VERSION)
public class DoraAmo {

    public static final String MODID = "doraamo";
    public static final String NAME = "DoraAmo";
    public static final String VERSION = "1.0.0";

    /** Sentinel: portal has no destination. Nether dim id is -1, so do not reuse -1. */
    public static final int BLANK_DIMENSION = Integer.MIN_VALUE;

    /** Portal charge time in ticks (same for survival and creative). */
    public static final int PORTAL_CHARGE_TICKS = 80;

    @Mod.Instance(MODID)
    public static DoraAmo instance;

    @SidedProxy(clientSide = "com.doraamo.proxy.ClientProxy", serverSide = "com.doraamo.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        File modConfigDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }
        DimensionConfig.init(event.getSuggestedConfigurationFile());
        DisplayCatalog.init(new File(modConfigDir, "catalog"));
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        DisplayCatalog.syncFromGame();
        proxy.postInit();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        DisplayCatalog.syncFromGame();
    }
}
