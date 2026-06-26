package io.github.urntt.litematicacreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

import fi.dy.masa.malilib.event.InitializationHandler;
import io.github.urntt.litematicacreator.config.Configs;

public class LitematicaCreator implements ModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    public static void debugLog(String message, Object... args)
    {
        if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
        {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }
}
