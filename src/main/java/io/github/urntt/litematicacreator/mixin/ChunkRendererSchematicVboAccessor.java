package io.github.urntt.litematicacreator.mixin;

import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;

@Mixin(ChunkRendererSchematicVbo.class)
public interface ChunkRendererSchematicVboAccessor
{
    @Invoker("getChunkPos")
    ChunkPos litematicacreator$invokeGetChunkPos();

    @Invoker("rebuildWorldView")
    void litematicacreator$invokeRebuildWorldView();
}
