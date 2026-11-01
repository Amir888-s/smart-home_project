package smarthome;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

/** Owns the injected executor's lifetime, draining accepted finite jobs before returning. */
public final class ProducerRunner {
    private final ExecutorService executor;
    private final Duration timeout;
    public ProducerRunner(ExecutorService executor, Duration timeout) {
        this.executor = Objects.requireNonNull(executor);
        this.timeout = Objects.requireNonNull(timeout);
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("Positive timeout required");
    }
    public void run(List<Runnable> jobs) throws InterruptedException, ExecutionException {
        try {
            List<Future<?>> futures = jobs.stream().map(executor::submit).toList();
            executor.shutdown();
            if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS))
                    throw new IllegalStateException("Producer ignored interruption");
                throw new IllegalStateException("Producer timeout: processing was cancelled");
            }
            for (Future<?> future : futures) future.get(); // Surface producer failures to the caller.
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw interrupted;
        } finally {
            if (!executor.isShutdown()) executor.shutdownNow();
        }
    }
}
