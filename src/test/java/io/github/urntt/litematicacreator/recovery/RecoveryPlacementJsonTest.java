package io.github.urntt.litematicacreator.recovery;

import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoveryPlacementJsonTest
{
    @Test
    void rebindsAPlacementCopiedFromAnotherGameDirectory()
    {
        JsonObject stored = placement("C:\\Minecraft\\old-instance\\recovery\\entry-1.litematic");
        Path currentCache = Path.of("C:\\Minecraft\\new-instance\\recovery\\entry-1.litematic");

        JsonObject restored = RecoveryPlacementJson.prepareForRestore(stored, currentCache);

        assertEquals(currentCache.toAbsolutePath().normalize().toString(), restored.get("schematic").getAsString());
        assertEquals(
                "C:\\Minecraft\\old-instance\\recovery\\entry-1.litematic",
                stored.get("schematic").getAsString()
        );
    }

    @Test
    void retainsTheRequiredPlacementStructureWhileRebinding()
    {
        JsonObject restored = RecoveryPlacementJson.prepareForRestore(
                placement("C:\\outside\\untrusted.litematic"),
                Path.of("cache", "verified.litematic")
        );

        assertEquals(0, restored.getAsJsonArray("placements").size());
    }

    @Test
    void rejectsMissingOrMalformedPlacementFields()
    {
        JsonObject missingSchematic = new JsonObject();
        missingSchematic.add("placements", new JsonArray());
        JsonObject missingPlacements = new JsonObject();
        missingPlacements.addProperty("schematic", "old.litematic");
        JsonObject malformedPlacements = placement("old.litematic");
        malformedPlacements.addProperty("placements", "not-an-array");

        assertThrows(
                IllegalArgumentException.class,
                () -> RecoveryPlacementJson.prepareForRestore(missingSchematic, Path.of("cache.litematic"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RecoveryPlacementJson.prepareForRestore(missingPlacements, Path.of("cache.litematic"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RecoveryPlacementJson.prepareForRestore(malformedPlacements, Path.of("cache.litematic"))
        );
    }

    private static JsonObject placement(String schematicPath)
    {
        JsonObject placement = new JsonObject();
        placement.addProperty("schematic", schematicPath);
        placement.add("placements", new JsonArray());
        return placement;
    }
}
