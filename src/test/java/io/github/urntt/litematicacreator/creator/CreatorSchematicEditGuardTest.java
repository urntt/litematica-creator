package io.github.urntt.litematicacreator.creator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorSchematicEditGuardTest
{
    @Test
    void rebuildsForDifferentChunksCanRunConcurrently() throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try
        {
            Future<?> first = executor.submit(() -> runHeldRebuild(1L, entered, release));
            Future<?> second = executor.submit(() -> runHeldRebuild(2L, entered, release));

            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertEquals(2, CreatorSchematicEditGuard.activeRebuildCount());
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        }
        finally
        {
            release.countDown();
            executor.shutdownNow();
        }

        assertEquals(0, CreatorSchematicEditGuard.activeRebuildCount());
    }

    @Test
    void rebuildsForSameChunkAreSerialized() throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch firstRelease = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);

        try
        {
            Future<?> first = executor.submit(() -> runHeldRebuild(11L, firstEntered, firstRelease));
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> CreatorSchematicEditGuard.runRebuild(11L, secondEntered::countDown));

            assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS));
            firstRelease.countDown();
            assertTrue(secondEntered.await(2, TimeUnit.SECONDS));
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        }
        finally
        {
            firstRelease.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void renderCompileWaitsForSameChunkRebuild() throws Exception
    {
        assertSameChunkOperationsAreExclusive(true);
    }

    @Test
    void rebuildWaitsForSameChunkRenderCompile() throws Exception
    {
        assertSameChunkOperationsAreExclusive(false);
    }

    @Test
    void renderAndRebuildForDifferentChunksCanRunConcurrently() throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try
        {
            Future<?> rebuild = executor.submit(() -> runHeldRebuild(21L, entered, release));
            Future<?> render = executor.submit(() -> runHeldRender(22L, entered, release));

            assertTrue(entered.await(2, TimeUnit.SECONDS));
            release.countDown();
            rebuild.get(2, TimeUnit.SECONDS);
            render.get(2, TimeUnit.SECONDS);
        }
        finally
        {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void editWaitsForExistingRebuildReader() throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readerEntered = new CountDownLatch(1);
        CountDownLatch readerRelease = new CountDownLatch(1);
        CountDownLatch editAttempting = new CountDownLatch(1);
        CountDownLatch editEntered = new CountDownLatch(1);

        try
        {
            Future<?> reader = executor.submit(() -> runHeldRebuild(31L, readerEntered, readerRelease));
            assertTrue(readerEntered.await(2, TimeUnit.SECONDS));

            Future<?> editor = executor.submit(() -> {
                editAttempting.countDown();

                try (CreatorSchematicEditGuard.EditTransaction ignored = CreatorSchematicEditGuard.beginEdit())
                {
                    editEntered.countDown();
                }
            });

            assertTrue(editAttempting.await(2, TimeUnit.SECONDS));
            assertFalse(editEntered.await(100, TimeUnit.MILLISECONDS));
            readerRelease.countDown();
            assertTrue(editEntered.await(2, TimeUnit.SECONDS));
            reader.get(2, TimeUnit.SECONDS);
            editor.get(2, TimeUnit.SECONDS);
        }
        finally
        {
            readerRelease.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void rebuildWaitsUntilEditPublishesFinalState() throws Exception
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch rebuildAttempting = new CountDownLatch(1);
        CountDownLatch rebuildEntered = new CountDownLatch(1);
        AtomicInteger state = new AtomicInteger(1);

        try
        {
            Future<Integer> observed;

            try (CreatorSchematicEditGuard.EditTransaction ignored = CreatorSchematicEditGuard.beginEdit())
            {
                observed = executor.submit(() -> {
                    AtomicInteger result = new AtomicInteger();
                    rebuildAttempting.countDown();
                    CreatorSchematicEditGuard.runRebuild(41L, () -> {
                        rebuildEntered.countDown();
                        result.set(state.get());
                    });
                    return result.get();
                });

                assertTrue(rebuildAttempting.await(2, TimeUnit.SECONDS));
                assertFalse(rebuildEntered.await(100, TimeUnit.MILLISECONDS));
                state.set(2);
            }

            assertTrue(rebuildEntered.await(2, TimeUnit.SECONDS));
            assertEquals(2, observed.get(2, TimeUnit.SECONDS));
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    void exceptionsReleaseBothKindsOfLock()
    {
        assertThrows(IllegalStateException.class, () -> CreatorSchematicEditGuard.runRebuild(51L, () -> {
            throw new IllegalStateException("rebuild failed");
        }));
        assertEquals(0, CreatorSchematicEditGuard.activeRebuildCount());

        assertThrows(IllegalStateException.class, () -> CreatorSchematicEditGuard.runRenderCompile(51L, () -> {
            throw new IllegalStateException("render failed");
        }));

        CreatorSchematicEditGuard.runRebuild(51L, () -> { });

        assertThrows(IllegalStateException.class, () -> {
            try (CreatorSchematicEditGuard.EditTransaction ignored = CreatorSchematicEditGuard.beginEdit())
            {
                throw new IllegalStateException("edit failed");
            }
        });
        assertFalse(CreatorSchematicEditGuard.isEditActive());
    }

    private static void assertSameChunkOperationsAreExclusive(boolean rebuildFirst) throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch firstRelease = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);

        try
        {
            Future<?> first = executor.submit(() -> {
                if (rebuildFirst)
                {
                    runHeldRebuild(61L, firstEntered, firstRelease);
                }
                else
                {
                    runHeldRender(61L, firstEntered, firstRelease);
                }
            });
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> {
                if (rebuildFirst)
                {
                    CreatorSchematicEditGuard.runRenderCompile(61L, secondEntered::countDown);
                }
                else
                {
                    CreatorSchematicEditGuard.runRebuild(61L, secondEntered::countDown);
                }
            });

            assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS));
            firstRelease.countDown();
            assertTrue(secondEntered.await(2, TimeUnit.SECONDS));
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        }
        finally
        {
            firstRelease.countDown();
            executor.shutdownNow();
        }
    }

    private static void runHeldRebuild(long chunkKey, CountDownLatch entered, CountDownLatch release)
    {
        CreatorSchematicEditGuard.runRebuild(chunkKey, () -> {
            awaitRelease(entered, release);
        });
    }

    private static void runHeldRender(long chunkKey, CountDownLatch entered, CountDownLatch release)
    {
        CreatorSchematicEditGuard.runRenderCompile(chunkKey, () -> {
            awaitRelease(entered, release);
        });
    }

    private static void awaitRelease(CountDownLatch entered, CountDownLatch release)
    {
        entered.countDown();

        try
        {
            release.await();
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
