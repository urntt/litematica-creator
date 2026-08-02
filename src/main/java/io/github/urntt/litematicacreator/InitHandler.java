package io.github.urntt.litematicacreator;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.event.WorldLoadHandler;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorPlacementIndex;
import io.github.urntt.litematicacreator.event.CreatorClientTickHandler;
import io.github.urntt.litematicacreator.event.CreatorHotkeyCallbacks;
import io.github.urntt.litematicacreator.event.CreatorWorldLoadListener;
import io.github.urntt.litematicacreator.event.InputHandler;
import io.github.urntt.litematicacreator.gui.GuiConfigs;
import io.github.urntt.litematicacreator.render.CreatorStatusHud;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new)
        );
        Configs.Generic.ENABLE_CREATOR_MODE.setValueChangeCallback(
                cfg -> CreatorManager.getInstance().setCreatorModeEnabled(cfg.getBooleanValue(), false)
        );

        CreatorHotkeyCallbacks.register();
        CreatorPlacementIndex.INSTANCE.register();
        CreatorRecoveryManager.getInstance().register();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(CreatorWorldLoadListener.INSTANCE);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(CreatorWorldLoadListener.INSTANCE);
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());
        TickHandler.getInstance().registerClientTickHandler(CreatorClientTickHandler.INSTANCE);
        InfoHud.getInstance().addInfoHudRenderer(CreatorStatusHud.INSTANCE, true);
    }
}
