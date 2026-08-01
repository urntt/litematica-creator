package io.github.urntt.litematicacreator.creator;

import java.util.List;
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
        List<SchematicPlacement> candidates = CreatorCandidateSelection.mergeByIdentity(
                hitCandidates.stream().map(CreatorPlacementTarget::placement).toList(),
                writeCandidates.stream().map(CreatorPlacementTarget::placement).toList()
        );

        boolean focusAvailable = focus != null && focus.placement().isEnabled() &&
                                 !CreatorPlacementVisibility.isSuppressed(focus.placement());

        return switch (CreatorTargetDecision.decide(candidates.size(), focusAvailable))
        {
            case EDIT_CANDIDATE -> new Resolution(Action.EDIT, candidates.getFirst(), candidates);
            case EDIT_FOCUS -> new Resolution(Action.EDIT, focus.placement(), List.of());
            case CREATE_NEW -> new Resolution(Action.CREATE_NEW, null, List.of());
            case CHOOSE_OVERLAP -> new Resolution(Action.CHOOSE_OVERLAP, null, candidates);
        };
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
