package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.camera.CreatorCameraController;

@Mixin(value = LocalPlayer.class, priority = 1100)
public abstract class LocalPlayerMixin
{
    @Shadow public ClientInput input;

    @Unique
    private final ClientInput litematicacreator$emptyInput = new ClientInput();

    @WrapMethod(method = "tick")
    private void litematicacreator$isolateRealPlayerTick(Operation<Void> original)
    {
        LocalPlayer player = (LocalPlayer) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        CreatorCameraController controller = CreatorCameraController.getInstance();

        if (!controller.shouldIsolatePlayer(player, minecraft))
        {
            original.call();
            return;
        }

        ClientInput previousInput = this.input;
        this.input = this.litematicacreator$emptyInput;
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setJumping(false);
        player.setShiftKeyDown(false);
        player.setSprinting(false);

        try
        {
            original.call();
        }
        finally
        {
            this.input = previousInput;
        }
    }

    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$preventRealPlayerCameraPackets(CallbackInfoReturnable<Boolean> cir)
    {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (CreatorCameraController.getInstance().shouldIsolatePlayer(player, Minecraft.getInstance()))
        {
            cir.setReturnValue(false);
        }
    }
}
