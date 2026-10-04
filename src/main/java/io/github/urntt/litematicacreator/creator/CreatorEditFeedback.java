package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

final class CreatorEditFeedback
{
    private CreatorEditFeedback()
    {
    }

    static boolean afterSuccessfulEdit(boolean changed, Runnable feedback)
    {
        if (changed)
        {
            feedback.run();
        }

        return changed;
    }

    static <T> T feedbackTarget(T player, @Nullable T camera)
    {
        return camera != null ? camera : player;
    }

    // Like a vanilla attack click, an attack that reaches no projection still swings, but only on the camera stand-in;
    // without an edit the real player must not animate.
    static boolean swingsWithoutEdit(boolean projectionTargeted, boolean cameraActive)
    {
        return !projectionTargeted && cameraActive;
    }
}
