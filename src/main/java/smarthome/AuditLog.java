package smarthome;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AuditLog implements Observer<HomeEvent> {
    private final ConcurrentLinkedQueue<HomeEvent> events;
    public AuditLog() { events = new ConcurrentLinkedQueue<>(); }
    @Override public void onEvent(HomeEvent event) { events.add(event); }
    public List<HomeEvent> snapshot() { return List.copyOf(events); }
}
