package smarthome;

import java.util.Map;

public record EnergySummary(Map<String, Double> wattsBySource, double totalWatts) implements HomeEvent {
    public EnergySummary { wattsBySource = Map.copyOf(wattsBySource); }
    @Override public EventType type() { return EventType.ENERGY_SUMMARY; }
}
