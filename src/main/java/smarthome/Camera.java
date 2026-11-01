package smarthome;

public record Camera(String id, String name, boolean on, double watts, boolean recording) implements Device {
    public Camera { DeviceChecks.identity(id, name); DeviceChecks.power(watts); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
