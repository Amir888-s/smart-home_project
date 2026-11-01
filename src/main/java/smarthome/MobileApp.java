package smarthome;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class MobileApp implements Observer<HomeEvent> {
    private final Map<EventType, AtomicLong> counts;
    public MobileApp() {
        var values = new EnumMap<EventType, AtomicLong>(EventType.class);
        for (EventType type : EventType.values()) values.put(type, new AtomicLong());
        counts = Map.copyOf(values);
    }
    @Override public void onEvent(HomeEvent event) { counts.get(event.type()).incrementAndGet(); }
    public long count(EventType type) { return counts.get(type).get(); }
}
