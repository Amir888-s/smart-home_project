package smarthome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
@Timeout(15)
class ConcurrencyTest {
    @Test void stressFourThreadsTimesOneThousandEventsEqualsFourThousand() throws Exception {
        var failures = new ConcurrentLinkedQueue<RuntimeException>();
        var subject = new ThreadSafeSubject<HomeEvent>(failures::add); var count = new AtomicInteger();
        subject.subscribe(EventType.DEVICE_STATE, e -> count.incrementAndGet());
        var start = new CyclicBarrier(4); var tasks = new ArrayList<Runnable>();
        for (int thread = 0; thread < 4; thread++) tasks.add(() -> {
            await(start); for (int i = 1; i <= 1000; i++) subject.publish(Fixtures.event(i));
        });
        var pool = Executors.newFixedThreadPool(4); new ProducerRunner(pool, Duration.ofSeconds(10)).run(tasks);
        assertEquals(4000, count.get()); assertTrue(failures.isEmpty()); assertTrue(pool.isTerminated());
    }
    @Test void removingSubscriberDoesNotCancelAlreadyCapturedSnapshot() throws Exception {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var count = new AtomicInteger();
        Observer<HomeEvent> removed = e -> count.incrementAndGet();
        subject.subscribe(EventType.DEVICE_STATE, e -> { entered.countDown(); await(release); });
        subject.subscribe(EventType.DEVICE_STATE, removed);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var inFlight = pool.submit(() -> subject.publish(Fixtures.event(1)));
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            subject.unsubscribe(EventType.DEVICE_STATE, removed); release.countDown(); inFlight.get(3, TimeUnit.SECONDS);
            subject.publish(Fixtures.event(2)); assertEquals(1, count.get());
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS)); }
    }
    @Test void concurrentObserverChainHasExactCountAndLatestTotals() throws Exception {
        var failures = new ConcurrentLinkedQueue<RuntimeException>(); var tracker = new EnergyTracker(new PowerUseVisitor(), failures::add);
        var log = new AuditLog(); tracker.subscribe(EventType.ENERGY_SUMMARY, log);
        var barrier = new CyclicBarrier(4); var tasks = new ArrayList<Runnable>();
        for (int thread = 0; thread < 4; thread++) {
            String source = "source-" + thread;
            tasks.add(() -> { await(barrier); for (int i = 1; i <= 1000; i++) tracker.onEvent(new DeviceStateChanged(source, i,
                new Room(source, "Room", List.of(new Lamp("l", "Lamp", true, 10))), "l")); });
        }
        new ProducerRunner(Executors.newFixedThreadPool(4), Duration.ofSeconds(10)).run(tasks);
        assertEquals(4000, log.snapshot().size()); assertTrue(failures.isEmpty());
        // Ask for a final summary AFTER producers stop; callback order is not global version order.
        tracker.onEvent(new DeviceStateChanged("source-0", 1, new Room("source-0", "Room", List.of(new Lamp("l", "Lamp", true, 1))), "l"));
        var events = log.snapshot(); assertEquals(40.0, ((EnergySummary)events.get(events.size()-1)).totalWatts(), 0.0001);
    }
    @Test void gracefulShutdownDrainsEveryAcceptedTask() throws Exception {
        var pool = Executors.newFixedThreadPool(3); var count = new AtomicInteger();
        new ProducerRunner(pool, Duration.ofSeconds(10)).run(List.of(
            () -> { for (int i=0;i<1000;i++) count.incrementAndGet(); },
            () -> { for (int i=0;i<1000;i++) count.incrementAndGet(); },
            () -> { for (int i=0;i<1000;i++) count.incrementAndGet(); }));
        assertEquals(3000, count.get()); assertTrue(pool.isTerminated());
    }
    @Test void producerFailureIsSurfacedAndExecutorTerminates() {
        var pool = Executors.newFixedThreadPool(3);
        var runner = new ProducerRunner(pool, Duration.ofSeconds(5));
        assertThrows(ExecutionException.class, () -> runner.run(List.of(() -> { throw new IllegalStateException("producer failure"); })));
        assertTrue(pool.isTerminated());
    }
    private static void await(CyclicBarrier barrier) {
        try { barrier.await(5, TimeUnit.SECONDS); } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Latch timeout"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
    }
}
