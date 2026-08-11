package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorTargetingPolicyTest
{
    @Test
    void noCandidatesProducesAMiss()
    {
        assertEquals(
                CreatorTargetingPolicy.Source.MISS,
                select(CreatorTargetingPolicy.Source.MISS, Double.POSITIVE_INFINITY, false, Double.POSITIVE_INFINITY)
        );
    }

    @Test
    void individualCandidatesArePreserved()
    {
        assertEquals(
                CreatorTargetingPolicy.Source.VANILLA_BLOCK,
                select(CreatorTargetingPolicy.Source.VANILLA_BLOCK, 4.0, false, Double.POSITIVE_INFINITY)
        );
        assertEquals(
                CreatorTargetingPolicy.Source.VANILLA_ENTITY,
                select(CreatorTargetingPolicy.Source.VANILLA_ENTITY, 4.0, false, Double.POSITIVE_INFINITY)
        );
        assertEquals(
                CreatorTargetingPolicy.Source.SCHEMATIC_BLOCK,
                select(CreatorTargetingPolicy.Source.MISS, Double.POSITIVE_INFINITY, true, 4.0)
        );
    }

    @Test
    void theNearestBlockCandidateWins()
    {
        assertEquals(
                CreatorTargetingPolicy.Source.VANILLA_BLOCK,
                select(CreatorTargetingPolicy.Source.VANILLA_BLOCK, 4.0, true, 9.0)
        );
        assertEquals(
                CreatorTargetingPolicy.Source.SCHEMATIC_BLOCK,
                select(CreatorTargetingPolicy.Source.VANILLA_BLOCK, 9.0, true, 4.0)
        );
    }

    @Test
    void anEntityOnlyOccludesTargetsAtOrBehindIt()
    {
        assertEquals(
                CreatorTargetingPolicy.Source.VANILLA_ENTITY,
                select(CreatorTargetingPolicy.Source.VANILLA_ENTITY, 4.0, true, 9.0)
        );
        assertEquals(
                CreatorTargetingPolicy.Source.SCHEMATIC_BLOCK,
                select(CreatorTargetingPolicy.Source.VANILLA_ENTITY, 9.0, true, 4.0)
        );
        assertEquals(
                CreatorTargetingPolicy.Source.VANILLA_ENTITY,
                select(CreatorTargetingPolicy.Source.VANILLA_ENTITY, 4.0, true, 4.0)
        );
    }

    @Test
    void aSchematicBlockWinsATieWithARealBlock()
    {
        assertEquals(
                CreatorTargetingPolicy.Source.SCHEMATIC_BLOCK,
                select(CreatorTargetingPolicy.Source.VANILLA_BLOCK, 4.0, true, 4.0)
        );
    }

    private static CreatorTargetingPolicy.Source select(
            CreatorTargetingPolicy.Source vanillaSource,
            double vanillaDistance,
            boolean schematicHit,
            double schematicDistance)
    {
        return CreatorTargetingPolicy.select(
                vanillaSource,
                vanillaDistance,
                schematicHit,
                schematicDistance
        );
    }
}
