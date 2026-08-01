package io.github.urntt.litematicacreator.recovery;

public final class RecoveryWriteScheduler
{
    public static final long IDLE_DELAY_TICKS = 5L * 20L;
    public static final long MAX_DELAY_TICKS = 30L * 20L;

    private long firstPendingTick = -1L;
    private long lastChangeTick = -1L;

    public void markChanged(long tick)
    {
        if (this.firstPendingTick < 0L)
        {
            this.firstPendingTick = tick;
        }

        this.lastChangeTick = tick;
    }

    public boolean hasPendingChanges()
    {
        return this.firstPendingTick >= 0L;
    }

    public boolean shouldWrite(long tick)
    {
        return this.hasPendingChanges() &&
               (tick - this.lastChangeTick >= IDLE_DELAY_TICKS || tick - this.firstPendingTick >= MAX_DELAY_TICKS);
    }

    public void markSubmitted()
    {
        this.firstPendingTick = -1L;
        this.lastChangeTick = -1L;
    }
}
