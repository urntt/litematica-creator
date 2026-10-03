package io.github.urntt.litematicacreator.compat.litematica;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;

/**
 * Converts between Creator's vanilla {@link CompoundTag} snapshots and Litematica's MaLiLib {@link CompoundData} model.
 * Every conversion returns an independent copy, so snapshots never share mutable data with a live schematic.
 */
public final class CreatorLitematicaDataAdapter
{
    private CreatorLitematicaDataAdapter()
    {
    }

    public static CompoundTag copyToVanilla(CompoundData data)
    {
        return DataConverterNbt.toVanillaCompound(data.copy());
    }

    public static CompoundData copyToRuntime(CompoundTag tag)
    {
        return DataConverterNbt.fromVanillaCompound(tag.copy());
    }

    public static Map<BlockPos, CompoundTag> snapshotBlockEntities(Map<BlockPos, CompoundData> source)
    {
        Map<BlockPos, CompoundTag> copy = new HashMap<>();
        source.forEach((pos, data) -> copy.put(pos, copyToVanilla(data)));
        return copy;
    }

    public static Map<BlockPos, CompoundData> restoreBlockEntities(Map<BlockPos, CompoundTag> source)
    {
        Map<BlockPos, CompoundData> copy = new HashMap<>();
        source.forEach((pos, tag) -> copy.put(pos, copyToRuntime(tag)));
        return copy;
    }

    public static CompoundTag snapshotEntityNbt(LitematicaSchematic.EntityInfo entity)
    {
        return copyToVanilla(entity.nbt());
    }

    public static LitematicaSchematic.EntityInfo createEntity(Vec3 position, CompoundTag nbt)
    {
        return new LitematicaSchematic.EntityInfo(position, copyToRuntime(nbt));
    }

    public static CompoundTag writeSchematicToNbt(LitematicaSchematic schematic)
    {
        return copyToVanilla(schematic.writeToData());
    }

    public static LitematicaSchematic readSchematic(Path file, CompoundTag nbt, FileType type)
    {
        return new LitematicaSchematic(file, copyToRuntime(nbt), type);
    }
}
