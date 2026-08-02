package io.github.urntt.litematicacreator.camera;

public final class CreatorCameraSessionPolicy
{
    private CreatorCameraSessionPolicy()
    {
    }

    static boolean startsFlying(boolean sourceIsPlayer)
    {
        return !sourceIsPlayer;
    }

    static boolean mustStopForPlayer(boolean samePlayerInstance, boolean playerAlive)
    {
        return !samePlayerInstance || !playerAlive;
    }

    public static boolean shouldActivateForCreatorMode(boolean creatorModeEnabled, boolean configured, boolean cameraActive)
    {
        return creatorModeEnabled && configured && !cameraActive;
    }

    public static boolean shouldDeactivateForCreatorMode(boolean creatorModeEnabled, boolean configured, boolean cameraActive)
    {
        return !creatorModeEnabled && configured && cameraActive;
    }
}
