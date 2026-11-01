package smarthome;

/** Fifth leaf, added in its own trade-off commit. Always draws standby power. */
public record SmokeDetector(String id, String name, boolean alarm, double watts) implements Device {
    public SmokeDetector { DeviceChecks.identity(id, name); DeviceChecks.power(watts); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
