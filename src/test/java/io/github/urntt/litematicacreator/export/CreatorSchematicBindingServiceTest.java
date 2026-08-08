package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import fi.dy.masa.litematica.schematic.SchematicMetadata;

class CreatorSchematicBindingServiceTest
{
    @Test
    void reservesEachNormalizedTargetForOnlyOneSaveAtATime()
    {
        CreatorBindingTargetRegistry registry = new CreatorBindingTargetRegistry();
        Path target = Path.of("build", "reservation-test").toAbsolutePath();
        Object first = new Object();
        Object second = new Object();

        assertTrue(registry.reserve(target, first));
        assertFalse(registry.reserve(target, second));
        assertTrue(registry.owns(target, first));
        assertFalse(registry.owns(target, second));
        assertTrue(registry.release(target, first));
        assertTrue(registry.reserve(target, second));
    }

    @Test
    void appliesSavedIdentityOnlyWhenThatFieldWasNotEditedAfterSnapshot()
    {
        SchematicMetadata current = new SchematicMetadata();
        current.setName("concurrent-name");
        current.setAuthor("source-author");
        current.setDescription("source-description");
        current.setTimeCreated(10L);
        current.setTimeModified(25L);
        current.setPreviewImagePixelData(new int[] { 1, 2 });
        CompoundTag source = metadata("source-name", "source-author", "source-description", 10L, 20L, new int[] { 1, 2 });
        CompoundTag output = metadata("saved-name", "saved-author", "saved-description", 11L, 21L, new int[] { 3, 4 });

        CreatorSchematicBindingService.applyIdentityMetadata(current, source, output);

        assertEquals("concurrent-name", current.getName());
        assertEquals("saved-author", current.getAuthor());
        assertEquals("saved-description", current.getDescription());
        assertEquals(11L, current.getTimeCreated());
        assertEquals(25L, current.getTimeModified());
        assertEquals(3, current.getPreviewImagePixelData()[0]);
    }

    @Test
    void comparesBindingPathsAfterAbsoluteNormalization()
    {
        Path base = Path.of("build", "binding").toAbsolutePath();

        assertTrue(CreatorSchematicBindingService.samePath(base.resolve("folder").resolve(".."), base));
        assertFalse(CreatorSchematicBindingService.samePath(null, base));
        assertFalse(CreatorSchematicBindingService.samePath(base.resolve("other"), base));
    }

    private static CompoundTag metadata(
            String name,
            String author,
            String description,
            long created,
            long modified,
            int[] preview)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Author", author);
        tag.putString("Description", description);
        tag.putLong("TimeCreated", created);
        tag.putLong("TimeModified", modified);
        tag.putIntArray("PreviewImageData", preview);
        return tag;
    }
}
