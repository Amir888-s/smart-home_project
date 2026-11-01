package smarthome;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class VisitorTest {
    @Test void homePowerMatchesHandCalculation() { assertEquals(1021.0, Fixtures.home().accept(new PowerUseVisitor()), 0.0001); }
    @Test void roomPowerMatchesHandCalculation() { assertEquals(1020.0, Fixtures.home().rooms().get(0).accept(new PowerUseVisitor()), 0.0001); }
    @Test void switchedOffDevicesContributeZero() {
        var visitor = new PowerUseVisitor();
        assertEquals(0.0, new Lamp("l", "Lamp", false, 10).accept(visitor), 0.0001);
        assertEquals(0.0, new Thermostat("t", "Heat", false, 1000, 22).accept(visitor), 0.0001);
        assertEquals(0.0, new Camera("c", "Cam", false, 8, true).accept(visitor), 0.0001);
    }
    @Test void statefulAuditAccumulatesAcrossBothRooms() {
        assertEquals(List.of("c: camera is not recording", "k: door is unlocked", "bc: camera is not recording"),
            Fixtures.home().accept(new SecurityAuditVisitor()));
    }
    @Test void secureRoomHasNoFindings() {
        var room = new Room("r", "Room", List.of(new Camera("c", "Cam", true, 8, true), new SmartLock("k", "Lock", true, 2)));
        assertEquals(List.of(), room.accept(new SecurityAuditVisitor()));
    }
    @Test void filterReturnsExactlyTwoHighRatedThermostats() {
        var result = Fixtures.home().accept(new DeviceFilterVisitor(d -> d.watts() > 100));
        assertEquals(List.of("t", "bt"), result.stream().map(Element::id).toList());
    }
    @Test void filterCanReturnEmptyList() { assertTrue(Fixtures.home().accept(new DeviceFilterVisitor(d -> false)).isEmpty()); }
    @Test void jsonHasExactNestingAndLeafFields() {
        var home = new Home("h", "Home", List.of(new Room("r", "Room", List.of(new Lamp("l", "Lamp", true, 10)))));
        assertEquals("{\"type\":\"Home\",\"id\":\"h\",\"name\":\"Home\",\"rooms\":[{\"type\":\"Room\",\"id\":\"r\",\"name\":\"Room\",\"devices\":[{\"type\":\"Lamp\",\"id\":\"l\",\"name\":\"Lamp\",\"watts\":10.0,\"on\":true}]}]}", home.accept(new JsonExportVisitor()));
    }
    @Test void jsonEscapesQuotesSlashesAndControlCharacters() {
        assertEquals("\"a\\\"\\\\\\n\\r\\t\\b\\f\\u0001\"", JsonExportVisitor.quote("a\"\\\n\r\t\b\f\u0001"));
    }
    @Test void jsonSupportsEmptyContainers() {
        assertEquals("{\"type\":\"Home\",\"id\":\"h\",\"name\":\"Home\",\"rooms\":[]}", new Home("h", "Home", List.of()).accept(new JsonExportVisitor()));
    }
    @Test void elementReferenceStillDispatchesToLampOverload() {
        Element leaf = new Lamp("l", "Lamp", true, 13);
        assertEquals(13.0, leaf.accept(new PowerUseVisitor()), 0.0001);
    }
    @Test void invalidDeviceDataIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Lamp("l", "Lamp", true, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Lamp("l", "Lamp", true, -1));
        assertThrows(IllegalArgumentException.class, () -> new Thermostat("t", "Heat", true, 10, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Room("r", "Room", List.of(new Lamp("l", "One", true, 1), new Lamp("l", "Two", true, 2))));
    }
}
