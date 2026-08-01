package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventHandler;

public final class CreatorPlacementVisibility
{
    private static final Set<SchematicPlacement> SUPPRESSED = Collections.newSetFromMap(new IdentityHashMap<>());

    private CreatorPlacementVisibility()
    {
    }

    public static boolean isSuppressed(SchematicPlacement placement)
    {
        return SUPPRESSED.contains(placement);
    }

    public static void suppressOverlapAlternatives(SchematicPlacement selected, List<SchematicPlacement> candidates)
    {
        restoreAll();

        for (SchematicPlacement placement : candidates)
        {
            if (placement != selected)
            {
                changeSuppressed(placement, true);
            }
        }
    }

    public static void restoreAll()
    {
        for (SchematicPlacement placement : new ArrayList<>(SUPPRESSED))
        {
            changeSuppressed(placement, false);
        }
    }

    public static void onPlacementRemoved(SchematicPlacement placement)
    {
        SUPPRESSED.remove(placement);
    }

    private static void changeSuppressed(SchematicPlacement placement, boolean suppressed)
    {
        if (SUPPRESSED.contains(placement) == suppressed)
        {
            return;
        }

        SchematicPlacementEventHandler events = SchematicPlacementEventHandler.getInstance();
        events.invokePrePlacementChange(CreatorPlacementIndex.INSTANCE, placement);

        if (suppressed)
        {
            SUPPRESSED.add(placement);
        }
        else
        {
            SUPPRESSED.remove(placement);
        }

        events.invokePlacementModified(CreatorPlacementIndex.INSTANCE, placement);
    }
}
