package smarthome;

public record Lamp(String id, String name, boolean on, double watts) implements Device {
    public Lamp { DeviceChecks.identity(id, name); DeviceChecks.power(watts); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
