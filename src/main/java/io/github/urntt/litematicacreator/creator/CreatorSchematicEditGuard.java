package io.github.urntt.litematicacreator.creator;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CreatorSchematicEditGuard
{
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
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

    public static void runRebuild(Runnable rebuild)
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
