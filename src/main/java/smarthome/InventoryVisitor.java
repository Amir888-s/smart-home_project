package smarthome;

import java.util.HashMap;
import java.util.Map;

/** New operation added without modifying any Element implementation or Visitor interface. */
public final class InventoryVisitor implements Visitor<Map<String, Integer>> {
    private final Map<String, Integer> counts;
    public InventoryVisitor() { counts = new HashMap<>(); }
    @Override public Map<String, Integer> visit(Home home) {
        home.rooms().forEach(r -> r.accept(this)); return Map.copyOf(counts);
    }
    @Override public Map<String, Integer> visit(Room room) {
        room.devices().forEach(d -> d.accept(this)); return Map.copyOf(counts);
    }
    private Map<String, Integer> count(String type) {
        counts.merge(type, 1, Integer::sum); return Map.copyOf(counts);
    }
    @Override public Map<String, Integer> visit(Lamp lamp) { return count("Lamp"); }
    @Override public Map<String, Integer> visit(Thermostat thermostat) { return count("Thermostat"); }
    @Override public Map<String, Integer> visit(Camera camera) { return count("Camera"); }
    @Override public Map<String, Integer> visit(SmartLock lock) { return count("SmartLock"); }
    @Override public Map<String, Integer> visit(SmokeDetector detector) { return count("SmokeDetector"); }
}
