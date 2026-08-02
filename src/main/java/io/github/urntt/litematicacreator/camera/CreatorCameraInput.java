package io.github.urntt.litematicacreator.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

final class CreatorCameraInput extends KeyboardInput
{
    private final Minecraft minecraft;

    CreatorCameraInput(Minecraft minecraft)
    {
        super(minecraft.options);
        this.minecraft = minecraft;
    }

    @Override
    public void tick()
    {
        if (this.minecraft.gui.screen() == null && CreatorCameraController.getInstance().acceptsInput())
        {
            super.tick();
        }
        else
        {
            this.keyPresses = Input.EMPTY;
            this.moveVector = Vec2.ZERO;
        }
    }
}
