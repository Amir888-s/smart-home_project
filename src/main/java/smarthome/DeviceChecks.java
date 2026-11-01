package smarthome;

import java.util.Objects;

/** Shared constructor validation keeps malformed data out of immutable snapshots. */
final class DeviceChecks {
    private DeviceChecks() { }
    static void identity(String id, String name) {
        if (Objects.requireNonNull(id).isBlank() || Objects.requireNonNull(name).isBlank())
            throw new IllegalArgumentException("ID and name must not be blank");
    }
    static void power(double watts) {
        if (!Double.isFinite(watts) || watts < 0) throw new IllegalArgumentException("Power must be finite and nonnegative");
    }
}
