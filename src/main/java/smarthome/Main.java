package smarthome;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

/** The only application composition root: constructs and connects all collaborating objects. */
public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int eventsPerProducer = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        if (args.length > 2 || threads < 3 || eventsPerProducer < 1)
            throw new IllegalArgumentException("Usage: Main [threads >= 3] [eventsPerProducer >= 1]");
        var errors = new ConcurrentLinkedQueue<RuntimeException>();
        var power = new PowerUseVisitor();
        var mobile = new MobileApp();
        var audit = new AuditLog();
        var reports = new ThreadSafeSubject<HomeEvent>(errors::add);
        var energy = new EnergyTracker(power, errors::add);
        var alarm = new SecurityAlarm(reports, SecurityAuditVisitor::new);
        var rules = new AutomationRules(reports);
        var living = new DeviceSource("living-source", new Room("living", "Living room", List.of(
                new Lamp("lamp", "Ceiling lamp", false, 10),
                new Thermostat("heat", "Heating", false, 1000, 22))), errors::add);
        var entrance = new DeviceSource("entry-source", new Room("entry", "Entrance", List.of(
                new Camera("camera", "Entry camera", true, 8, true),
                new SmartLock("lock", "Front lock", true, 2))), errors::add);
        var bedroom = new DeviceSource("bed-source", new Room("bed", "Bedroom", List.of(
                new Lamp("bed-lamp", "Bedside lamp", false, 6),
                new SmokeDetector("smoke", "Bedroom smoke detector", false, 1))), errors::add);
        for (DeviceSource source : List.of(living, entrance, bedroom)) {
            for (Observer<HomeEvent> listener : List.<Observer<HomeEvent>>of(mobile, energy, alarm, rules, audit))
                source.subscribe(EventType.DEVICE_STATE, listener);
        }
        energy.subscribe(EventType.ENERGY_SUMMARY, mobile);
        energy.subscribe(EventType.ENERGY_SUMMARY, audit);
        for (EventType type : List.of(EventType.SECURITY_REPORT, EventType.AUTOMATION_COMMAND)) {
            reports.subscribe(type, mobile); reports.subscribe(type, audit);
        }
        var runner = new ProducerRunner(Executors.newFixedThreadPool(threads), Duration.ofSeconds(30));
        runner.run(List.of(
            () -> { for (int i = 0; i < eventsPerProducer; i++) living.change(new Thermostat("heat", "Heating", true, 1000, i % 2 == 0 ? 22 : 28)); },
            () -> { for (int i = 0; i < eventsPerProducer; i++) entrance.change(new SmartLock("lock", "Front lock", i % 2 == 0, 2)); },
            () -> { for (int i = 0; i < eventsPerProducer; i++) bedroom.change(new Lamp("bed-lamp", "Bedside lamp", i % 2 != 0, 6)); }
        ));
        var home = new Home("home", "Smart home", List.of(living.snapshot(), entrance.snapshot(), bedroom.snapshot()));
        System.out.println("SMART HOME | Assignment 6 | Topic 7");
        System.out.println("Producer threads: " + threads + ", producers: 3");
        for (EventType type : EventType.values()) System.out.println(type + " received: " + mobile.count(type));
        System.out.println("Audit log entries: " + audit.snapshot().size());
        System.out.println("Final power: " + home.accept(power) + " W");
        System.out.println("Security findings: " + home.accept(new SecurityAuditVisitor()));
        System.out.println("Devices rated above 100 W: " + home.accept(new DeviceFilterVisitor(d -> d.watts() > 100)).size());
        System.out.println("Inventory: " + new java.util.TreeMap<>(home.accept(new InventoryVisitor())));
        System.out.println("JSON: " + home.accept(new JsonExportVisitor()));
        System.out.println("Listener failures: " + errors.size());
        if (!errors.isEmpty()) throw new IllegalStateException("Listener failed", errors.peek());
    }
}
