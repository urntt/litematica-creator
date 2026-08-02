package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorSchematicCoordinatesTest
{
    @Test
    void roundTripsEveryPlacementAndSubregionTransform()
    {
        assertEveryTransformRoundTrips(new BlockPos(7, 5, 9));
        assertEveryTransformRoundTrips(new BlockPos(-7, 5, -9));
    }

    private static void assertEveryTransformRoundTrips(BlockPos regionSize)
    {
        BlockPos containerPos = new BlockPos(3, 2, 4);
        BlockPos regionPos = new BlockPos(-7, 5, 13);
        BlockPos placementOrigin = new BlockPos(43, -12, 91);

        for (Mirror placementMirror : Mirror.values())
        {
            for (Rotation placementRotation : Rotation.values())
            {
                for (Mirror regionMirror : Mirror.values())
                {
                    for (Rotation regionRotation : Rotation.values())
                    {
                        BlockPos worldPos = CreatorSchematicCoordinates.toWorld(
                                containerPos,
                                regionPos,
                                regionSize,
                                placementOrigin,
                                placementMirror,
                                placementRotation,
                                regionMirror,
                                regionRotation
                        );
                        BlockPos roundTrip = CreatorSchematicCoordinates.toContainer(
                                worldPos,
                                regionPos,
                                regionSize,
                                placementOrigin,
                                placementMirror,
                                placementRotation,
                                regionMirror,
                                regionRotation
                        );

                        assertEquals(
                                containerPos,
                                roundTrip,
                                () -> "placement=" + placementMirror + "/" + placementRotation +
                                      ", region=" + regionMirror + "/" + regionRotation +
                                      ", size=" + regionSize
                        );
                    }
                }
            }
        }
    }
}
