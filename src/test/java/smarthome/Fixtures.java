package smarthome;
import java.util.List;
final class Fixtures {
    private Fixtures() { }
    static Home home() {
        return new Home("h", "Home", List.of(
            new Room("living", "Living", List.of(new Lamp("l", "Lamp", true, 10),
                new Thermostat("t", "Heating", true, 1000, 22),
                new Camera("c", "Camera", true, 8, false), new SmartLock("k", "Lock", false, 2))),
            new Room("bed", "Bedroom", List.of(new Lamp("b", "Bed lamp", false, 6),
                new Thermostat("bt", "Bed heating", false, 800, 20),
                new Camera("bc", "Bed camera", false, 5, false), new SmartLock("bk", "Bed lock", true, 1)))));
    }
    static DeviceStateChanged event(long version) {
        return new DeviceStateChanged("living-source", version, home().rooms().get(0), "l");
    }
}
