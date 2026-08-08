package io.github.urntt.litematicacreator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CreatorExportRegionModeTest
{
    @Test
    void serializesEveryModeUsingStableValues()
    {
        assertEquals("raw", CreatorExportRegionMode.RAW.getStringValue());
        assertEquals("sparse_compact", CreatorExportRegionMode.SPARSE_COMPACT.getStringValue());
        assertEquals("enclosing_projection", CreatorExportRegionMode.ENCLOSING_PROJECTION.getStringValue());
        assertEquals("enclosing_with_world", CreatorExportRegionMode.ENCLOSING_WITH_WORLD.getStringValue());
    }

    @Test
    void restoresKnownModesAndFallsBackToSparseCompaction()
    {
        assertEquals(CreatorExportRegionMode.RAW, CreatorExportRegionMode.RAW.fromString("RAW"));
        assertEquals(CreatorExportRegionMode.ENCLOSING_WITH_WORLD, CreatorExportRegionMode.RAW.fromString("enclosing_with_world"));
        assertEquals(CreatorExportRegionMode.SPARSE_COMPACT, CreatorExportRegionMode.RAW.fromString("unknown"));
        assertEquals(CreatorExportRegionMode.SPARSE_COMPACT, CreatorExportRegionMode.RAW.fromString(null));
    }

    @Test
    void cyclesAcrossAllFourModesInBothDirections()
    {
        assertEquals(CreatorExportRegionMode.SPARSE_COMPACT, CreatorExportRegionMode.RAW.cycle(true));
        assertEquals(CreatorExportRegionMode.RAW, CreatorExportRegionMode.ENCLOSING_WITH_WORLD.cycle(true));
        assertEquals(CreatorExportRegionMode.ENCLOSING_WITH_WORLD, CreatorExportRegionMode.RAW.cycle(false));
    }
}
