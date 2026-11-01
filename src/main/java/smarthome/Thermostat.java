package smarthome;

public record Thermostat(String id, String name, boolean heating, double watts, double targetCelsius) implements Device {
    public Thermostat {
        DeviceChecks.identity(id, name); DeviceChecks.power(watts);
        if (!Double.isFinite(targetCelsius)) throw new IllegalArgumentException("Temperature must be finite");
    }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
