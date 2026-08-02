package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.camera.CreatorCameraChunkRefresh;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
    @Unique private boolean litematicacreator$hasCameraChunk;
    @Unique private int litematicacreator$cameraChunkX;
    @Unique private int litematicacreator$cameraChunkZ;

    @Inject(method = "repositionCamera", at = @At("HEAD"), require = 0)
    private void litematicacreator$rebuildChunksAtCameraEdge(CameraRenderState camera, CallbackInfo ci)
    {
        CreatorCameraController controller = CreatorCameraController.getInstance();

        if (!controller.isActive())
        {
            this.litematicacreator$hasCameraChunk = false;
            return;
        }

        int chunkX = SectionPos.blockToSectionCoord(camera.pos.x);
        int chunkZ = SectionPos.blockToSectionCoord(camera.pos.z);

        if (this.litematicacreator$hasCameraChunk &&
            (chunkX != this.litematicacreator$cameraChunkX || chunkZ != this.litematicacreator$cameraChunkZ))
        {
            CreatorCameraChunkRefresh.markTransition(
                    net.minecraft.client.Minecraft.getInstance(),
                    chunkX,
                    chunkZ,
                    this.litematicacreator$cameraChunkX,
                    this.litematicacreator$cameraChunkZ
            );
        }

        this.litematicacreator$cameraChunkX = chunkX;
        this.litematicacreator$cameraChunkZ = chunkZ;
        this.litematicacreator$hasCameraChunk = true;
    }
}
