package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorPlacementTracePolicyTest
{
    @Test
    void blockTargetsRemainEditableRegardlessOfAirPlacementSetting()
    {
        assertEquals(
                CreatorPlacementTracePolicy.Action.USE_BLOCK_TARGET,
                decide(CreatorTargetingPolicy.Source.VANILLA_BLOCK, false)
        );
        assertEquals(
                CreatorPlacementTracePolicy.Action.USE_BLOCK_TARGET,
                decide(CreatorTargetingPolicy.Source.SCHEMATIC_BLOCK, true)
        );
    }

    @Test
    void entitiesAlwaysBlockInsteadOfFallingBackToAirPlacement()
    {
        assertEquals(
                CreatorPlacementTracePolicy.Action.BLOCKED_BY_ENTITY,
                decide(CreatorTargetingPolicy.Source.VANILLA_ENTITY, false)
        );
        assertEquals(
                CreatorPlacementTracePolicy.Action.BLOCKED_BY_ENTITY,
                decide(CreatorTargetingPolicy.Source.VANILLA_ENTITY, true)
        );
    }

    @Test
    void onlyATrueMissMayBecomeAnAirTarget()
    {
        assertEquals(
                CreatorPlacementTracePolicy.Action.NO_TARGET,
                decide(CreatorTargetingPolicy.Source.MISS, false)
        );
        assertEquals(
                CreatorPlacementTracePolicy.Action.USE_AIR_TARGET,
                decide(CreatorTargetingPolicy.Source.MISS, true)
        );
    }

    private static CreatorPlacementTracePolicy.Action decide(
            CreatorTargetingPolicy.Source source,
            boolean airPlacementEnabled)
    {
        return CreatorPlacementTracePolicy.decide(source, airPlacementEnabled);
    }
}
