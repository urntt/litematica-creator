package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fi.dy.masa.malilib.util.nbt.NbtUtils;

class CreatorExportStorageTest
{
    @TempDir
    Path temporaryDirectory;

    @Test
    void appendsTheLitematicExtensionOnlyWhenNeeded()
    {
        assertEquals(Path.of("draft.litematic"), CreatorSchematicExportService.ensureExtension(Path.of("draft")));
        assertEquals(Path.of("draft.litematic"), CreatorSchematicExportService.ensureExtension(Path.of("draft.litematic")));
        assertEquals(Path.of("DRAFT.LITEMATIC"), CreatorSchematicExportService.ensureExtension(Path.of("DRAFT.LITEMATIC")));
    }

    @Test
    void atomicallyReplacesTheTargetAndRemovesTheTemporaryFile()
    {
        Path target = this.temporaryDirectory.resolve("test.litematic");
        CompoundTag first = new CompoundTag();
        first.putInt("generation", 1);
        CompoundTag second = new CompoundTag();
        second.putInt("generation", 2);

        assertTrue(CreatorSchematicExportService.writeAtomically(target, first).success());
        assertEquals(1, NbtUtils.readNbtFromFile(target).getIntOr("generation", -1));
        assertTrue(CreatorSchematicExportService.writeAtomically(target, second).success());
        assertEquals(2, NbtUtils.readNbtFromFile(target).getIntOr("generation", -1));
        assertFalse(Files.exists(target.resolveSibling("test.litematic.tmp")));
    }

    @Test
    void onlySaveOperationsBindTheCurrentSchematic()
    {
        assertTrue(CreatorExportOperation.SAVE.bindsFile());
        assertTrue(CreatorExportOperation.SAVE_AS_AND_BIND.bindsFile());
        assertFalse(CreatorExportOperation.EXPORT_COPY.bindsFile());
    }
}
