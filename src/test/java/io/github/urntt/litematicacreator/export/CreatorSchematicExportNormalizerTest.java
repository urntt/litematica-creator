package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import java.time.ZoneId;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class CreatorSchematicExportNormalizerTest
{
    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long EXPORT_TIME = 1_800_000_000_000L;

    @Test
    void replacesCreatorDraftNameWithFinalFileStem()
    {
        CompoundTag original = schematic("creator-draft-20260808-123456", 123L);
        CompoundTag before = original.copy();

        CompoundTag normalized = normalize(original, "test1.litematic");

        assertNotSame(original, normalized);
        assertEquals("test1", metadata(normalized).getStringOr("Name", ""));
        assertEquals(before, original);
    }

    @Test
    void acceptsExtensionlessUnicodeAndAlreadySanitizedFinalNames()
    {
        CompoundTag original = schematic("creator-draft-20260808-123456", 123L);

        assertEquals("test1", metadata(normalize(original, "test1")).getStringOr("Name", ""));
        assertEquals("建筑方案", metadata(normalize(original, "建筑方案.litematic")).getStringOr("Name", ""));
        assertEquals("方案一", metadata(normalize(original, "方案一.litematic")).getStringOr("Name", ""));
    }

    @Test
    void preservesValidCreationTimeAndUnrelatedData()
    {
        CompoundTag original = schematic("creator-draft-20260808-123456", 456L);
        CompoundTag regions = new CompoundTag();
        regions.putString("marker", "unchanged");
        original.put("Regions", regions);
        metadata(original).putLong("TimeModified", 789L);
        metadata(original).putString("Author", "author");

        CompoundTag normalized = normalize(original, "renamed.litematic");

        assertEquals(456L, metadata(normalized).getLongOr("TimeCreated", -1L));
        assertEquals(789L, metadata(normalized).getLongOr("TimeModified", -1L));
        assertEquals("author", metadata(normalized).getStringOr("Author", ""));
        assertEquals("unchanged", normalized.getCompoundOrEmpty("Regions").getStringOr("marker", ""));
    }

    @Test
    void recoversMissingZeroAndNegativeCreationTimesFromDraftName()
    {
        long expected = LocalDateTime.of(2026, 8, 8, 12, 34, 56)
                .atZone(TEST_ZONE)
                .toInstant()
                .toEpochMilli();

        CompoundTag missing = schematic("creator-draft-20260808-123456", null);
        CompoundTag zero = schematic("creator-draft-20260808-123456", 0L);
        CompoundTag negative = schematic("creator-draft-20260808-123456", -1L);

        assertEquals(expected, metadata(normalize(missing, "missing.litematic")).getLongOr("TimeCreated", -1L));
        assertEquals(expected, metadata(normalize(zero, "zero.litematic")).getLongOr("TimeCreated", -1L));
        assertEquals(expected, metadata(normalize(negative, "negative.litematic")).getLongOr("TimeCreated", -1L));
    }

    @Test
    void invalidDraftDateFallsBackToExportTime()
    {
        CompoundTag original = schematic("creator-draft-20260230-120000", -1L);

        CompoundTag normalized = normalize(original, "fallback.litematic");

        assertEquals(EXPORT_TIME, metadata(normalized).getLongOr("TimeCreated", -1L));
    }

    @Test
    void leavesOrdinarySchematicAndInputNbtUntouched()
    {
        CompoundTag original = schematic("ordinary-schematic", -1L);
        CompoundTag before = original.copy();

        CompoundTag normalized = normalize(original, "save-as.litematic");

        assertSame(original, normalized);
        assertEquals(before, original);
    }

    private static CompoundTag normalize(CompoundTag original, String finalFileName)
    {
        return CreatorSchematicExportNormalizer.normalizeForExport(original, finalFileName, EXPORT_TIME, TEST_ZONE);
    }

    private static CompoundTag schematic(String name, Long timeCreated)
    {
        CompoundTag schematic = new CompoundTag();
        CompoundTag metadata = new CompoundTag();
        metadata.putString("Name", name);

        if (timeCreated != null)
        {
            metadata.putLong("TimeCreated", timeCreated);
        }

        schematic.put("Metadata", metadata);
        return schematic;
    }

    private static CompoundTag metadata(CompoundTag schematic)
    {
        return schematic.getCompoundOrEmpty("Metadata");
    }
}
