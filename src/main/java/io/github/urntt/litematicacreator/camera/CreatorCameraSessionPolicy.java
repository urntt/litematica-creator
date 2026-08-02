package io.github.urntt.litematicacreator.camera;

final class CreatorCameraSessionPolicy
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
}
