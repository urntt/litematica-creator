package io.github.urntt.litematicacreator.recovery;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public record RecoveryManifest(
        int formatVersion,
        UUID entryId,
        RecoveryWorldKey worldKey,
        String schematicName,
        @Nullable String originalFile,
        String originalFileType,
        boolean dirty,
        long cachedAtEpochMillis,
        long generation,
        String cacheFile,
        List<JsonObject> placements,
        @Nullable UUID selectedPlacementHash,
        @Nullable UUID creatorFocusHash)
{
    public static final int CURRENT_FORMAT_VERSION = 1;

    public RecoveryManifest
    {
        if (formatVersion != CURRENT_FORMAT_VERSION)
        {
            throw new IllegalArgumentException("Unsupported recovery manifest version: " + formatVersion);
        }

        entryId = Objects.requireNonNull(entryId, "entryId");
        worldKey = Objects.requireNonNull(worldKey, "worldKey");
        schematicName = Objects.requireNonNull(schematicName, "schematicName");
        originalFileType = requireNonBlank(originalFileType, "originalFileType");
        cacheFile = validateCacheFile(cacheFile);

        if (cachedAtEpochMillis < 0L || generation < 0L)
        {
            throw new IllegalArgumentException("Recovery timestamps must not be negative");
        }

        Objects.requireNonNull(placements, "placements");
        List<JsonObject> placementCopies = new ArrayList<>(placements.size());

        for (JsonObject placement : placements)
        {
            placementCopies.add(Objects.requireNonNull(placement, "placement").deepCopy());
        }

        placements = List.copyOf(placementCopies);
    }

    public JsonObject toJson()
    {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", this.formatVersion);
        root.addProperty("entry_id", this.entryId.toString());
        root.addProperty("world", this.worldKey.world());
        root.addProperty("dimension", this.worldKey.dimension());
        root.addProperty("schematic_name", this.schematicName);
        root.add("original_file", this.originalFile != null ? new com.google.gson.JsonPrimitive(this.originalFile) : JsonNull.INSTANCE);
        root.addProperty("original_file_type", this.originalFileType);
        root.addProperty("dirty", this.dirty);
        root.addProperty("cached_at", this.cachedAtEpochMillis);
        root.addProperty("generation", this.generation);
        root.addProperty("cache_file", this.cacheFile);

        JsonArray placementArray = new JsonArray();
        this.placements.forEach(placement -> placementArray.add(placement.deepCopy()));
        root.add("placements", placementArray);

        if (this.selectedPlacementHash != null)
        {
            root.addProperty("selected_placement_hash", this.selectedPlacementHash.toString());
        }

        if (this.creatorFocusHash != null)
        {
            root.addProperty("creator_focus_hash", this.creatorFocusHash.toString());
        }

        return root;
    }

    public boolean belongsTo(RecoveryWorldKey key)
    {
        return this.worldKey.equals(key);
    }

    public static Optional<RecoveryManifest> fromJson(JsonElement element)
    {
        try
        {
            if (element == null || !element.isJsonObject())
            {
                return Optional.empty();
            }

            JsonObject root = element.getAsJsonObject();
            int formatVersion = required(root, "format_version").getAsInt();
            UUID entryId = UUID.fromString(required(root, "entry_id").getAsString());
            RecoveryWorldKey worldKey = new RecoveryWorldKey(
                    required(root, "world").getAsString(),
                    required(root, "dimension").getAsString()
            );
            String schematicName = required(root, "schematic_name").getAsString();
            JsonElement originalFileElement = required(root, "original_file");
            String originalFile = originalFileElement.isJsonNull() ? null : originalFileElement.getAsString();
            String originalFileType = required(root, "original_file_type").getAsString();
            boolean dirty = required(root, "dirty").getAsBoolean();
            long cachedAt = required(root, "cached_at").getAsLong();
            long generation = required(root, "generation").getAsLong();
            String cacheFile = required(root, "cache_file").getAsString();
            JsonElement placementsElement = required(root, "placements");

            if (!placementsElement.isJsonArray())
            {
                return Optional.empty();
            }

            List<JsonObject> placements = new ArrayList<>();

            for (JsonElement placement : placementsElement.getAsJsonArray())
            {
                if (!placement.isJsonObject())
                {
                    return Optional.empty();
                }

                placements.add(placement.getAsJsonObject());
            }

            UUID selectedHash = optionalUuid(root, "selected_placement_hash");
            UUID focusHash = optionalUuid(root, "creator_focus_hash");

            return Optional.of(new RecoveryManifest(
                    formatVersion,
                    entryId,
                    worldKey,
                    schematicName,
                    originalFile,
                    originalFileType,
                    dirty,
                    cachedAt,
                    generation,
                    cacheFile,
                    placements,
                    selectedHash,
                    focusHash
            ));
        }
        catch (RuntimeException e)
        {
            return Optional.empty();
        }
    }

    private static JsonElement required(JsonObject root, String key)
    {
        JsonElement element = root.get(key);

        if (element == null || (element.isJsonNull() && !key.equals("original_file")))
        {
            throw new IllegalArgumentException("Missing recovery manifest field: " + key);
        }

        return element;
    }

    @Nullable
    private static UUID optionalUuid(JsonObject root, String key)
    {
        JsonElement element = root.get(key);
        return element == null || element.isJsonNull() ? null : UUID.fromString(element.getAsString());
    }

    private static String validateCacheFile(String cacheFile)
    {
        String value = requireNonBlank(cacheFile, "cacheFile");

        try
        {
            Path path = Path.of(value);

            if (path.isAbsolute() || path.getNameCount() != 1 || !path.getFileName().toString().equals(value) || !value.endsWith(".litematic"))
            {
                throw new IllegalArgumentException("Invalid recovery cache file name: " + value);
            }
        }
        catch (InvalidPathException e)
        {
            throw new IllegalArgumentException("Invalid recovery cache file name: " + value, e);
        }

        return value;
    }

    private static String requireNonBlank(String value, String name)
    {
        Objects.requireNonNull(value, name);

        if (value.isBlank())
        {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
