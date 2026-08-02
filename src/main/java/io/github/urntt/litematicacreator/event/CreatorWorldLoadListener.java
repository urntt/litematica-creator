package io.github.urntt.litematicacreator.event;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import io.github.urntt.litematicacreator.creator.CreatorEditService;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.creator.CreatorEditGestureController;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorPlacementIndex;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public class CreatorWorldLoadListener implements IWorldLoadListener
{
    public static final CreatorWorldLoadListener INSTANCE = new CreatorWorldLoadListener();

    private CreatorWorldLoadListener()
    {
    }

    @Override
    public void onWorldLoadPre(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
    {
        if (worldBefore != null)
        {
            CreatorCameraController.getInstance().deactivate(mc);
            CreatorRecoveryManager.getInstance().beforeWorldChange(worldBefore);

            if (worldAfter == null)
            {
                this.resetSessionState();
            }
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
    {
        if (worldAfter != null)
        {
            this.resetSessionState();
            CreatorRecoveryManager.getInstance().afterWorldJoin(worldAfter);
        }
        else
        {
            CreatorRecoveryManager.getInstance().afterWorldLeave();
        }

        CreatorPlacementIndex.INSTANCE.rebuild();
    }

    private void resetSessionState()
    {
        CreatorManager manager = CreatorManager.getInstance();
        manager.setCreatorModeEnabled(false, false);
        manager.clearFocusSilently();
        CreatorEditGestureController.INSTANCE.resetTransientState();
        CreatorEditService.getInstance().resetTransientState();
    }
}
