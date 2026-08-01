package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.util.GuiUtils;
import io.github.urntt.litematicacreator.creator.CreatorManager;

@Mixin(fi.dy.masa.litematica.event.InputHandler.class)
public abstract class LitematicaInputHandlerMixin
{
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$skipRebuildKeyboardInput(
            KeyEvent input,
            boolean eventKeyState,
            CallbackInfoReturnable<Boolean> cir)
    {
        Minecraft mc = Minecraft.getInstance();

        if (eventKeyState && shouldSuppressLitematicaInput() &&
            (mc.options.keyUse.matches(input) || mc.options.keyAttack.matches(input)))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$skipRebuildMouseInput(
            MouseButtonEvent click,
            boolean eventButtonState,
            CallbackInfoReturnable<Boolean> cir)
    {
        Minecraft mc = Minecraft.getInstance();

        if (eventButtonState && shouldSuppressLitematicaInput() &&
            (mc.options.keyUse.matchesMouse(click) || mc.options.keyAttack.matchesMouse(click)))
        {
            cir.setReturnValue(false);
        }
    }

    private static boolean shouldSuppressLitematicaInput()
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() && GuiUtils.getCurrentScreen() == null;
    }
}
