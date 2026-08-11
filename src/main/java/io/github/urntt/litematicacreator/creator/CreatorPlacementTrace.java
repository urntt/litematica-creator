package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

record CreatorPlacementTrace(Kind kind, @Nullable CreatorEditTarget target)
{
    CreatorPlacementTrace
    {
        if ((kind == Kind.TARGET) != (target != null))
        {
            throw new IllegalArgumentException("Only a TARGET placement trace may contain a target");
        }
    }

    static CreatorPlacementTrace target(CreatorEditTarget target)
    {
        return new CreatorPlacementTrace(Kind.TARGET, target);
    }

    static CreatorPlacementTrace noTarget()
    {
        return new CreatorPlacementTrace(Kind.NO_TARGET, null);
    }

    static CreatorPlacementTrace blockedByEntity()
    {
        return new CreatorPlacementTrace(Kind.BLOCKED_BY_ENTITY, null);
    }

    enum Kind
    {
        TARGET,
        NO_TARGET,
        BLOCKED_BY_ENTITY
    }
}
