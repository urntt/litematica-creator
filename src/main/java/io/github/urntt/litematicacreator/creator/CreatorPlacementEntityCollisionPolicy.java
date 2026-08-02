package io.github.urntt.litematicacreator.creator;

final class CreatorPlacementEntityCollisionPolicy
{
    private CreatorPlacementEntityCollisionPolicy()
    {
    }

    static boolean shouldCheck(boolean creatorCameraActive, boolean ignoreEntityCollision)
    {
        return creatorCameraActive && !ignoreEntityCollision;
    }

    static boolean canPlace(boolean realWorldUnobstructed, boolean creatorCameraIntersects)
    {
        return realWorldUnobstructed && !creatorCameraIntersects;
    }
}
