package smarthome;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** One source owns one room; changing a device publishes a complete immutable room snapshot. */
public final class DeviceSource extends ThreadSafeSubject<HomeEvent> {
    private record VersionedRoom(long version, Room room) { }
    private final String sourceId;
    private final AtomicReference<VersionedRoom> current;
    public DeviceSource(String sourceId, Room initialRoom, Consumer<RuntimeException> errorSink) {
        super(errorSink);
        this.sourceId = Objects.requireNonNull(sourceId);
        current = new AtomicReference<>(new VersionedRoom(0, Objects.requireNonNull(initialRoom)));
    }
    public DeviceStateChanged change(Device replacement) {
        Objects.requireNonNull(replacement);
        VersionedRoom next = current.updateAndGet(previous -> {
            if (previous.room().devices().stream().noneMatch(d -> d.id().equals(replacement.id())))
                throw new IllegalArgumentException("Device does not belong to this room: " + replacement.id());
            var devices = previous.room().devices().stream()
                    .map(d -> d.id().equals(replacement.id()) ? replacement : d).toList();
            return new VersionedRoom(previous.version() + 1, new Room(previous.room().id(), previous.room().name(), devices));
        });
        var event = new DeviceStateChanged(sourceId, next.version(), next.room(), replacement.id());
        publish(event);
        return event;
    }
    public Room snapshot() { return current.get().room(); }
}
