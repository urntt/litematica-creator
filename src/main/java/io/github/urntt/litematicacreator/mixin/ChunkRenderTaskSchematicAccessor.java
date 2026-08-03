package io.github.urntt.litematicacreator.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;

@Mixin(ChunkRenderTaskSchematic.class)
public interface ChunkRenderTaskSchematicAccessor
{
    @Invoker("getRenderChunk")
    ChunkRendererSchematicVbo litematicacreator$invokeGetRenderChunk();

    @Invoker("getType")
    ChunkRenderTaskSchematic.Type litematicacreator$invokeGetType();
}
