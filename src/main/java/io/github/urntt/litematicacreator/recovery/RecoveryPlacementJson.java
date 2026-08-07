package io.github.urntt.litematicacreator.recovery;

import java.nio.file.Path;
import java.util.Objects;

import com.google.gson.JsonObject;

final class RecoveryPlacementJson
{
    private RecoveryPlacementJson()
    {
    }

    static JsonObject prepareForRestore(JsonObject storedJson, Path verifiedCacheFile)
    {
        Objects.requireNonNull(storedJson, "storedJson");
        Objects.requireNonNull(verifiedCacheFile, "verifiedCacheFile");
        JsonObject json = storedJson.deepCopy();

        if (!json.has("schematic") || !json.get("schematic").isJsonPrimitive() ||
            json.get("schematic").getAsString().isBlank() ||
            !json.has("placements") || !json.get("placements").isJsonArray())
        {
            throw new IllegalArgumentException("Recovery placement has an invalid schematic path or subregion list");
        }

        json.addProperty("schematic", verifiedCacheFile.toAbsolutePath().normalize().toString());
        return json;
    }
}
