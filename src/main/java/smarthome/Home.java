package smarthome;

import java.util.List;
import java.util.HashSet;

public record Home(String id, String name, List<Room> rooms) implements Element {
    public Home {
        DeviceChecks.identity(id, name);
        rooms = List.copyOf(rooms);
        var ids = new HashSet<String>();
        for (Room room : rooms) if (!ids.add(room.id())) throw new IllegalArgumentException("Duplicate room ID");
    }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
