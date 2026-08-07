package io.github.urntt.litematicacreator.camera;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;

public final class CreatorCameraCollisionResolver
{
    private CreatorCameraCollisionResolver()
    {
    }

    @Nullable
    public static Vec3 resolveIfProjectionCollision(CreatorCameraEntity camera, Vec3 movement)
    {
        if (movement.lengthSqr() == 0.0D)
        {
            return null;
        }

        ClientLevel level = (ClientLevel) camera.level();
        AABB boundingBox = camera.getBoundingBox();
        AABB movementBounds = boundingBox.expandTowards(movement);
        List<VoxelShape> projectionColliders = CreatorProjectionCollisions.collect(camera, level, movementBounds);

        if (projectionColliders.isEmpty())
        {
            return null;
        }

        List<VoxelShape> entityColliders = level.getEntityCollisions(camera, movementBounds);
        Vec3 movementStep = collideBoundingBox(
                camera,
                level,
                movement,
                boundingBox,
                entityColliders,
                movementBounds,
                projectionColliders
        );
        boolean xCollision = movement.x != movementStep.x;
        boolean yCollision = movement.y != movementStep.y;
        boolean zCollision = movement.z != movementStep.z;
        boolean onGroundAfterCollision = yCollision && movement.y < 0.0D;

        if (camera.maxUpStep() > 0.0F &&
            (onGroundAfterCollision || camera.onGround()) &&
            (xCollision || zCollision))
        {
            AABB groundedBox = onGroundAfterCollision ? boundingBox.move(0.0D, movementStep.y, 0.0D) : boundingBox;
            AABB stepUpBounds = groundedBox.expandTowards(movement.x, camera.maxUpStep(), movement.z);

            if (!onGroundAfterCollision)
            {
                stepUpBounds = stepUpBounds.expandTowards(0.0D, -1.0E-5F, 0.0D);
            }

            List<VoxelShape> stepColliders = collectColliders(
                    camera,
                    level,
                    entityColliders,
                    stepUpBounds,
                    CreatorProjectionCollisions.collect(camera, level, stepUpBounds)
            );
            float[] candidateHeights = collectCandidateStepUpHeights(
                    groundedBox,
                    stepColliders,
                    camera.maxUpStep(),
                    (float) movementStep.y
            );

            for (float candidateHeight : candidateHeights)
            {
                Vec3 stepFromGround = collideWithShapes(
                        new Vec3(movement.x, candidateHeight, movement.z),
                        groundedBox,
                        stepColliders
                );

                if (stepFromGround.horizontalDistanceSqr() > movementStep.horizontalDistanceSqr())
                {
                    double distanceToGround = boundingBox.minY - groundedBox.minY;
                    return stepFromGround.subtract(0.0D, distanceToGround, 0.0D);
                }
            }
        }

        return movementStep;
    }

    // Mirrors the vanilla Entity collision ordering and step-up candidate rules.
    private static Vec3 collideBoundingBox(
            CreatorCameraEntity camera,
            ClientLevel level,
            Vec3 movement,
            AABB boundingBox,
            List<VoxelShape> entityColliders,
            AABB movementBounds,
            List<VoxelShape> projectionColliders)
    {
        List<VoxelShape> colliders = collectColliders(
                camera,
                level,
                entityColliders,
                movementBounds,
                projectionColliders
        );
        return collideWithShapes(movement, boundingBox, colliders);
    }

    private static List<VoxelShape> collectColliders(
            CreatorCameraEntity camera,
            ClientLevel level,
            List<VoxelShape> entityColliders,
            AABB bounds,
            List<VoxelShape> projectionColliders)
    {
        List<VoxelShape> colliders = new ArrayList<>(entityColliders.size() + projectionColliders.size() + 1);
        colliders.addAll(entityColliders);

        WorldBorder worldBorder = level.getWorldBorder();

        if (worldBorder.isInsideCloseToBorder(camera, bounds))
        {
            colliders.add(worldBorder.getCollisionShape());
        }

        for (VoxelShape shape : level.getBlockCollisions(camera, bounds))
        {
            colliders.add(shape);
        }

        colliders.addAll(projectionColliders);
        return List.copyOf(colliders);
    }

    static float[] collectCandidateStepUpHeights(
            AABB boundingBox,
            List<VoxelShape> colliders,
            float maxStepHeight,
            float stepHeightToSkip)
    {
        FloatSet candidates = new FloatArraySet(4);

        for (VoxelShape collider : colliders)
        {
            for (double coordinate : collider.getCoords(Direction.Axis.Y))
            {
                float relativeCoordinate = (float) (coordinate - boundingBox.minY);

                if (relativeCoordinate >= 0.0F && relativeCoordinate != stepHeightToSkip)
                {
                    if (relativeCoordinate > maxStepHeight)
                    {
                        break;
                    }

                    candidates.add(relativeCoordinate);
                }
            }
        }

        float[] sortedCandidates = candidates.toFloatArray();
        FloatArrays.unstableSort(sortedCandidates);
        return sortedCandidates;
    }

    static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes)
    {
        if (shapes.isEmpty())
        {
            return movement;
        }

        Vec3 resolvedMovement = Vec3.ZERO;

        for (Direction.Axis axis : Direction.axisStepOrder(movement))
        {
            double axisMovement = movement.get(axis);

            if (axisMovement != 0.0D)
            {
                double collision = Shapes.collide(axis, boundingBox.move(resolvedMovement), shapes, axisMovement);
                resolvedMovement = resolvedMovement.with(axis, collision);
            }
        }

        return resolvedMovement;
    }
}
