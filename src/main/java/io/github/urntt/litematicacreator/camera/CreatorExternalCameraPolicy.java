package io.github.urntt.litematicacreator.camera;

final class CreatorExternalCameraPolicy
{
    private CreatorExternalCameraPolicy()
    {
    }

    static boolean canRestoreCapturedCamera(boolean capturedCameraIsTweakeroo, boolean tweakerooActive)
    {
        return !capturedCameraIsTweakeroo || tweakerooActive;
    }

    static boolean shouldCaptureNewTweakerooCamera(boolean activeBefore, boolean activeNow, boolean externalCameraPresent)
    {
        return !activeBefore && activeNow && externalCameraPresent;
    }
}
