package io.github.urntt.litematicacreator.export;

import java.nio.file.Path;
import javax.annotation.Nullable;

public record CreatorExportWriteResult(boolean success, Path target, @Nullable Throwable error)
{
    static CreatorExportWriteResult success(Path target)
    {
        return new CreatorExportWriteResult(true, target, null);
    }

    static CreatorExportWriteResult failure(Path target, Throwable error)
    {
        return new CreatorExportWriteResult(false, target, error);
    }
}
