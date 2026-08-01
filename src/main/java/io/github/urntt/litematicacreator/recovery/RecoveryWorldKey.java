package io.github.urntt.litematicacreator.recovery;

import java.util.Objects;

public record RecoveryWorldKey(String world, String dimension)
{
    public RecoveryWorldKey
    {
        world = requireValue(world, "world");
        dimension = requireValue(dimension, "dimension");
    }

    private static String requireValue(String value, String name)
    {
        Objects.requireNonNull(value, name);

        if (value.isBlank())
        {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
