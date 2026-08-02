package io.github.urntt.litematicacreator.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.mixin.LevelExtractorAccessor;

public final class CreatorCameraRenderStates
{
    private CreatorCameraRenderStates()
    {
    }

    public static void append(
            LevelExtractor extractor,
            Camera view,
            Frustum frustum,
            DeltaTracker deltaTracker,
            LevelRenderState output)
    {
        Minecraft minecraft = Minecraft.getInstance();
        CreatorCameraController controller = CreatorCameraController.getInstance();
        CreatorCameraEntity creatorCamera = controller.getCamera();
        LocalPlayer realPlayer = controller.getSessionPlayer();

        if (!controller.isActive() || minecraft.level == null || creatorCamera == null || realPlayer == null)
        {
            return;
        }

        Vec3 viewPosition = view.position();
        boolean realAlreadyPresent = containsAvatar(output, realPlayer.getId());
        boolean realVisible = extractor.isEntityVisible(
                realPlayer,
                frustum,
                viewPosition.x(),
                viewPosition.y(),
                viewPosition.z()
        );

        if (CreatorCameraRenderPolicy.shouldAppendRealPlayer(
                true,
                realPlayer.level() == minecraft.level,
                realVisible,
                realAlreadyPresent
        ))
        {
            output.entityRenderStates.add(extract(extractor, realPlayer, deltaTracker));
        }

        if (CreatorCameraRenderPolicy.shouldAppendCameraAvatar(
                true,
                creatorCamera.level() == minecraft.level,
                containsAvatar(output, creatorCamera.getId())
        ))
        {
            EntityRenderState cameraState = extract(extractor, creatorCamera, deltaTracker);
            makeCameraAvatarTranslucent(cameraState);
            output.entityRenderStates.add(cameraState);
        }

        output.lastEntityRenderStateCount = output.entityRenderStates.size();
    }

    private static EntityRenderState extract(LevelExtractor extractor, Entity entity, DeltaTracker deltaTracker)
    {
        boolean ticking = !entity.level().tickRateManager().isEntityFrozen(entity);
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(ticking);
        return ((LevelExtractorAccessor) (Object) extractor).litematicacreator$extractEntity(entity, partialTicks);
    }

    private static void makeCameraAvatarTranslucent(EntityRenderState state)
    {
        state.isInvisible = true;
        state.displayFireAnimation = false;
        state.nameTag = null;
        state.scoreText = null;
        state.shadowRadius = 0.0F;
        state.outlineColor = EntityRenderState.NO_OUTLINE;

        if (state instanceof LivingEntityRenderState livingState)
        {
            livingState.isInvisibleToPlayer = false;
            livingState.hasRedOverlay = false;
        }

        if (state instanceof AvatarRenderState avatarState)
        {
            // Keep the full avatar model; invisibility alone selects vanilla's translucent player render type.
            avatarState.isSpectator = false;
        }
    }

    private static boolean containsAvatar(LevelRenderState output, int entityId)
    {
        for (EntityRenderState state : output.entityRenderStates)
        {
            if (state instanceof AvatarRenderState avatarState && avatarState.id == entityId)
            {
                return true;
            }
        }

        return false;
    }
}
