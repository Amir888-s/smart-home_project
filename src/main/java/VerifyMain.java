import smarthome.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Standalone verification with JDK 17 only; separate from the JUnit suite. */
public final class VerifyMain {
    private static int checks;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++; System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        var failures = new ConcurrentLinkedQueue<RuntimeException>();
        var room = new Room("r", "Room", List.of(new Lamp("l", "Lamp", true, 10),
            new Thermostat("t", "Heat", true, 1000, 28), new Camera("c", "Cam", true, 8, false),
            new SmartLock("k", "Lock", false, 2), new SmokeDetector("s", "Smoke", false, 1)));
        var home = new Home("h", "Home", List.of(room));
        check(home.accept(new PowerUseVisitor()) == 1021.0, "hand calculated power 1021 W");
        check(home.accept(new SecurityAuditVisitor()).equals(List.of("c: camera is not recording", "k: door is unlocked")), "whole tree audit");
        check(home.accept(new DeviceFilterVisitor(d -> d.watts() > 100)).size() == 1, "high power filter");
        check(home.accept(new InventoryVisitor()).equals(Map.of("Lamp",1,"Thermostat",1,"Camera",1,"SmartLock",1,"SmokeDetector",1)), "inventory across five leaf types");
        check(home.accept(new JsonExportVisitor()).contains("\"rooms\":[{\"type\":\"Room\""), "nested JSON");
        var subject = new ThreadSafeSubject<HomeEvent>(failures::add); var count = new AtomicInteger();
        var self = new AtomicReference<smarthome.Observer<HomeEvent>>();
        self.set(e -> { count.incrementAndGet(); subject.unsubscribe(EventType.DEVICE_STATE,self.get()); });
        subject.subscribe(EventType.DEVICE_STATE,self.get());
        var event = new DeviceStateChanged("source",1,room,"l"); subject.publish(event); subject.publish(event);
        check(count.get()==1, "unsubscribe inside handler");
        var reports = new ThreadSafeSubject<HomeEvent>(failures::add); var log = new AuditLog();
        reports.subscribe(EventType.SECURITY_REPORT,log);
        new SecurityAlarm(reports,SecurityAuditVisitor::new).onEvent(event);
        check(log.snapshot().equals(List.of(new SecurityReport("r",1,List.of("c: camera is not recording","k: door is unlocked")))), "room event to audit report");
        var energy = new EnergyTracker(new PowerUseVisitor(),failures::add); var summary = new AtomicReference<EnergySummary>();
        energy.subscribe(EventType.ENERGY_SUMMARY,e -> summary.set((EnergySummary)e)); energy.onEvent(event);
        check(summary.get().totalWatts()==1021.0, "observer to subject energy chain");
        var stress = new ThreadSafeSubject<HomeEvent>(failures::add); var received = new AtomicInteger();
        stress.subscribe(EventType.DEVICE_STATE,e -> received.incrementAndGet());
        var barrier = new CyclicBarrier(4); var tasks = new ArrayList<Runnable>();
        for(int thread=0;thread<4;thread++) tasks.add(() -> {
            try { barrier.await(5,TimeUnit.SECONDS); } catch(Exception ex) { throw new IllegalStateException(ex); }
            for(int i=0;i<1000;i++) stress.publish(event);
        });
        var pool=Executors.newFixedThreadPool(4); new ProducerRunner(pool,Duration.ofSeconds(10)).run(tasks);
        check(received.get()==4000,"4 threads x 1000 events gives exactly 4000 deliveries");
        check(pool.isTerminated(),"graceful shutdown leaves no executor threads");
        check(failures.isEmpty(),"zero listener failures");
        System.out.println("Verified " + checks + " checks with JDK 17 only");
    }
}
