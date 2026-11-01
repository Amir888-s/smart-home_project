package smarthome;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class ObserverTest {
    @Test void onlySubscribedTypeIsDelivered() {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        var count = new AtomicInteger();
        subject.subscribe(EventType.DEVICE_STATE, e -> count.incrementAndGet());
        subject.publish(new SecurityReport("r", 1, List.of()));
        subject.publish(Fixtures.event(1));
        assertEquals(1, count.get());
    }
    @Test void duplicateRegistrationDoesNotDuplicateDelivery() {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        var count = new AtomicInteger(); Observer<HomeEvent> observer = e -> count.incrementAndGet();
        subject.subscribe(EventType.DEVICE_STATE, observer); subject.subscribe(EventType.DEVICE_STATE, observer);
        subject.publish(Fixtures.event(1)); assertEquals(1, count.get());
    }
    @Test void externalUnsubscribeStopsFutureDelivery() {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        var count = new AtomicInteger(); Observer<HomeEvent> observer = e -> count.incrementAndGet();
        subject.subscribe(EventType.DEVICE_STATE, observer); subject.unsubscribe(EventType.DEVICE_STATE, observer);
        subject.publish(Fixtures.event(1)); assertEquals(0, count.get());
    }
    @Test void listenerCanUnsubscribeInsideHandler() {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        var count = new AtomicInteger(); var self = new AtomicReference<Observer<HomeEvent>>();
        self.set(e -> { count.incrementAndGet(); subject.unsubscribe(EventType.DEVICE_STATE, self.get()); });
        subject.subscribe(EventType.DEVICE_STATE, self.get());
        subject.publish(Fixtures.event(1)); subject.publish(Fixtures.event(2)); assertEquals(1, count.get());
    }
    @Test void subscriptionInsideHandlerStartsWithNextSnapshot() {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        var count = new AtomicInteger(); Observer<HomeEvent> late = e -> count.incrementAndGet();
        subject.subscribe(EventType.DEVICE_STATE, e -> subject.subscribe(EventType.DEVICE_STATE, late));
        subject.publish(Fixtures.event(1)); assertEquals(0, count.get());
        subject.publish(Fixtures.event(2)); assertEquals(1, count.get());
    }
    @Test void failingListenerIsRecordedAndOthersStillReceive() {
        var failures = new ArrayList<RuntimeException>(); var subject = new ThreadSafeSubject<HomeEvent>(failures::add);
        var count = new AtomicInteger();
        subject.subscribe(EventType.DEVICE_STATE, e -> { throw new IllegalStateException("broken listener"); });
        subject.subscribe(EventType.DEVICE_STATE, e -> count.incrementAndGet());
        subject.publish(Fixtures.event(1)); assertEquals(1, failures.size()); assertEquals(1, count.get());
    }
    @Test void nullSubscriberIsRejected() {
        var subject = new ThreadSafeSubject<HomeEvent>(failure -> fail(failure));
        assertThrows(NullPointerException.class, () -> subject.subscribe(EventType.DEVICE_STATE, null));
    }
    @Test void eventSnapshotDefensivelyCopiesItsCollections() {
        var devices = new ArrayList<Device>(); devices.add(new Lamp("l", "Lamp", true, 10));
        var room = new Room("r", "Room", devices); devices.clear();
        assertEquals(1, room.devices().size());
        assertThrows(UnsupportedOperationException.class, () -> room.devices().clear());
        var findings = new ArrayList<String>(); findings.add("warning");
        var report = new SecurityReport("r", 1, findings); findings.clear(); assertEquals(List.of("warning"), report.findings());
    }
    @Test void twoSourcesShareOneListener() {
        var app = new MobileApp();
        var first = new DeviceSource("one", Fixtures.home().rooms().get(0), failure -> fail(failure));
        var second = new DeviceSource("two", Fixtures.home().rooms().get(1), failure -> fail(failure));
        first.subscribe(EventType.DEVICE_STATE, app); second.subscribe(EventType.DEVICE_STATE, app);
        first.change(new Lamp("l", "Lamp", false, 10)); second.change(new Lamp("b", "Bed lamp", true, 6));
        assertEquals(2, app.count(EventType.DEVICE_STATE));
    }
}
