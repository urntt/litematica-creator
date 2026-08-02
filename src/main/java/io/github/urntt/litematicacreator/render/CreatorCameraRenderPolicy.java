package io.github.urntt.litematicacreator.render;

final class CreatorCameraRenderPolicy
{
    private CreatorCameraRenderPolicy()
    {
    }

    static boolean shouldAppendRealPlayer(boolean cameraActive, boolean sameLevel, boolean visible, boolean alreadyPresent)
    {
        return cameraActive && sameLevel && visible && !alreadyPresent;
    }

    static boolean shouldAppendCameraAvatar(boolean cameraActive, boolean sameLevel, boolean alreadyPresent)
    {
        return cameraActive && sameLevel && !alreadyPresent;
    }
}
