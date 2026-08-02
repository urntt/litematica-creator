package io.github.urntt.litematicacreator.camera;

final class CreatorPlayerIsolationPolicy
{
    private CreatorPlayerIsolationPolicy()
    {
    }

    static boolean shouldIsolate(boolean cameraActive, boolean sessionPlayer, boolean currentPlayer)
    {
        return cameraActive && sessionPlayer && currentPlayer;
    }
}
