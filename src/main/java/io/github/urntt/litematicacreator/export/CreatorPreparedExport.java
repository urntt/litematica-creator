package io.github.urntt.litematicacreator.export;

import java.nio.file.Path;

import net.minecraft.nbt.CompoundTag;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import io.github.urntt.litematicacreator.config.CreatorExportRegionMode;

public record CreatorPreparedExport(
        LitematicaSchematic source,
        Path target,
        CreatorExportOperation operation,
        CreatorExportRegionMode mode,
        CompoundTag sourceNbt,
        CompoundTag outputNbt,
        CreatorExportPreview preview)
{
}
