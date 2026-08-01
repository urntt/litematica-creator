package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class CreatorCandidateSelection
{
    private CreatorCandidateSelection()
    {
    }

    public static <T> List<T> mergeByIdentity(List<T> first, List<T> second)
    {
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<T> merged = new ArrayList<>();
        addUnique(first, seen, merged);
        addUnique(second, seen, merged);
        return List.copyOf(merged);
    }

    private static <T> void addUnique(List<T> values, Set<T> seen, List<T> merged)
    {
        for (T value : values)
        {
            if (seen.add(value))
            {
                merged.add(value);
            }
        }
    }
}
