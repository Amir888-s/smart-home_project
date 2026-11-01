package smarthome;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InventoryVisitorTest {
    @Test void newVisitorCountsAllEightDevicesWithoutChangingElements() {
        assertEquals(Map.of("Lamp", 2, "Thermostat", 2, "Camera", 2, "SmartLock", 2),
            Fixtures.home().accept(new InventoryVisitor()));
    }
    @Test void emptyHomeProducesEmptyInventory() {
        assertEquals(Map.of(), new Home("h", "Home", List.of()).accept(new InventoryVisitor()));
    }
}
