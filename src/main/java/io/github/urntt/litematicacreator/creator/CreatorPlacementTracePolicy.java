package io.github.urntt.litematicacreator.creator;

final class CreatorPlacementTracePolicy
{
    private CreatorPlacementTracePolicy()
    {
    }

    static Action decide(CreatorTargetingPolicy.Source source, boolean airPlacementEnabled)
    {
        return switch (source)
        {
            case VANILLA_BLOCK, SCHEMATIC_BLOCK -> Action.USE_BLOCK_TARGET;
            case VANILLA_ENTITY -> Action.BLOCKED_BY_ENTITY;
            case MISS -> airPlacementEnabled ? Action.USE_AIR_TARGET : Action.NO_TARGET;
        };
    }

    enum Action
    {
        USE_BLOCK_TARGET,
        USE_AIR_TARGET,
        NO_TARGET,
        BLOCKED_BY_ENTITY
    }
}
