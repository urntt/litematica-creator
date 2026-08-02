package io.github.urntt.litematicacreator.creator;

import java.util.List;
import java.util.function.Function;

final class CreatorEditBatchExecutor
{
    private CreatorEditBatchExecutor()
    {
    }

    static <T> boolean execute(
            List<T> targets,
            Function<T, CreatorEditOutcome> attempt,
            Runnable onOverlap)
    {
        for (T target : targets)
        {
            if (attempt.apply(target) == CreatorEditOutcome.OVERLAP)
            {
                onOverlap.run();
                return true;
            }
        }

        return false;
    }
}
