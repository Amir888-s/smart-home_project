package smarthome;

import java.util.ArrayList;
import java.util.List;

/** Stateful over one traversal. Create a fresh instance for each independent audit. */
public final class SecurityAuditVisitor implements Visitor<List<String>> {
    private final List<String> findings;
    public SecurityAuditVisitor() { findings = new ArrayList<>(); }
    @Override public List<String> visit(Home home) {
        home.rooms().forEach(r -> r.accept(this)); return List.copyOf(findings);
    }
    @Override public List<String> visit(Room room) {
        room.devices().forEach(d -> d.accept(this)); return List.copyOf(findings);
    }
    @Override public List<String> visit(Lamp lamp) { return List.copyOf(findings); }
    @Override public List<String> visit(Thermostat thermostat) { return List.copyOf(findings); }
    @Override public List<String> visit(Camera camera) {
        if (!camera.on() || !camera.recording()) findings.add(camera.id() + ": camera is not recording");
        return List.copyOf(findings);
    }
    @Override public List<String> visit(SmartLock lock) {
        if (!lock.locked()) findings.add(lock.id() + ": door is unlocked");
        return List.copyOf(findings);
    }
    @Override public List<String> visit(SmokeDetector detector) {
        if (detector.alarm()) findings.add(detector.id() + ": smoke detected");
        return List.copyOf(findings);
    }
}
