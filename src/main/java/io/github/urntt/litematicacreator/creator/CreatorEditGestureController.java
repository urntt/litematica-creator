package io.github.urntt.litematicacreator.creator;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.util.GuiUtils;

public final class CreatorEditGestureController
{
    public static final CreatorEditGestureController INSTANCE = new CreatorEditGestureController();

    private static final int PLACE_INTERVAL_TICKS = 4;

    private final InputLatch placeInput = new InputLatch();
    private final InputLatch breakInput = new InputLatch();
    private long nextPlaceTick;

    private CreatorEditGestureController()
    {
    }

    public void onPlaceInput(boolean pressed, boolean acceptsCreatorEdits)
    {
        this.placeInput.update(pressed, acceptsCreatorEdits);
    }

    public void onBreakInput(boolean pressed, boolean acceptsCreatorEdits)
    {
        this.breakInput.update(pressed, acceptsCreatorEdits);
    }

    public void onClientTick(Minecraft mc, long tick)
    {
        boolean acceptsCreatorEdits = CreatorManager.getInstance().isCreatorModeEnabled() &&
                                      mc.level != null &&
                                      mc.player != null &&
                                      GuiUtils.getCurrentScreen() == null;
        boolean placeDown = mc.options.keyUse.isDown();
        boolean breakDown = mc.options.keyAttack.isDown();

        this.placeInput.sync(placeDown, acceptsCreatorEdits);
        this.breakInput.sync(breakDown, acceptsCreatorEdits);

        if (!acceptsCreatorEdits)
        {
            this.suspend(placeDown, breakDown);
            return;
        }

        CreatorEditService edits = CreatorEditService.getInstance();

        while (this.breakInput.consumePress())
        {
            edits.deleteProjectionBlock();

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
        }

        boolean freshPlace = false;

        while (this.placeInput.consumePress())
        {
            freshPlace = true;
            edits.placeProjectionBlock(true);
            this.nextPlaceTick = tick + PLACE_INTERVAL_TICKS;

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
        }

        if (!freshPlace && this.placeInput.isHeld() && tick >= this.nextPlaceTick)
        {
            edits.placeProjectionBlock(false);
            this.nextPlaceTick = tick + PLACE_INTERVAL_TICKS;

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
            }
        }
    }

    public void resetTransientState()
    {
        Minecraft mc = Minecraft.getInstance();
        this.suspend(mc.options.keyUse.isDown(), mc.options.keyAttack.isDown());
        this.nextPlaceTick = 0L;
    }

    private void suspend(boolean placeDown, boolean breakDown)
    {
        this.placeInput.suspend(placeDown);
        this.breakInput.suspend(breakDown);
    }

    private static final class InputLatch
    {
        private boolean down;
        private boolean armed;
        private boolean blockedUntilRelease;
        private int pendingPresses;

        private void update(boolean pressed, boolean acceptsCreatorEdits)
        {
            if (!pressed)
            {
                this.down = false;
                this.armed = false;
                this.blockedUntilRelease = false;
                return;
            }

            if (this.down)
            {
                return;
            }

            this.down = true;

            if (acceptsCreatorEdits && !this.blockedUntilRelease)
            {
                ++this.pendingPresses;
                this.armed = true;
            }
            else
            {
                this.armed = false;
                this.blockedUntilRelease = true;
            }
        }

        private void sync(boolean currentlyDown, boolean acceptsCreatorEdits)
        {
            if (currentlyDown != this.down)
            {
                this.update(currentlyDown, acceptsCreatorEdits);
            }
        }

        private boolean consumePress()
        {
            if (this.pendingPresses <= 0)
            {
                return false;
            }

            --this.pendingPresses;
            return true;
        }

        private boolean isHeld()
        {
            return this.down && this.armed && !this.blockedUntilRelease;
        }

        private void suspend(boolean currentlyDown)
        {
            this.down = currentlyDown;
            this.armed = false;
            this.blockedUntilRelease = currentlyDown;
            this.pendingPresses = 0;
        }
    }
}
