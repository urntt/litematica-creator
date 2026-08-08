package io.github.urntt.litematicacreator.mixin;

import java.nio.file.Path;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
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
            require = 2
    )
    private boolean litematicacreator$normalizeCreatorDraftExport(CompoundTag nbt, Path file)
    {
        CompoundTag normalized = CreatorSchematicExportNormalizer.normalizeForExport(nbt, file.getFileName().toString());
        return NbtUtils.writeCompoundTagToCompressedFile(normalized, file);
    }
}
