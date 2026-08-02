package io.github.urntt.litematicacreator.camera;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.creator.CreatorCameraCompat;
import io.github.urntt.litematicacreator.creator.CreatorCameraCompat.TweakerooConfigSnapshot;
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
    private boolean originalCameraIsTweakeroo;
    private boolean tweakerooActiveLastTick;
    private boolean tweakerooActivatedDuringSession;
    @Nullable
    private TweakerooConfigSnapshot tweakerooSnapshot;

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
        boolean tweakerooActive = CreatorCameraCompat.isTweakerooFreeCameraActive();

        try
        {
            this.originalCamera = source;
            this.sessionPlayer = minecraft.player;
            this.originalSmartCull = minecraft.smartCull;
            this.originalCameraIsTweakeroo = tweakerooActive && !sourceIsPlayer;
            this.tweakerooActiveLastTick = tweakerooActive;
            this.tweakerooActivatedDuringSession = false;
            this.tweakerooSnapshot = CreatorCameraCompat.captureAndSuspendTweakeroo();
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

    public boolean toggle(Minecraft minecraft, boolean notify)
    {
        if (this.isActive())
        {
            this.deactivate(minecraft);

            if (notify)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.camera.disabled");
            }

            return true;
        }

        if (minecraft.level == null || minecraft.player == null)
        {
            return false;
        }

        boolean activated = this.activate(minecraft) && this.isActive();

        if (activated && notify)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.camera.enabled");
        }

        return activated;
    }

    public void onClientTick(Minecraft minecraft)
    {
        if (minecraft.level == null || minecraft.player == null)
        {
            this.deactivate(minecraft);
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

        this.reconcileTweakeroo(minecraft);

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

    public boolean shouldIsolatePlayer(LocalPlayer player, Minecraft minecraft)
    {
        return CreatorPlayerIsolationPolicy.shouldIsolate(
                this.camera != null,
                player == this.sessionPlayer,
                player == minecraft.player
        );
    }

    @Nullable
    public CreatorCameraEntity getCamera()
    {
        return this.camera;
    }

    @Nullable
    public LocalPlayer getSessionPlayer()
    {
        return this.sessionPlayer;
    }

    public void turnCamera(double yawChange, double pitchChange)
    {
        if (this.camera != null)
        {
            this.camera.turnCamera(yawChange, pitchChange);
        }
    }

    private void disableCreatorModeAfterPlayerReplacement()
    {
        if (!this.changingCreatorMode)
        {
            this.changingCreatorMode = true;

            try
            {
                CreatorManager.getInstance().setCreatorModeEnabled(false, false);
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.camera.player_replaced");
            }
            finally
            {
                this.changingCreatorMode = false;
            }
        }
    }

    private void restoreState(Minecraft minecraft)
    {
        CreatorCameraEntity oldCamera = this.camera;
        Entity restoreCamera = this.getValidRestoreCamera(minecraft);

        if (this.tweakerooActivatedDuringSession &&
            CreatorCameraCompat.isTweakerooFreeCameraActive() &&
            minecraft.player != null)
        {
            CreatorCameraCompat.rebaseTweakerooOriginalCamera(minecraft.player);
        }

        if (restoreCamera != null)
        {
            minecraft.setCameraEntity(restoreCamera);
        }

        minecraft.smartCull = this.originalSmartCull;
        CreatorCameraCompat.restoreTweakeroo(this.tweakerooSnapshot);

        if (oldCamera != null && restoreCamera != null)
        {
            CreatorCameraChunkRefresh.markTransition(
                    minecraft,
                    restoreCamera.chunkPosition().x(),
                    restoreCamera.chunkPosition().z(),
                    oldCamera.chunkPosition().x(),
                    oldCamera.chunkPosition().z()
            );
        }

        this.camera = null;
        this.originalCamera = null;
        this.sessionPlayer = null;
        this.originalCameraIsTweakeroo = false;
        this.tweakerooActiveLastTick = false;
        this.tweakerooActivatedDuringSession = false;
        this.tweakerooSnapshot = null;
    }

    @Nullable
    private Entity getValidRestoreCamera(Minecraft minecraft)
    {
        boolean tweakerooActive = CreatorCameraCompat.isTweakerooFreeCameraActive();

        if (this.originalCamera != null &&
            !this.originalCamera.isRemoved() &&
            this.originalCamera.level() == minecraft.level &&
            CreatorExternalCameraPolicy.canRestoreCapturedCamera(this.originalCameraIsTweakeroo, tweakerooActive) &&
            (this.originalCamera != this.sessionPlayer || this.sessionPlayer == minecraft.player))
        {
            return this.originalCamera;
        }

        return minecraft.player;
    }

    private void reconcileTweakeroo(Minecraft minecraft)
    {
        boolean activeNow = CreatorCameraCompat.isTweakerooFreeCameraActive();
        Entity currentCamera = minecraft.getCameraEntity();
        boolean externalCameraPresent = currentCamera != null && currentCamera != this.camera;

        if (CreatorExternalCameraPolicy.shouldCaptureNewTweakerooCamera(
                this.tweakerooActiveLastTick,
                activeNow,
                externalCameraPresent
        ))
        {
            this.originalCamera = currentCamera;
            this.originalCameraIsTweakeroo = currentCamera != minecraft.player;
            this.tweakerooActivatedDuringSession = true;

            if (this.tweakerooSnapshot == null)
            {
                this.tweakerooSnapshot = CreatorCameraCompat.captureAndSuspendTweakeroo();
            }
        }

        if (activeNow)
        {
            CreatorCameraCompat.enforceTweakerooCreatorState();
        }

        this.tweakerooActiveLastTick = activeNow;
    }
}
