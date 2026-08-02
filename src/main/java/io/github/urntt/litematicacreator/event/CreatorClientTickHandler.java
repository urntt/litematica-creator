package io.github.urntt.litematicacreator.event;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.creator.CreatorCameraCompat;
import io.github.urntt.litematicacreator.creator.CreatorEditGestureController;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public class CreatorClientTickHandler implements IClientTickHandler
{
    public static final CreatorClientTickHandler INSTANCE = new CreatorClientTickHandler();

    private static long clientTicks;

    private boolean changedFreeCameraPlayerInputs;

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

        this.updateTweakerooFreeCameraCompatibility(mc);
        CreatorEditGestureController.INSTANCE.onClientTick(mc, clientTicks);
        CreatorRecoveryManager.getInstance().onClientTick(mc, clientTicks);
    }

    public void updateTweakerooFreeCameraCompatibility(Minecraft mc)
    {
        boolean inWorld = mc.level != null && mc.player != null;
        boolean creatorMode = CreatorManager.getInstance().isCreatorModeEnabled();
        boolean freeCameraActive = CreatorCameraCompat.isTweakerooFreeCameraActive();
        boolean freeCameraPlayerInputs = CreatorCameraCompat.isTweakerooFreeCameraPlayerInputsEnabled();

        if (inWorld && creatorMode && freeCameraActive && freeCameraPlayerInputs)
        {
            CreatorCameraCompat.warnIfTweakerooFreeCameraPlayerInputsEnabled();

            if (CreatorCameraCompat.setTweakerooFreeCameraPlayerInputs(false))
            {
                this.changedFreeCameraPlayerInputs = true;
                LitematicaCreator.debugLog("Temporarily disabled Tweakeroo freeCameraPlayerInputs for Creator mode.");
            }
        }
        else if (this.changedFreeCameraPlayerInputs && (!inWorld || !creatorMode || !freeCameraActive))
        {
            if (CreatorCameraCompat.setTweakerooFreeCameraPlayerInputs(true))
            {
                LitematicaCreator.debugLog("Restored Tweakeroo freeCameraPlayerInputs after leaving Creator free camera editing.");
            }

            this.changedFreeCameraPlayerInputs = false;
        }
    }

    public void resetCompatibilityState()
    {
        if (this.changedFreeCameraPlayerInputs)
        {
            CreatorCameraCompat.setTweakerooFreeCameraPlayerInputs(true);
            this.changedFreeCameraPlayerInputs = false;
        }
    }
}
