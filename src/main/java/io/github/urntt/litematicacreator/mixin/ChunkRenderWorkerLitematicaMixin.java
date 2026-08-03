package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;

import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderWorkerLitematica;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditGuard;

@Mixin(ChunkRenderWorkerLitematica.class)
public class ChunkRenderWorkerLitematicaMixin
{
    @WrapMethod(method = "processTask")
    private void litematicacreator$readStableSchematicChunk(
            ChunkRenderTaskSchematic task,
            Operation<Void> original) throws InterruptedException
    {
        ChunkRenderTaskSchematicAccessor taskAccessor = (ChunkRenderTaskSchematicAccessor) task;
        ChunkRendererSchematicVbo renderer =
                taskAccessor.litematicacreator$invokeGetRenderChunk();
        ChunkPos chunkPos = renderer == null
                ? null
                : ((ChunkRendererSchematicVboAccessor) renderer).litematicacreator$invokeGetChunkPos();

        if (chunkPos == null)
        {
            original.call(task);
            return;
        }

        CreatorSchematicEditGuard.runRenderCompile(chunkPos.pack(), () -> {
            if (task.getStatus() == ChunkRenderTaskSchematic.Status.PENDING &&
                taskAccessor.litematicacreator$invokeGetType() == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                ((ChunkRendererSchematicVboAccessor) renderer).litematicacreator$invokeRebuildWorldView();
            }

            original.call(task);
        });
    }
}
