package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

public final class CreatorTargetResolver
{
    private CreatorTargetResolver()
    {
    }

    public static Resolution resolve(
            List<CreatorPlacementTarget> hitCandidates,
            List<CreatorPlacementTarget> writeCandidates,
            @Nullable CreatorFocus focus)
    {
        Set<SchematicPlacement> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<SchematicPlacement> candidates = new ArrayList<>();
        addUniquePlacements(hitCandidates, seen, candidates);
        addUniquePlacements(writeCandidates, seen, candidates);

        if (candidates.size() > 1)
        {
            return new Resolution(Action.CHOOSE_OVERLAP, null, List.copyOf(candidates));
        }

        if (candidates.size() == 1)
        {
            return new Resolution(Action.EDIT, candidates.getFirst(), List.copyOf(candidates));
        }

        if (focus != null)
        {
            return new Resolution(Action.EDIT, focus.placement(), List.of());
        }

        return new Resolution(Action.CREATE_NEW, null, List.of());
    }

    private static void addUniquePlacements(
            List<CreatorPlacementTarget> targets,
            Set<SchematicPlacement> seen,
            List<SchematicPlacement> candidates)
    {
        for (CreatorPlacementTarget target : targets)
        {
            if (seen.add(target.placement()))
            {
                candidates.add(target.placement());
            }
        }
    }

    public enum Action
    {
        EDIT,
        CREATE_NEW,
        CHOOSE_OVERLAP
    }

    public record Resolution(
            Action action,
            @Nullable SchematicPlacement placement,
            List<SchematicPlacement> candidates)
    {
    }
}
