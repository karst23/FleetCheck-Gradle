package pt.upt.fleetcheck;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetServiceTest {

    @Test
    void vehicleAtServiceIntervalShouldNeedService() {
        FleetService service = new FleetService();

        Vehicle vehicle = new Vehicle(
                "V1",
                "Test Car",
                50000,
                40000,
                10000
        );

        assertTrue(service.needsService(vehicle));
    }
}