package io.github.urntt.litematicacreator.creator;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.config.Configs;

final class CreatorPlacementEntityCollision
{
    private CreatorPlacementEntityCollision()
    {
    }

    static boolean canPlace(Minecraft minecraft, Level schematicWorld, BlockState state, BlockPos pos)
    {
        CreatorCameraController controller = CreatorCameraController.getInstance();

        if (!CreatorPlacementEntityCollisionPolicy.shouldCheck(
                controller.isActive(),
                Configs.Generic.IGNORE_CREATOR_CAMERA_ENTITY_PLACEMENT_COLLISION.getBooleanValue()
        ) || minecraft.level == null || minecraft.player == null)
        {
            return true;
        }

        CollisionContext context = CollisionContext.placementContext(minecraft.player);
        VoxelShape localShape = context.getCollisionShape(state, schematicWorld, pos);

        if (localShape.isEmpty())
        {
            return true;
        }

        VoxelShape worldShape = localShape.move(pos);
        boolean realWorldUnobstructed = minecraft.level.isUnobstructed(null, worldShape);
        CreatorCameraEntity camera = controller.getCamera();
        boolean cameraIntersects = camera != null && Shapes.joinIsNotEmpty(
                worldShape,
                Shapes.create(camera.getBoundingBox()),
                BooleanOp.AND
        );
        return CreatorPlacementEntityCollisionPolicy.canPlace(realWorldUnobstructed, cameraIntersects);
    }
}
