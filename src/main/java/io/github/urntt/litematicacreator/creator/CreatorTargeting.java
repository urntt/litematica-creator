package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import io.github.urntt.litematicacreator.config.Configs;

public final class CreatorTargeting
{
    private CreatorTargeting()
    {
    }

    @Nullable
    public static RayTraceWrapper trace(Minecraft mc)
    {
        return traceResult(mc).trace();
    }

    static TraceResult traceResult(Minecraft mc)
    {
        Entity camera = CreatorCameraCompat.getCameraEntity();

        if (camera == null || mc.level == null)
        {
            return TraceResult.miss();
        }

        double range = Configs.Generic.CREATOR_EDIT_RANGE.getIntegerValue();
        Vec3 eyes = camera.getEyePosition(1.0F);
        // Litematica's generic trace drops entity hits, so preserve and merge both traces here.
        HitResult vanilla = RayTraceUtils.getRayTraceFromEntity(mc.level, camera, false, range);
        @Nullable BlockHitResult schematic = RayTraceUtils.traceToSchematicWorld(camera, range, true, false);
        CreatorTargetingPolicy.Source vanillaSource = switch (vanilla.getType())
        {
            case ENTITY -> CreatorTargetingPolicy.Source.VANILLA_ENTITY;
            case BLOCK -> CreatorTargetingPolicy.Source.VANILLA_BLOCK;
            case MISS -> CreatorTargetingPolicy.Source.MISS;
        };
        boolean schematicHit = schematic != null && schematic.getType() == HitResult.Type.BLOCK;
        double vanillaDistance = vanillaSource != CreatorTargetingPolicy.Source.MISS ?
                eyes.distanceToSqr(vanilla.getLocation()) : Double.POSITIVE_INFINITY;
        double schematicDistance = schematicHit ?
                eyes.distanceToSqr(schematic.getLocation()) : Double.POSITIVE_INFINITY;
        CreatorTargetingPolicy.Source source = CreatorTargetingPolicy.select(
                vanillaSource,
                vanillaDistance,
                schematicHit,
                schematicDistance
        );

        return switch (source)
        {
            case VANILLA_ENTITY -> new TraceResult(
                    source,
                    new RayTraceWrapper(RayTraceWrapper.HitType.VANILLA_ENTITY, (EntityHitResult) vanilla)
            );
            case SCHEMATIC_BLOCK -> new TraceResult(
                    source,
                    new RayTraceWrapper(RayTraceWrapper.HitType.SCHEMATIC_BLOCK, schematic)
            );
            case VANILLA_BLOCK -> new TraceResult(
                    source,
                    new RayTraceWrapper(RayTraceWrapper.HitType.VANILLA_BLOCK, (BlockHitResult) vanilla)
            );
            case MISS -> TraceResult.miss();
        };
    }

    record TraceResult(CreatorTargetingPolicy.Source source, @Nullable RayTraceWrapper trace)
    {
        private static TraceResult miss()
        {
            return new TraceResult(CreatorTargetingPolicy.Source.MISS, null);
        }
    }
}
