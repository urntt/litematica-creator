package io.github.urntt.litematicacreator.creator;

public final class CreatorTargetDecision
{
    private CreatorTargetDecision()
    {
    }

    public static Action decide(int candidateCount, boolean focusAvailable)
    {
        if (candidateCount > 1)
        {
            return Action.CHOOSE_OVERLAP;
        }

        if (candidateCount == 1)
        {
            return Action.EDIT_CANDIDATE;
        }

        return focusAvailable ? Action.EDIT_FOCUS : Action.CREATE_NEW;
    }

    public enum Action
    {
        EDIT_CANDIDATE,
        EDIT_FOCUS,
        CREATE_NEW,
        CHOOSE_OVERLAP
    }
}
