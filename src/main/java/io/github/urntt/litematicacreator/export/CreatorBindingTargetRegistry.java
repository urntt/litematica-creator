package io.github.urntt.litematicacreator.export;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class CreatorBindingTargetRegistry
{
    private final Map<Path, Object> reservations = new HashMap<>();

    synchronized boolean reserve(Path target, Object token)
    {
        return this.reservations.putIfAbsent(target, token) == null;
    }

    synchronized boolean isReserved(Path target)
    {
        return this.reservations.containsKey(target);
    }

    synchronized boolean owns(Path target, Object token)
    {
        return this.reservations.get(target) == token;
    }

    synchronized boolean release(Path target, Object token)
    {
        return this.reservations.remove(target, token);
    }
}
