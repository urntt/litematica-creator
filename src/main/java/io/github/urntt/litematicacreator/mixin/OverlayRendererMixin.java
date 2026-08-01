package io.github.urntt.litematicacreator.mixin;

import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer.BoxType;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.Box;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorPlacementVisibility;

@Mixin(OverlayRenderer.class)
public class OverlayRendererMixin
{
    @Shadow
    @Final
    private Map<SchematicPlacement, ImmutableMap<String, Box>> placements;

    @Inject(method = "updatePlacementCache", at = @At("RETURN"))
    private void litematicacreator$removeSuppressedPlacements(CallbackInfo ci)
    {
        this.placements.keySet().removeIf(CreatorPlacementVisibility::isSuppressed);
    }

    @Inject(method = "renderSelectionBox", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$hideCreatorSubRegionBoxes(
            Box box,
            BoxType boxType,
            float expand,
            float lineWidthBlockBox,
            float lineWidthArea,
            @Nullable SchematicPlacement placement,
            CallbackInfo ci)
    {
        if (placement != null && CreatorManager.getInstance().isCreatorModeEnabled() &&
            Configs.Generic.HIDE_SUBREGION_BOXES_IN_CREATOR_MODE.getBooleanValue())
        {
            ci.cancel();
        }
    }
}
