package io.github.urntt.litematicacreator.compat.litematica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.util.data.tag.CompoundData;

class CreatorLitematicaDataAdapterTest
{
    @Test
    void detectsTheEntityInfoDataModel()
            throws ReflectiveOperationException
    {
        Class<?> runtimeType = LitematicaSchematic.EntityInfo.class.getMethod("nbt").getReturnType();
        CreatorLitematicaDataAdapter.DataModel expected = runtimeType == CompoundData.class
                ? CreatorLitematicaDataAdapter.DataModel.COMPOUND_DATA
                : CreatorLitematicaDataAdapter.DataModel.COMPOUND_TAG;

        assertEquals(expected, CreatorLitematicaDataAdapter.dataModel());
    }

    @Test
    void compoundConversionsAreLosslessCopies()
    {
        CompoundTag source = new CompoundTag();
        source.putString("marker", "original");
        CompoundTag nested = new CompoundTag();
        nested.putIntArray("values", new int[] { 1, 2, 3 });
        source.put("nested", nested);
        CompoundTag expected = source.copy();
        Object runtime = CreatorLitematicaDataAdapter.copyToRuntime(source);

        source.putString("marker", "changed");
        CompoundTag first = CreatorLitematicaDataAdapter.copyToVanilla(runtime);
        assertEquals(expected, first);

        first.putString("marker", "changed-again");
        assertEquals("original", CreatorLitematicaDataAdapter.copyToVanilla(runtime).getStringOr("marker", ""));
    }

    @Test
    void blockEntityMapsUseTheDetectedRuntimeTypeWithoutSharingTags()
    {
        BlockPos position = new BlockPos(2, 3, 4);
        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString("id", "minecraft:chest");
        Map<BlockPos, CompoundTag> source = Map.of(position, blockEntity);

        Map<BlockPos, Object> runtime = CreatorLitematicaDataAdapter.restoreBlockEntities(source);
        Object runtimeValue = runtime.get(position);

        if (CreatorLitematicaDataAdapter.dataModel() == CreatorLitematicaDataAdapter.DataModel.COMPOUND_DATA)
        {
            assertInstanceOf(CompoundData.class, runtimeValue);
        }
        else
        {
            assertInstanceOf(CompoundTag.class, runtimeValue);
        }

        blockEntity.putString("id", "minecraft:furnace");
        Map<BlockPos, CompoundTag> restored = CreatorLitematicaDataAdapter.snapshotBlockEntities(runtime);
        assertEquals("minecraft:chest", restored.get(position).getStringOr("id", ""));
        assertNotSame(blockEntity, restored.get(position));
    }

    @Test
    void entitySnapshotsRoundTripWithoutMutatingTheInput()
    {
        Vec3 position = new Vec3(4.75, 8.25, -2.5);
        CompoundTag source = new CompoundTag();
        source.putInt("SleepingX", 99);
        source.putString("id", "minecraft:armor_stand");

        LitematicaSchematic.EntityInfo entity = CreatorLitematicaDataAdapter.createEntity(position, source);
        CompoundTag restored = CreatorLitematicaDataAdapter.snapshotEntityNbt(entity);

        assertEquals(position, entity.posVec());
        assertEquals(99, source.getIntOr("SleepingX", -1));
        assertEquals(4, restored.getIntOr("SleepingX", -1));
        assertEquals("minecraft:armor_stand", restored.getStringOr("id", ""));
    }

    @Test
    void schematicConstructorAndWriterMatchTheDetectedDataModel()
            throws ReflectiveOperationException
    {
        Class<?> runtimeType = CreatorLitematicaDataAdapter.dataModel() ==
                               CreatorLitematicaDataAdapter.DataModel.COMPOUND_DATA
                ? CompoundData.class
                : CompoundTag.class;
        String writerName = runtimeType == CompoundData.class ? "writeToData" : "writeToNBT";

        assertEquals(
                runtimeType,
                LitematicaSchematic.class.getMethod(writerName).getReturnType()
        );
        assertEquals(
                LitematicaSchematic.class,
                LitematicaSchematic.class
                        .getConstructor(Path.class, runtimeType, FileType.class)
                        .getDeclaringClass()
        );
    }

    @Test
    void detectedLitematicaContainsItsExpectedWriteCall()
            throws IOException
    {
        String classConstants = readClassConstants("/fi/dy/masa/litematica/schematic/LitematicaSchematic.class");

        if (CreatorLitematicaDataAdapter.dataModel() == CreatorLitematicaDataAdapter.DataModel.COMPOUND_DATA)
        {
            assertTrue(classConstants.contains("writeCompoundDataToCompressedNbtFile"));
        }
        else
        {
            assertTrue(classConstants.contains("writeCompoundTagToCompressedFile"));
        }
    }

    @Test
    void creatorWriteMixinContainsBothSupportedWriteHooks()
            throws IOException
    {
        String classConstants = readClassConstants(
                "/io/github/urntt/litematicacreator/mixin/LitematicaSchematicWriteMixin.class"
        );

        assertTrue(classConstants.contains("writeCompoundTagToCompressedFile"));
        assertTrue(classConstants.contains("writeCompoundDataToCompressedNbtFile"));
    }

    private static String readClassConstants(String resource)
            throws IOException
    {
        try (InputStream stream = CreatorLitematicaDataAdapterTest.class.getResourceAsStream(resource))
        {
            if (stream == null)
            {
                throw new IOException("Missing test classpath resource " + resource);
            }

            return new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
