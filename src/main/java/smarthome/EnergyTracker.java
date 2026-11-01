package smarthome;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Observer -> subject chain. CAS protects an immutable map; no whole-subject lock. */
public final class EnergyTracker extends ThreadSafeSubject<HomeEvent> implements Observer<HomeEvent> {
    private record Reading(long version, double watts) { }
    private final AtomicReference<Map<String, Reading>> readings;
    private final PowerUseVisitor powerVisitor;
    public EnergyTracker(PowerUseVisitor powerVisitor, Consumer<RuntimeException> errorSink) {
        super(errorSink);
        this.powerVisitor = java.util.Objects.requireNonNull(powerVisitor);
        readings = new AtomicReference<>(Map.of());
    }
    @Override public void onEvent(HomeEvent event) {
        if (!(event instanceof DeviceStateChanged changed)) return;
        double watts = changed.room().accept(powerVisitor);
        Map<String, Reading> snapshot = readings.updateAndGet(previous -> {
            Reading last = previous.get(changed.sourceId());
            if (last != null && last.version() >= changed.version()) return previous;
            var updated = new HashMap<>(previous);
            updated.put(changed.sourceId(), new Reading(changed.version(), watts));
            return Map.copyOf(updated);
        });
        var values = new HashMap<String, Double>();
        snapshot.forEach((source, reading) -> values.put(source, reading.watts()));
        publish(new EnergySummary(values, values.values().stream().mapToDouble(Double::doubleValue).sum()));
    }
}
