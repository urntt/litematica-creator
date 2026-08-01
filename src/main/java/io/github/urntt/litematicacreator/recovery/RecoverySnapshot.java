package io.github.urntt.litematicacreator.recovery;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;

public record RecoverySnapshot(RecoveryManifest manifest, CompoundTag schematicNbt)
{
    public RecoverySnapshot
    {
        manifest = Objects.requireNonNull(manifest, "manifest");
        schematicNbt = Objects.requireNonNull(schematicNbt, "schematicNbt");
    }
}
