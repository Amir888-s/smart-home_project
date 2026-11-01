package smarthome;

import java.util.Objects;

public record AutomationCommand(String deviceId, String action) implements HomeEvent {
    public AutomationCommand { Objects.requireNonNull(deviceId); Objects.requireNonNull(action); }
    @Override public EventType type() { return EventType.AUTOMATION_COMMAND; }
}
