package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandBlockEditScreen.class)
public interface CommandBlockEditScreenAccessor
{
    @Accessor("mode")
    CommandBlockEntity.Mode litematicacreator$getMode();

    @Accessor("conditional")
    boolean litematicacreator$isConditional();

    @Accessor("autoexec")
    boolean litematicacreator$isAutoexec();
}
