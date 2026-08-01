package io.github.urntt.litematicacreator.recovery;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class RecoverySuppressionSet<T>
{
    private final Set<T> values = Collections.newSetFromMap(new IdentityHashMap<>());

    public void suppress(T value)
    {
        this.values.add(value);
    }

    public void release(T value)
    {
        this.values.remove(value);
    }

    public boolean contains(T value)
    {
        return this.values.contains(value);
    }

    public void clear()
    {
        this.values.clear();
    }
}
