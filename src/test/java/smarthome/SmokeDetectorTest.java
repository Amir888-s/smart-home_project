package smarthome;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SmokeDetectorTest {
    @Test void smokeDetectorAddsStandbyWatts() {
        var room = new Room("r", "Room", List.of(new Lamp("l", "Lamp", true, 10), new SmokeDetector("s", "Smoke", false, 1)));
        assertEquals(11.0, room.accept(new PowerUseVisitor()), 0.0001);
    }
    @Test void smokeAlarmAddsSecurityFinding() {
        assertEquals(List.of("s: smoke detected"), new SmokeDetector("s", "Smoke", true, 1).accept(new SecurityAuditVisitor()));
    }
    @Test void filterAndInventorySupportNewLeaf() {
        var detector = new SmokeDetector("s", "Smoke", false, 1);
        assertEquals(List.of(detector), detector.accept(new DeviceFilterVisitor(d -> d instanceof SmokeDetector)));
        assertEquals(Map.of("SmokeDetector", 1), detector.accept(new InventoryVisitor()));
    }
    @Test void jsonExportsNewLeafFields() {
        assertEquals("{\"type\":\"SmokeDetector\",\"id\":\"s\",\"name\":\"Smoke\",\"watts\":1.0,\"alarm\":false}",
            new SmokeDetector("s", "Smoke", false, 1).accept(new JsonExportVisitor()));
    }
}
