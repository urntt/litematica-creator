package io.github.urntt.litematicacreator.compat.litematica;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import io.github.urntt.litematicacreator.LitematicaCreator;

/**
 * Isolates the Litematica 0.28.2 and 0.28.5 schematic data-model boundary.
 */
public final class CreatorLitematicaDataAdapter
{
    private static final RuntimeAdapter RUNTIME = RuntimeAdapter.detect();

    private CreatorLitematicaDataAdapter()
    {
    }

    public static void initialize()
    {
        LitematicaCreator.LOGGER.info("Using Litematica {} schematic data compatibility", RUNTIME.model().description);
    }

    public static DataModel dataModel()
    {
        return RUNTIME.model();
    }

    public static CompoundTag copyToVanilla(Object data)
    {
        if (data instanceof CompoundTag tag)
        {
            return tag.copy();
        }

        if (data instanceof CompoundData compoundData)
        {
            return DataConverterNbt.toVanillaCompound(compoundData.copy());
        }

        throw new IllegalArgumentException("Unsupported Litematica compound data type: " + typeName(data));
    }

    public static Object copyToRuntime(CompoundTag tag)
    {
        return RUNTIME.model() == DataModel.COMPOUND_DATA
                ? DataConverterNbt.fromVanillaCompound(tag.copy())
                : tag.copy();
    }

    public static Map<BlockPos, CompoundTag> snapshotBlockEntities(Map<BlockPos, ?> source)
    {
        Map<BlockPos, CompoundTag> copy = new HashMap<>();
        source.forEach((pos, data) -> copy.put(pos, copyToVanilla(data)));
        return copy;
    }

    public static Map<BlockPos, Object> restoreBlockEntities(Map<BlockPos, CompoundTag> source)
    {
        Map<BlockPos, Object> copy = new HashMap<>();
        source.forEach((pos, tag) -> copy.put(pos, copyToRuntime(tag)));
        return copy;
    }

    public static CompoundTag snapshotEntityNbt(LitematicaSchematic.EntityInfo entity)
    {
        return copyToVanilla(invoke(RUNTIME.entityNbt(), entity));
    }

    public static LitematicaSchematic.EntityInfo createEntity(Vec3 position, CompoundTag nbt)
    {
        Object runtimeNbt = copyToRuntime(nbt);

        try
        {
            return RUNTIME.entityConstructor().newInstance(position, runtimeNbt);
        }
        catch (ReflectiveOperationException exception)
        {
            throw compatibilityFailure("create schematic entity", exception);
        }
    }

    public static CompoundTag writeSchematicToNbt(LitematicaSchematic schematic)
    {
        return copyToVanilla(invoke(RUNTIME.schematicWriter(), schematic));
    }

    public static LitematicaSchematic readSchematic(Path file, CompoundTag nbt, FileType type)
    {
        Object runtimeNbt = copyToRuntime(nbt);

        try
        {
            return RUNTIME.schematicConstructor().newInstance(file, runtimeNbt, type);
        }
        catch (ReflectiveOperationException exception)
        {
            throw compatibilityFailure("read schematic data", exception);
        }
    }

    private static Object invoke(Method method, Object target)
    {
        try
        {
            return method.invoke(target);
        }
        catch (IllegalAccessException | InvocationTargetException exception)
        {
            throw compatibilityFailure("invoke " + method.getName(), exception);
        }
    }

    private static IllegalStateException compatibilityFailure(String operation, ReflectiveOperationException exception)
    {
        Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : exception;
        return new IllegalStateException("Failed to " + operation + " using the detected Litematica " +
                                         RUNTIME.model().description + " data model", cause);
    }

    private static String typeName(Object value)
    {
        return value != null ? value.getClass().getName() : "null";
    }

    public enum DataModel
    {
        COMPOUND_TAG("CompoundTag"),
        COMPOUND_DATA("CompoundData");

        private final String description;

        DataModel(String description)
        {
            this.description = description;
        }
    }

    private record RuntimeAdapter(
            DataModel model,
            Method entityNbt,
            Constructor<LitematicaSchematic.EntityInfo> entityConstructor,
            Method schematicWriter,
            Constructor<LitematicaSchematic> schematicConstructor)
    {
        static RuntimeAdapter detect()
        {
            try
            {
                Method entityNbt = LitematicaSchematic.EntityInfo.class.getMethod("nbt");
                Class<?> runtimeCompoundType = entityNbt.getReturnType();
                DataModel model;
                String writerName;

                if (runtimeCompoundType == CompoundTag.class)
                {
                    model = DataModel.COMPOUND_TAG;
                    writerName = "writeToNBT";
                }
                else if (runtimeCompoundType == CompoundData.class)
                {
                    model = DataModel.COMPOUND_DATA;
                    writerName = "writeToData";
                }
                else
                {
                    throw new NoSuchMethodException("Unsupported EntityInfo.nbt return type " + runtimeCompoundType.getName());
                }

                Constructor<LitematicaSchematic.EntityInfo> entityConstructor =
                        LitematicaSchematic.EntityInfo.class.getConstructor(Vec3.class, runtimeCompoundType);
                Method schematicWriter = LitematicaSchematic.class.getMethod(writerName);
                Constructor<LitematicaSchematic> schematicConstructor =
                        LitematicaSchematic.class.getConstructor(Path.class, runtimeCompoundType, FileType.class);

                if (schematicWriter.getReturnType() != runtimeCompoundType)
                {
                    throw new NoSuchMethodException(writerName + " returns " + schematicWriter.getReturnType().getName() +
                                                    " instead of " + runtimeCompoundType.getName());
                }

                return new RuntimeAdapter(model, entityNbt, entityConstructor, schematicWriter, schematicConstructor);
            }
            catch (ReflectiveOperationException exception)
            {
                throw new IllegalStateException(
                        "Unsupported Litematica schematic data model; expected 0.28.2-style CompoundTag or " +
                        "0.28.5-style CompoundData APIs",
                        exception
                );
            }
        }
    }
}
