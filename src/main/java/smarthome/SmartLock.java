package smarthome;

public record SmartLock(String id, String name, boolean locked, double watts) implements Device {
    public SmartLock { DeviceChecks.identity(id, name); DeviceChecks.power(watts); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
