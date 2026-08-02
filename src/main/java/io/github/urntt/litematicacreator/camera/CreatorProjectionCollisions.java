package io.github.urntt.litematicacreator.camera;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.creator.CreatorProjectionStateResolver;

public final class CreatorProjectionCollisions
{
    private static final double COLLISION_EPSILON = 1.0E-7D;

    private CreatorProjectionCollisions()
    {
    }

    public static List<VoxelShape> collect(CreatorCameraEntity camera, ClientLevel level, AABB bounds)
    {
        if (camera.isCreatorFlying() || !Configs.Generic.CREATOR_CAMERA_PROJECTION_COLLISION.getBooleanValue())
        {
            return List.of();
        }

        List<VoxelShape> shapes = new ArrayList<>();
        CollisionContext context = CollisionContext.of(camera);
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        CollisionGetter shapeWorld = schematicWorld != null ? schematicWorld : level;
        AABB queryBounds = bounds.inflate(COLLISION_EPSILON);

        for (BlockPos pos : BlockPos.betweenClosed(queryBounds))
        {
            BlockState state = CreatorProjectionStateResolver.resolve(level, pos).orElse(null);

            if (state == null || state.isAir())
            {
                continue;
            }

            VoxelShape shape = context.getCollisionShape(state, shapeWorld, pos);

            if (!shape.isEmpty())
            {
                VoxelShape moved = shape.move(pos);

                if (moved.bounds().intersects(queryBounds))
                {
                    shapes.add(moved);
                }
            }
        }

        return List.copyOf(shapes);
    }
}
