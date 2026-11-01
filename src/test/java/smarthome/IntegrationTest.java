package smarthome;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class IntegrationTest {
    @Test void eventAuditsOnlyRelatedRoomAndRepublishesReport() {
        var reports = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure)); var log = new AuditLog();
        reports.subscribe(EventType.SECURITY_REPORT, log);
        var alarm = new SecurityAlarm(reports, SecurityAuditVisitor::new);
        var source = new DeviceSource("living-source", Fixtures.home().rooms().get(0), failure -> fail(failure));
        source.subscribe(EventType.DEVICE_STATE, alarm); source.change(new Lamp("l", "Lamp", false, 10));
        assertEquals(1, log.snapshot().size());
        var report = (SecurityReport) log.snapshot().get(0);
        assertEquals("living", report.roomId()); assertEquals(1, report.sourceVersion());
        assertEquals(List.of("c: camera is not recording", "k: door is unlocked"), report.findings());
        assertFalse(report.findings().stream().anyMatch(text -> text.startsWith("bc:")));
    }
    @Test void energyObserverRepublishesAggregatedData() {
        var tracker = new EnergyTracker(new PowerUseVisitor(), failure -> fail(failure));
        var result = new AtomicReference<EnergySummary>();
        tracker.subscribe(EventType.ENERGY_SUMMARY, e -> result.set((EnergySummary)e));
        tracker.onEvent(Fixtures.event(1));
        tracker.onEvent(new DeviceStateChanged("bed-source", 1, Fixtures.home().rooms().get(1), "b"));
        assertEquals(1021.0, result.get().totalWatts(), 0.0001);
        assertEquals(Map.of("living-source", 1020.0, "bed-source", 1.0), result.get().wattsBySource());
    }
    @Test void oldVersionsCannotReplaceNewerEnergyReading() {
        var tracker = new EnergyTracker(new PowerUseVisitor(), failure -> fail(failure)); var result = new AtomicReference<EnergySummary>();
        tracker.subscribe(EventType.ENERGY_SUMMARY, e -> result.set((EnergySummary)e));
        tracker.onEvent(Fixtures.event(2));
        tracker.onEvent(new DeviceStateChanged("living-source", 1, new Room("living", "Living", List.of(new Lamp("l", "Lamp", true, 1))), "l"));
        assertEquals(1020.0, result.get().totalWatts(), 0.0001);
    }
    @Test void hotThermostatProducesOneRecommendation() {
        var commands = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure)); var log = new AuditLog();
        commands.subscribe(EventType.AUTOMATION_COMMAND, log);
        new AutomationRules(commands).onEvent(new DeviceStateChanged("s", 1, new Room("r", "Room", List.of(new Thermostat("t", "Heat", true, 1000, 28))), "t"));
        assertEquals(List.of(new AutomationCommand("t", "Recommend target temperature of 22 C")), log.snapshot());
    }
    @Test void normalThermostatDoesNotProduceCommand() {
        var commands = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure)); var log = new AuditLog();
        commands.subscribe(EventType.AUTOMATION_COMMAND, log); new AutomationRules(commands).onEvent(Fixtures.event(1));
        assertTrue(log.snapshot().isEmpty());
    }
    @Test void sourceVersionIncrementsAndPreviousSnapshotStaysUnchanged() {
        var source = new DeviceSource("s", Fixtures.home().rooms().get(0), failure -> fail(failure));
        var first = source.change(new Lamp("l", "Lamp", false, 10)); var second = source.change(new Lamp("l", "Lamp", true, 10));
        assertEquals(1, first.version()); assertEquals(2, second.version());
        assertFalse(((Lamp)first.room().devices().get(0)).on()); assertTrue(((Lamp)second.room().devices().get(0)).on());
    }
    @Test void sourceRejectsDeviceOutsideItsRoom() {
        var source = new DeviceSource("s", Fixtures.home().rooms().get(0), failure -> fail(failure));
        assertThrows(IllegalArgumentException.class, () -> source.change(new Lamp("missing", "Unknown", true, 1)));
    }
}
