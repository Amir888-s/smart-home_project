package smarthome;

import java.util.Objects;

public record DeviceStateChanged(String sourceId, long version, Room room, String deviceId) implements HomeEvent {
    public DeviceStateChanged {
        Objects.requireNonNull(sourceId); Objects.requireNonNull(room); Objects.requireNonNull(deviceId);
        if (version < 1) throw new IllegalArgumentException("Version must be positive");
        if (room.devices().stream().noneMatch(d -> d.id().equals(deviceId))) throw new IllegalArgumentException("Unknown device");
    }
    @Override public EventType type() { return EventType.DEVICE_STATE; }
}
