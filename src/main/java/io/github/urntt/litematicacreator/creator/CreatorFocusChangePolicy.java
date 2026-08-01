package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

final class CreatorFocusChangePolicy
{
    private CreatorFocusChangePolicy()
    {
    }

    static Decision decide(
            @Nullable Object previousPlacement,
            @Nullable Object nextPlacement,
            boolean notificationsEnabled)
    {
        boolean changed = previousPlacement != nextPlacement;
        return new Decision(changed, changed && notificationsEnabled);
    }

    record Decision(boolean changed, boolean notifyPlayer)
    {
    }
}
