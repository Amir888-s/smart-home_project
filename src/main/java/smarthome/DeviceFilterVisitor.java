package smarthome;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Returns only leaves matching a caller-supplied rule. */
public final class DeviceFilterVisitor implements Visitor<List<Element>> {
    private final Predicate<Device> predicate;
    public DeviceFilterVisitor(Predicate<Device> predicate) { this.predicate = Objects.requireNonNull(predicate); }
    @Override public List<Element> visit(Home home) {
        return home.rooms().stream().flatMap(r -> r.accept(this).stream()).toList();
    }
    @Override public List<Element> visit(Room room) {
        return room.devices().stream().flatMap(d -> d.accept(this).stream()).toList();
    }
    private List<Element> match(Device device) { return predicate.test(device) ? List.of(device) : List.of(); }
    @Override public List<Element> visit(Lamp lamp) { return match(lamp); }
    @Override public List<Element> visit(Thermostat thermostat) { return match(thermostat); }
    @Override public List<Element> visit(Camera camera) { return match(camera); }
    @Override public List<Element> visit(SmartLock lock) { return match(lock); }
    @Override public List<Element> visit(SmokeDetector detector) { return match(detector); }
}
