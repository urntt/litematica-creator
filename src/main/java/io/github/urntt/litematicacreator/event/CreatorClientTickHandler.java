package io.github.urntt.litematicacreator.event;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.creator.CreatorEditGestureController;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.export.CreatorSchematicExportService;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public class CreatorClientTickHandler implements IClientTickHandler
{
    public static final CreatorClientTickHandler INSTANCE = new CreatorClientTickHandler();

    private static long clientTicks;

    private CreatorClientTickHandler()
    {
    }

    public static long getClientTicks()
    {
        return clientTicks;
    }

    @Override
    public void onClientTick(Minecraft mc)
    {
        ++clientTicks;

        if (mc.level != null && mc.player != null)
        {
            CreatorInventory.getInstance().load(mc.level.registryAccess());
        }

        CreatorCameraController.getInstance().onClientTick(mc);
        CreatorEditGestureController.INSTANCE.onClientTick(mc, clientTicks);
        CreatorSchematicExportService.getInstance().onClientTick(mc);
        CreatorRecoveryManager.getInstance().onClientTick(mc, clientTicks);
    }

}
