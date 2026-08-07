package io.github.urntt.litematicacreator.camera;

final class CreatorCameraFlightPolicy
{
    private CreatorCameraFlightPolicy()
    {
    }

    static boolean shouldStartFallFlying(
            boolean jumpDown,
            boolean jumpWasDown,
            boolean wasOnGround,
            boolean creativeFlying,
            boolean fallFlying,
            boolean canGlide,
            boolean inWater)
    {
        return jumpDown &&
                !jumpWasDown &&
                !wasOnGround &&
                !creativeFlying &&
                !fallFlying &&
                canGlide &&
                !inWater;
    }

    static boolean shouldStopFallFlying(boolean creativeFlying, boolean fallFlying, boolean canGlide)
    {
        return fallFlying && (creativeFlying || !canGlide);
    }
}
