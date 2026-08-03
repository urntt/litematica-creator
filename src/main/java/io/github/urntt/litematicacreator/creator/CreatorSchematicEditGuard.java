package io.github.urntt.litematicacreator.creator;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CreatorSchematicEditGuard
{
    private static final ReentrantReadWriteLock EDIT_LOCK = new ReentrantReadWriteLock(true);
    private static final int CHUNK_LOCK_STRIPES = 256;
    private static final ReentrantReadWriteLock[] CHUNK_LOCKS = createChunkLocks();
    private static final AtomicLong NEXT_TRANSACTION_ID = new AtomicLong();

    private CreatorSchematicEditGuard()
    {
    }

    public static EditTransaction beginEdit()
    {
        long transactionId = NEXT_TRANSACTION_ID.incrementAndGet();
        long start = System.nanoTime();
        Lock writeLock = EDIT_LOCK.writeLock();
        writeLock.lock();
        return new EditTransaction(transactionId, System.nanoTime() - start, writeLock);
    }

    public static void runRebuild(long chunkKey, Runnable rebuild)
    {
        Lock chunkWriteLock = chunkLock(chunkKey).writeLock();
        chunkWriteLock.lock();

        try
        {
            Lock editReadLock = EDIT_LOCK.readLock();
            editReadLock.lock();

            try
            {
                rebuild.run();
            }
            finally
            {
                editReadLock.unlock();
            }
        }
        finally
        {
            chunkWriteLock.unlock();
        }
    }

    public static void runRenderCompile(long chunkKey, Runnable renderCompile)
    {
        Lock chunkReadLock = chunkLock(chunkKey).readLock();
        chunkReadLock.lock();

        try
        {
            renderCompile.run();
        }
        finally
        {
            chunkReadLock.unlock();
        }
    }

    static int activeRebuildCount()
    {
        return EDIT_LOCK.getReadLockCount();
    }

    static boolean isEditActive()
    {
        return EDIT_LOCK.isWriteLocked();
    }

    private static ReentrantReadWriteLock chunkLock(long chunkKey)
    {
        return CHUNK_LOCKS[Math.floorMod(Long.hashCode(chunkKey), CHUNK_LOCK_STRIPES)];
    }

    private static ReentrantReadWriteLock[] createChunkLocks()
    {
        ReentrantReadWriteLock[] locks = new ReentrantReadWriteLock[CHUNK_LOCK_STRIPES];

        for (int index = 0; index < locks.length; index++)
        {
            locks[index] = new ReentrantReadWriteLock(true);
        }

        return locks;
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
