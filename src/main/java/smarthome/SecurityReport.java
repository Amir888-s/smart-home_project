package smarthome;

import java.util.List;
import java.util.Objects;

public record SecurityReport(String roomId, long sourceVersion, List<String> findings) implements HomeEvent {
    public SecurityReport { Objects.requireNonNull(roomId); findings = List.copyOf(findings); }
    @Override public EventType type() { return EventType.SECURITY_REPORT; }
}
