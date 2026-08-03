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
}
