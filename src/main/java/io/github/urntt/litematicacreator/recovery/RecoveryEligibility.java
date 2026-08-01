package io.github.urntt.litematicacreator.recovery;

import java.nio.file.Path;
import javax.annotation.Nullable;

public final class RecoveryEligibility
{
    private RecoveryEligibility()
    {
    }

    public static boolean shouldCache(@Nullable Path schematicFile, boolean modifiedSinceSaved)
    {
        return schematicFile == null || modifiedSinceSaved;
    }
}
