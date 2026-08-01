package io.github.urntt.litematicacreator.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class RecoveryManifestTest
{
    @Test
    void roundTripsVersionOneManifestAndPlacementData()
    {
        UUID entryId = UUID.randomUUID();
        UUID placementHash = UUID.randomUUID();
        JsonObject placement = new JsonObject();
        placement.addProperty("hash_code", placementHash.toString());
        placement.add("placements", new JsonArray());
        RecoveryManifest original = new RecoveryManifest(
                RecoveryManifest.CURRENT_FORMAT_VERSION,
                entryId,
                new RecoveryWorldKey("Test World", "minecraft:overworld"),
                "draft",
                "C:\\schematics\\draft.litematic",
                "LITEMATICA_SCHEMATIC",
                true,
                1234L,
                42L,
                entryId + "-42.litematic",
                List.of(placement),
                placementHash,
                placementHash
        );

        RecoveryManifest restored = RecoveryManifest.fromJson(original.toJson()).orElseThrow();

        assertEquals(original, restored);
        assertEquals(0, restored.placements().getFirst().getAsJsonArray("placements").size());
    }

    @Test
    void matchesWorldAndDimensionTogether()
    {
        RecoveryManifest manifest = manifestFor(new RecoveryWorldKey("server.example", "minecraft:the_nether"));

        assertTrue(manifest.belongsTo(new RecoveryWorldKey("server.example", "minecraft:the_nether")));
        assertFalse(manifest.belongsTo(new RecoveryWorldKey("server.example", "minecraft:overworld")));
        assertFalse(manifest.belongsTo(new RecoveryWorldKey("other.example", "minecraft:the_nether")));
    }

    @Test
    void rejectsUnknownVersionAndEscapingCachePath()
    {
        JsonObject unknownVersion = manifestFor(new RecoveryWorldKey("world", "dimension")).toJson();
        unknownVersion.addProperty("format_version", 2);
        assertTrue(RecoveryManifest.fromJson(unknownVersion).isEmpty());

        JsonObject escapingPath = manifestFor(new RecoveryWorldKey("world", "dimension")).toJson();
        escapingPath.addProperty("cache_file", "../draft.litematic");
        assertTrue(RecoveryManifest.fromJson(escapingPath).isEmpty());
    }

    private static RecoveryManifest manifestFor(RecoveryWorldKey key)
    {
        UUID entryId = UUID.randomUUID();
        return new RecoveryManifest(
                RecoveryManifest.CURRENT_FORMAT_VERSION,
                entryId,
                key,
                "draft",
                null,
                "LITEMATICA_SCHEMATIC",
                false,
                1L,
                1L,
                entryId + "-1.litematic",
                List.of(),
                null,
                null
        );
    }
}
