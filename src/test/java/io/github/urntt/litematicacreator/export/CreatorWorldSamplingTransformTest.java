package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class CreatorWorldSamplingTransformTest
{
    private static final BlockPos ORIGIN = new BlockPos(10, 20, 30);
    private static final BlockPos LOCAL = new BlockPos(2, 3, 4);

    @Test
    void appliesPlacementOriginRotationAndMirror()
    {
        assertEquals(new BlockPos(12, 23, 34), transform(Mirror.NONE, Rotation.NONE));
        assertEquals(new BlockPos(6, 23, 32), transform(Mirror.NONE, Rotation.CLOCKWISE_90));
        assertEquals(new BlockPos(8, 23, 26), transform(Mirror.NONE, Rotation.CLOCKWISE_180));
        assertEquals(new BlockPos(14, 23, 28), transform(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90));
        assertEquals(new BlockPos(12, 23, 26), transform(Mirror.LEFT_RIGHT, Rotation.NONE));
        assertEquals(new BlockPos(14, 23, 32), transform(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90));
        assertEquals(new BlockPos(12, 23, 26), transform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_180));
    }

    private static BlockPos transform(Mirror mirror, Rotation rotation)
    {
        return new CreatorWorldSamplingTransform(ORIGIN, mirror, rotation).toWorld(LOCAL);
    }
}
