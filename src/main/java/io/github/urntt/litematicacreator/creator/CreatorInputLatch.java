package io.github.urntt.litematicacreator.creator;

final class CreatorInputLatch
{
    private boolean down;
    private boolean armed;
    private boolean blockedUntilRelease;
    private int pendingPresses;

    void update(boolean pressed, boolean acceptsCreatorEdits)
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

    void sync(boolean currentlyDown, boolean acceptsCreatorEdits)
    {
        if (currentlyDown != this.down)
        {
            this.update(currentlyDown, acceptsCreatorEdits);
        }
    }

    boolean consumePress()
    {
        if (this.pendingPresses <= 0)
        {
            return false;
        }

        --this.pendingPresses;
        return true;
    }

    boolean isHeld()
    {
        return this.down && this.armed && !this.blockedUntilRelease;
    }

    void suspend(boolean currentlyDown)
    {
        this.down = currentlyDown;
        this.armed = false;
        this.blockedUntilRelease = currentlyDown;
        this.pendingPresses = 0;
    }
}
