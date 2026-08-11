package io.github.urntt.litematicacreator.creator;

final class CreatorTargetingPolicy
{
    private CreatorTargetingPolicy()
    {
    }

    static Source select(
            Source vanillaSource,
            double vanillaDistanceSqr,
            boolean schematicHit,
            double schematicDistanceSqr)
    {
        if (vanillaSource == Source.SCHEMATIC_BLOCK)
        {
            throw new IllegalArgumentException("The vanilla candidate cannot be a schematic block");
        }

        Source selected = schematicHit ? Source.SCHEMATIC_BLOCK : Source.MISS;
        double selectedDistance = schematicHit ? schematicDistanceSqr : Double.POSITIVE_INFINITY;

        if (vanillaSource != Source.MISS &&
            (selected == Source.MISS ||
             vanillaDistanceSqr < selectedDistance ||
             Double.compare(vanillaDistanceSqr, selectedDistance) == 0 && priority(vanillaSource) > priority(selected)))
        {
            selected = vanillaSource;
        }

        return selected;
    }

    private static int priority(Source source)
    {
        return switch (source)
        {
            case VANILLA_ENTITY -> 3;
            case SCHEMATIC_BLOCK -> 2;
            case VANILLA_BLOCK -> 1;
            case MISS -> 0;
        };
    }

    enum Source
    {
        MISS,
        VANILLA_BLOCK,
        SCHEMATIC_BLOCK,
        VANILLA_ENTITY
    }
}
