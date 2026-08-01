package io.github.urntt.litematicacreator.creator;

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
}
