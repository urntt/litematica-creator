package io.github.urntt.litematicacreator.export;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import io.github.urntt.litematicacreator.LitematicaCreator;

public final class CreatorSchematicMetadataCopies
{
    private CreatorSchematicMetadataCopies()
    {
    }

    public static void copy(SchematicMetadata target, SchematicMetadata source)
    {
        if (target == source)
        {
            return;
        }

        synchronized (source)
        {
            int[] preview = snapshotPreview(source);

            try
            {
                target.copyFrom(source);
            }
            finally
            {
                source.setPreviewImagePixelData(clonePreview(preview));
            }

            target.setPreviewImagePixelData(clonePreview(preview));
        }
    }

    @Nullable
    public static int[] snapshotPreview(SchematicMetadata metadata)
    {
        synchronized (metadata)
        {
            try
            {
                int[] preview = clonePreview(metadata.getPreviewImagePixelData());
                metadata.setPreviewImagePixelData(clonePreview(preview));
                return preview;
            }
            catch (IllegalStateException exception)
            {
                LitematicaCreator.LOGGER.warn("Discarding an already-consumed schematic preview stream", exception);
                metadata.setPreviewImagePixelData(null);
                return null;
            }
        }
    }

    public static CompoundTag writeSchematicToNbt(LitematicaSchematic schematic)
    {
        SchematicMetadata metadata = schematic.getMetadata();

        synchronized (metadata)
        {
            // Clear preview streams left consumed by older Creator builds before serialization.
            snapshotPreview(metadata);
            return schematic.writeToNBT();
        }
    }

    @Nullable
    private static int[] clonePreview(@Nullable int[] preview)
    {
        return preview != null ? preview.clone() : null;
    }
}
