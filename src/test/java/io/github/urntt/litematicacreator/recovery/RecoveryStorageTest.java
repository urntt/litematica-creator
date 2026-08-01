package io.github.urntt.litematicacreator.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecoveryStorageTest
{
    @TempDir
    Path tempDir;

    @Test
    void commitsAndReplacesGenerationsAtomically() throws Exception
    {
        RecoveryStorage storage = new RecoveryStorage(this.tempDir.resolve("recovery"));
        UUID entryId = UUID.randomUUID();
        storage.commit(snapshot(entryId, 1L, "first"));

        Path firstGeneration = storage.generationPath(entryId, 1L);
        assertTrue(Files.isRegularFile(firstGeneration));
        assertTrue(
                storage.readManifest(storage.manifestPath(entryId)).isPresent(),
                Files.readString(storage.manifestPath(entryId))
        );
        assertTrue(NbtUtils.readNbtFromFile(firstGeneration) != null);
        assertTrue(storage.scan().invalidManifests().isEmpty(), storage.scan().invalidManifests().toString());
        assertEquals(1L, storage.scan().entries().getFirst().manifest().generation());

        storage.commit(snapshot(entryId, 2L, "second"));

        RecoveryStorage.StoredEntry stored = storage.scan().entries().getFirst();
        assertEquals(2L, stored.manifest().generation());
        assertEquals("second", stored.schematicNbt().getStringOr("marker", ""));
        assertFalse(Files.exists(firstGeneration));
        assertTrue(Files.isRegularFile(storage.generationPath(entryId, 2L)));
    }

    @Test
    void failedManifestCommitLeavesPreviousGenerationUsable() throws Exception
    {
        Path root = this.tempDir.resolve("recovery");
        UUID entryId = UUID.randomUUID();
        RecoveryStorage storage = new RecoveryStorage(root);
        storage.commit(snapshot(entryId, 1L, "stable"));

        RecoveryStorage failingStorage = new RecoveryStorage(root, (generation, manifest) -> {
            throw new IOException("simulated interruption");
        });

        assertThrows(IOException.class, () -> failingStorage.commit(snapshot(entryId, 2L, "interrupted")));

        RecoveryStorage.StoredEntry stored = storage.scan().entries().getFirst();
        assertEquals(1L, stored.manifest().generation());
        assertEquals("stable", stored.schematicNbt().getStringOr("marker", ""));
        assertTrue(Files.isRegularFile(storage.generationPath(entryId, 1L)));
    }

    @Test
    void deletingKnownEntryRemovesManifestGenerationsAndTemps() throws Exception
    {
        RecoveryStorage storage = new RecoveryStorage(this.tempDir.resolve("recovery"));
        UUID entryId = UUID.randomUUID();
        storage.commit(snapshot(entryId, 1L, "value"));
        Files.writeString(storage.getRoot().resolve(entryId + "-99.litematic.tmp"), "partial");

        storage.deleteEntry(entryId);

        assertTrue(storage.scan().entries().isEmpty());
        try (var files = Files.list(storage.getRoot()))
        {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(entryId.toString())));
        }
    }

    private static RecoverySnapshot snapshot(UUID entryId, long generation, String marker)
    {
        JsonObject placement = new JsonObject();
        placement.addProperty("hash_code", UUID.randomUUID().toString());
        RecoveryManifest manifest = new RecoveryManifest(
                RecoveryManifest.CURRENT_FORMAT_VERSION,
                entryId,
                new RecoveryWorldKey("world", "minecraft:overworld"),
                "draft",
                null,
                "LITEMATICA_SCHEMATIC",
                false,
                generation,
                generation,
                entryId + "-" + generation + ".litematic",
                List.of(placement),
                null,
                null
        );
        CompoundTag nbt = new CompoundTag();
        nbt.putString("marker", marker);
        return new RecoverySnapshot(manifest, nbt);
    }
}
