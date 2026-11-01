package smarthome;

/** Instantaneous power in watts, not energy in watt-hours. Stateless and reusable. */
public final class PowerUseVisitor implements Visitor<Double> {
    @Override public Double visit(Home home) { return home.rooms().stream().mapToDouble(r -> r.accept(this)).sum(); }
    @Override public Double visit(Room room) { return room.devices().stream().mapToDouble(d -> d.accept(this)).sum(); }
    @Override public Double visit(Lamp lamp) { return lamp.on() ? lamp.watts() : 0.0; }
    @Override public Double visit(Thermostat thermostat) { return thermostat.heating() ? thermostat.watts() : 0.0; }
    @Override public Double visit(Camera camera) { return camera.on() ? camera.watts() : 0.0; }
    @Override public Double visit(SmartLock lock) { return lock.watts(); }
    @Override public Double visit(SmokeDetector detector) { return detector.watts(); }
}
