package io.github.urntt.litematicacreator.mixin;

import java.nio.file.Path;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataFileUtils;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.export.CreatorSchematicExportNormalizer;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LitematicaSchematic.class)
public abstract class LitematicaSchematicWriteMixin
{
    @Redirect(
            method = "writeToFile(Ljava/nio/file/Path;Ljava/lang/String;ZZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/nbt/NbtUtils;writeCompoundTagToCompressedFile(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)Z"
            ),
            require = 0
    )
    private boolean litematicacreator$normalizeLegacyCreatorDraftExport(CompoundTag nbt, Path file)
    {
        CompoundTag normalized = litematicacreator$normalizeOrOriginal(nbt, file);
        return NbtUtils.writeCompoundTagToCompressedFile(normalized, file);
    }

    @Redirect(
            method = "writeToFile(Ljava/nio/file/Path;Ljava/lang/String;ZZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/data/tag/util/DataFileUtils;writeCompoundDataToCompressedNbtFile(Ljava/nio/file/Path;Lfi/dy/masa/malilib/util/data/tag/BaseData;)Z"
            ),
            require = 0
    )
    private boolean litematicacreator$normalizeCurrentCreatorDraftExport(Path file, BaseData data)
    {
        if (!(data instanceof CompoundData compoundData))
        {
            LitematicaCreator.LOGGER.warn(
                    "Skipping Creator schematic metadata normalization for unexpected MaLiLib data type {}",
                    data != null ? data.getClass().getName() : "null"
            );
            return DataFileUtils.writeCompoundDataToCompressedNbtFile(file, data);
        }

        CompoundTag vanilla = DataConverterNbt.toVanillaCompound(compoundData.copy());
        CompoundTag normalized = litematicacreator$normalizeOrOriginal(vanilla, file);
        return DataFileUtils.writeCompoundDataToCompressedNbtFile(
                file,
                DataConverterNbt.fromVanillaCompound(normalized)
        );
    }

    private static CompoundTag litematicacreator$normalizeOrOriginal(CompoundTag nbt, Path file)
    {
        try
        {
            return CreatorSchematicExportNormalizer.normalizeForExport(nbt, file.getFileName().toString());
        }
        catch (Throwable error)
        {
            LitematicaCreator.LOGGER.warn(
                    "Failed to normalize Creator schematic metadata for '{}'; writing the original data",
                    file,
                    error
            );
            return nbt;
        }
    }
}
