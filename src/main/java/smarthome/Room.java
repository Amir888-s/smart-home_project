package smarthome;

import java.util.List;
import java.util.HashSet;

public record Room(String id, String name, List<Device> devices) implements Element {
    public Room {
        DeviceChecks.identity(id, name);
        devices = List.copyOf(devices);
        var ids = new HashSet<String>();
        for (Device device : devices) if (!ids.add(device.id())) throw new IllegalArgumentException("Duplicate device ID");
    }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
