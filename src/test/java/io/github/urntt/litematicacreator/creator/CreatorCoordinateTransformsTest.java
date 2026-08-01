package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorCoordinateTransformsTest
{
    @Test
    void roundTripsEveryPlacementRotationAndMirror()
    {
        BlockPos origin = new BlockPos(43, -12, 91);
        BlockPos relative = new BlockPos(-7, 5, 13);

        for (Mirror mirror : Mirror.values())
        {
            for (Rotation rotation : Rotation.values())
            {
                BlockPos world = CreatorCoordinateTransforms.toWorld(relative, origin, mirror, rotation);
                assertEquals(relative, CreatorCoordinateTransforms.toSchematicRelative(world, origin, mirror, rotation));
            }
        }
    }
}
