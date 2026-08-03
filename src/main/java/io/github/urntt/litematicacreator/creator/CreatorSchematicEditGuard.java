package io.github.urntt.litematicacreator.creator;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CreatorSchematicEditGuard
{
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private static final int REBUILD_LOCK_STRIPES = 256;
    private static final ReentrantLock[] REBUILD_LOCKS = createRebuildLocks();
    private static final ThreadLocal<Integer> PLACEMENT_REFRESH_SUPPRESSION_DEPTH =
            ThreadLocal.withInitial(() -> 0);
    private static final AtomicLong NEXT_TRANSACTION_ID = new AtomicLong();

    private CreatorSchematicEditGuard()
    {
    }

    public static EditTransaction beginEdit()
    {
        long transactionId = NEXT_TRANSACTION_ID.incrementAndGet();
        long start = System.nanoTime();
        Lock writeLock = LOCK.writeLock();
        writeLock.lock();
        return new EditTransaction(transactionId, System.nanoTime() - start, writeLock);
    }

    public static void runRebuild(long chunkKey, Runnable rebuild)
    {
        Lock chunkLock = REBUILD_LOCKS[Math.floorMod(Long.hashCode(chunkKey), REBUILD_LOCK_STRIPES)];
        chunkLock.lock();

        try
        {
            Lock readLock = LOCK.readLock();
            readLock.lock();

            try
            {
                rebuild.run();
            }
            finally
            {
                readLock.unlock();
            }
        }
        finally
        {
            chunkLock.unlock();
        }
    }

    public static PlacementRefreshSuppression suppressPlacementRefreshScheduling()
    {
        PLACEMENT_REFRESH_SUPPRESSION_DEPTH.set(PLACEMENT_REFRESH_SUPPRESSION_DEPTH.get() + 1);
        return new PlacementRefreshSuppression();
    }

    public static boolean isPlacementRefreshSchedulingSuppressed()
    {
        return PLACEMENT_REFRESH_SUPPRESSION_DEPTH.get() > 0;
    }

    static int activeRebuildCount()
    {
        return LOCK.getReadLockCount();
    }

    static boolean isEditActive()
    {
        return LOCK.isWriteLocked();
    }

    private static ReentrantLock[] createRebuildLocks()
    {
        ReentrantLock[] locks = new ReentrantLock[REBUILD_LOCK_STRIPES];

        for (int index = 0; index < locks.length; index++)
        {
            locks[index] = new ReentrantLock(true);
        }

        return locks;
    }

    public static final class PlacementRefreshSuppression implements AutoCloseable
    {
        private boolean closed;

        private PlacementRefreshSuppression()
        {
        }

        @Override
        public void close()
        {
            if (!this.closed)
            {
                this.closed = true;
                int depth = PLACEMENT_REFRESH_SUPPRESSION_DEPTH.get() - 1;

                if (depth <= 0)
                {
                    PLACEMENT_REFRESH_SUPPRESSION_DEPTH.remove();
                }
                else
                {
                    PLACEMENT_REFRESH_SUPPRESSION_DEPTH.set(depth);
                }
            }
        }
    }

    public static final class EditTransaction implements AutoCloseable
    {
        private final long transactionId;
        private final long waitNanos;
        private final Lock writeLock;
        private boolean closed;

        private EditTransaction(long transactionId, long waitNanos, Lock writeLock)
        {
            this.transactionId = transactionId;
            this.waitNanos = waitNanos;
            this.writeLock = writeLock;
        }

        public long transactionId()
        {
            return this.transactionId;
        }

        public long waitNanos()
        {
            return this.waitNanos;
        }

        @Override
        public void close()
        {
            if (!this.closed)
            {
                this.closed = true;
                this.writeLock.unlock();
            }
        }
    }
}
