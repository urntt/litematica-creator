package io.github.urntt.litematicacreator.export;

public enum CreatorExportOperation
{
    SAVE,
    SAVE_AS_AND_BIND,
    EXPORT_COPY;

    public boolean bindsFile()
    {
        return this != EXPORT_COPY;
    }
}
