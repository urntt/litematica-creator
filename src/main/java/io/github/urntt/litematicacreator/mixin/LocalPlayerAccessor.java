package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor
{
    @Accessor("autoJumpEnabled")
    void litematicacreator$setAutoJumpEnabled(boolean enabled);
}
