package io.github.urntt.litematicacreator.camera;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.creator.CreatorManager;

public final class CreatorCameraController
{
    private static final CreatorCameraController INSTANCE = new CreatorCameraController();

    @Nullable
    private CreatorCameraEntity camera;
    @Nullable
    private Entity originalCamera;
    @Nullable
    private LocalPlayer sessionPlayer;
    private boolean originalSmartCull;
    private boolean changingCreatorMode;

    private CreatorCameraController()
    {
    }

    public static CreatorCameraController getInstance()
    {
        return INSTANCE;
    }

    public boolean activate(Minecraft minecraft)
    {
        if (this.camera != null)
        {
            return true;
        }

        if (minecraft.level == null || minecraft.player == null)
        {
            return true;
        }

        Entity source = minecraft.getCameraEntity();

        if (source == null || source.level() != minecraft.level)
        {
            source = minecraft.player;
        }

        boolean sourceIsPlayer = source == minecraft.player;

        try
        {
            this.originalCamera = source;
            this.sessionPlayer = minecraft.player;
            this.originalSmartCull = minecraft.smartCull;
            this.camera = new CreatorCameraEntity(
                    minecraft,
                    minecraft.level,
                    minecraft.player,
                    source,
                    CreatorCameraSessionPolicy.startsFlying(sourceIsPlayer)
            );
            minecraft.setCameraEntity(this.camera);
            minecraft.smartCull = false;
            LitematicaCreator.debugLog(
                    "Started Creator Camera at [{}, {}, {}], flying={}",
                    this.camera.getX(),
                    this.camera.getY(),
                    this.camera.getZ(),
                    this.camera.isCreatorFlying()
            );
            return true;
        }
        catch (RuntimeException | LinkageError error)
        {
            LitematicaCreator.LOGGER.error("Failed to initialize Creator Camera", error);
            this.restoreState(minecraft);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica-creator.message.camera.initialization_failed");
            return false;
        }
    }

    public void deactivate(Minecraft minecraft)
    {
        if (this.camera == null && this.originalCamera == null && this.sessionPlayer == null)
        {
            return;
        }

        CreatorCameraEntity oldCamera = this.camera;
        this.restoreState(minecraft);

        if (oldCamera != null)
        {
            LitematicaCreator.debugLog(
                    "Stopped Creator Camera at [{}, {}, {}]",
                    oldCamera.getX(),
                    oldCamera.getY(),
                    oldCamera.getZ()
            );
        }
    }

    public void onClientTick(Minecraft minecraft)
    {
        if (!CreatorManager.getInstance().isCreatorModeEnabled())
        {
            this.deactivate(minecraft);
            return;
        }

        if (minecraft.level == null || minecraft.player == null)
        {
            this.deactivate(minecraft);
            return;
        }

        if (!this.activate(minecraft))
        {
            this.disableCreatorModeAfterFailure();
            return;
        }

        if (this.camera == null || this.sessionPlayer == null)
        {
            return;
        }

        if (CreatorCameraSessionPolicy.mustStopForPlayer(
                minecraft.player == this.sessionPlayer,
                minecraft.player.isAlive()
        ))
        {
            this.deactivate(minecraft);
            this.disableCreatorModeAfterPlayerReplacement();
            return;
        }

        if (minecraft.getCameraEntity() != this.camera)
        {
            minecraft.setCameraEntity(this.camera);
        }

        this.camera.creatorTick();
    }

    public boolean acceptsInput()
    {
        return this.camera != null && !this.changingCreatorMode;
    }

    public boolean isActive()
    {
        return this.camera != null;
    }

    public boolean isFlying()
    {
        return this.camera != null && this.camera.isCreatorFlying();
    }

    public boolean isCamera(Entity entity)
    {
        return entity == this.camera;
    }

    @Nullable
    public CreatorCameraEntity getCamera()
    {
        return this.camera;
    }

    public void turnCamera(double yawChange, double pitchChange)
    {
        if (this.camera != null)
        {
            this.camera.turnCamera(yawChange, pitchChange);
        }
    }

    private void disableCreatorModeAfterFailure()
    {
        if (!this.changingCreatorMode)
        {
            this.changingCreatorMode = true;
            CreatorManager.getInstance().setCreatorModeEnabled(false, false);
            this.changingCreatorMode = false;
        }
    }

    private void disableCreatorModeAfterPlayerReplacement()
    {
        if (!this.changingCreatorMode)
        {
            this.changingCreatorMode = true;
            CreatorManager.getInstance().setCreatorModeEnabled(false, false);
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.camera.player_replaced");
            this.changingCreatorMode = false;
        }
    }

    private void restoreState(Minecraft minecraft)
    {
        Entity restoreCamera = this.getValidRestoreCamera(minecraft);

        if (restoreCamera != null)
        {
            minecraft.setCameraEntity(restoreCamera);
        }

        minecraft.smartCull = this.originalSmartCull;
        this.camera = null;
        this.originalCamera = null;
        this.sessionPlayer = null;
    }

    @Nullable
    private Entity getValidRestoreCamera(Minecraft minecraft)
    {
        if (this.originalCamera != null &&
            !this.originalCamera.isRemoved() &&
            this.originalCamera.level() == minecraft.level &&
            (this.originalCamera != this.sessionPlayer || this.sessionPlayer == minecraft.player))
        {
            return this.originalCamera;
        }

        return minecraft.player;
    }
}
