package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fi.dy.masa.litematica.schematic.SchematicMetadata;

class CreatorSchematicMetadataCopiesTest
{
    @Test
    void copyingMetadataDoesNotConsumeTheSourcePreview()
    {
        SchematicMetadata source = metadataWithPreview(1, 2, 3, 4);
        SchematicMetadata first = new SchematicMetadata();
        SchematicMetadata second = new SchematicMetadata();

        CreatorSchematicMetadataCopies.copy(first, source);
        CreatorSchematicMetadataCopies.copy(second, source);

        assertArrayEquals(new int[] { 1, 2, 3, 4 }, CreatorSchematicMetadataCopies.snapshotPreview(source));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, CreatorSchematicMetadataCopies.snapshotPreview(first));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, CreatorSchematicMetadataCopies.snapshotPreview(second));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, source.writeData().getIntArray("PreviewImageData"));
    }

    @Test
    void repeatedPreviewSnapshotsRemainReadable()
    {
        SchematicMetadata metadata = metadataWithPreview(9, 8, 7, 6);

        assertArrayEquals(new int[] { 9, 8, 7, 6 }, CreatorSchematicMetadataCopies.snapshotPreview(metadata));
        assertArrayEquals(new int[] { 9, 8, 7, 6 }, CreatorSchematicMetadataCopies.snapshotPreview(metadata));
    }

    @Test
    void alreadyConsumedPreviewIsClearedInsteadOfCrashingLaterSerialization()
    {
        SchematicMetadata source = metadataWithPreview(5, 4, 3, 2);
        new SchematicMetadata().copyFrom(source);

        assertNull(CreatorSchematicMetadataCopies.snapshotPreview(source));
        assertDoesNotThrow(source::writeData);
    }

    private static SchematicMetadata metadataWithPreview(int... pixels)
    {
        SchematicMetadata metadata = new SchematicMetadata();
        metadata.setPreviewImagePixelData(pixels);
        return metadata;
    }
}
