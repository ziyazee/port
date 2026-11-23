package com.porter.ksa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.porter.ksa.config.GpsModuleProperties;
import com.porter.ksa.dto.OrderVehicleAssignRequest;
import com.porter.ksa.dto.OrderVehicleResponse;
import com.porter.ksa.entity.CargoSpecification;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.Driver;
import com.porter.ksa.entity.DriverStatus;
import com.porter.ksa.entity.GpsEventType;
import com.porter.ksa.entity.GpsProviderType;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.OrderVehicleStatus;
import com.porter.ksa.entity.TrackingMode;
import com.porter.ksa.entity.TrackingStatus;
import com.porter.ksa.entity.TransportOrder;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.repository.DriverRepository;
import com.porter.ksa.repository.GpsEventRepository;
import com.porter.ksa.repository.OrderVehicleRepository;
import com.porter.ksa.repository.TrackingSessionRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import com.porter.ksa.repository.VehicleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrackingLifecycleServiceTests {

    @Autowired
    private CustomerAccountRepository customers;

    @Autowired
    private DriverRepository drivers;

    @Autowired
    private TransportOrderRepository orders;

    @Autowired
    private VehicleRepository vehicles;

    @Autowired
    private OrderVehicleRepository orderVehicles;

    @Autowired
    private TrackingSessionRepository trackingSessions;

    @Autowired
    private GpsEventRepository gpsEvents;

    @Autowired
    private OrderVehicleAssignmentService assignmentService;

    @Autowired
    private GeofenceService geofenceService;

    @Autowired
    private GpsModuleProperties gpsProperties;

    @Test
    void gpsLifecycleCompletesOrderAndReleasesVehicle() {
        CustomerAccount customer = customers.save(new CustomerAccount(
                "Lifecycle Test Company",
                "REG-LIFECYCLE",
                "VAT-LIFECYCLE",
                "Riyadh",
                "0500000000",
                "lifecycle@example.com",
                List.of("ops@example.com"),
                null,
                null,
                null,
                "test"
        ));
        TransportOrder order = orders.save(new TransportOrder(
                customer,
                "Pickup",
                "Destination",
                24.7136,
                46.6753,
                24.8000,
                46.8000,
                VehicleType.FLATBED,
                "Lifecycle cargo",
                CargoSpecification.REGULAR_CARGO,
                null,
                null,
                null,
                1,
                "test"
        ));
        Vehicle vehicle = new Vehicle(
                "LIFECYCLE-001",
                "TEST-LIFECYCLE-001",
                VehicleType.FLATBED,
                VehicleStatus.AVAILABLE
        );
        vehicle.updateGpsDevice(GpsProviderType.WIALON, "734455");
        vehicle = vehicles.save(vehicle);
        Driver driver = new Driver(
                "Lifecycle Driver",
                "+966500000001",
                "NAT-LIFECYCLE-001",
                "LIC-LIFECYCLE-001",
                DriverStatus.AVAILABLE
        );
        driver.assignToVehicle(vehicle);
        vehicle.updateDriverName(driver.getFullName());
        drivers.save(driver);
        vehicles.save(vehicle);
        gpsProperties.setAutoStartTracking(true);
        OrderVehicleResponse assignment = assignmentService.assign(
                order.getId(),
                new OrderVehicleAssignRequest(vehicle.getId(), null, "test")
        );
        Long trackingSessionId = trackingSessions.findByOrderVehicle_Id(assignment.id()).orElseThrow().getId();
        assertEquals(OrderStatus.TRACKING_STARTED, orders.findById(order.getId()).orElseThrow().getOrderStatus());

        gpsProperties.setDeliveryMinimumDwellSeconds(0);
        observe(trackingSessionId, vehicle.getId(), 24.7000, 46.6600, 30);
        assertStatus(trackingSessionId, TrackingStatus.EN_ROUTE_TO_PICKUP);
        observe(trackingSessionId, vehicle.getId(), 24.7136, 46.6753, 0);
        assertStatus(trackingSessionId, TrackingStatus.ARRIVED_AT_PICKUP);
        observe(trackingSessionId, vehicle.getId(), 24.7200, 46.6900, 25);
        assertStatus(trackingSessionId, TrackingStatus.LEFT_PICKUP);
        observe(trackingSessionId, vehicle.getId(), 24.7300, 46.7000, 30);
        assertStatus(trackingSessionId, TrackingStatus.IN_TRANSIT);
        observe(trackingSessionId, vehicle.getId(), 24.8000, 46.8000, 2);
        assertStatus(trackingSessionId, TrackingStatus.ARRIVED_AT_DROP);
        observe(trackingSessionId, vehicle.getId(), 24.8000, 46.8000, 0);
        assertStatus(trackingSessionId, TrackingStatus.UNLOADING);
        observe(trackingSessionId, vehicle.getId(), 24.8100, 46.8100, 20);
        assertStatus(trackingSessionId, TrackingStatus.DELIVERED);

        assertEquals(
                OrderVehicleStatus.DELIVERED,
                orderVehicles.findById(assignment.id()).orElseThrow().getStatus()
        );
        assertEquals(OrderStatus.COMPLETED, orders.findById(order.getId()).orElseThrow().getOrderStatus());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(vehicle.getId()).orElseThrow().getStatus());
        Driver completedDriver = drivers.findById(driver.getId()).orElseThrow();
        assertEquals(DriverStatus.AVAILABLE, completedDriver.getStatus());
        assertEquals(vehicle.getId(), completedDriver.getCurrentVehicle().getId());
        assertEquals(
                TrackingMode.STOPPED,
                trackingSessions.findById(trackingSessionId).orElseThrow().getMode()
        );
        assertNotNull(trackingSessions.findById(trackingSessionId).orElseThrow().getStoppedAt());
        assertEquals(
                1,
                gpsEvents.findByTrackingSession_IdOrderByEventAtAsc(trackingSessionId)
                        .stream()
                        .filter(event -> event.getEventType() == GpsEventType.DELIVERED)
                        .count()
        );
    }

    private void observe(
            Long trackingSessionId,
            Long vehicleId,
            double latitude,
            double longitude,
            double speedKph
    ) {
        Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow();
        vehicle.updateLatestLocation(latitude, longitude, speedKph, 0, java.time.Instant.now());
        vehicles.save(vehicle);
        geofenceService.evaluate(trackingSessions.findByIdWithLifecycle(trackingSessionId).orElseThrow());
    }

    private void assertStatus(Long trackingSessionId, TrackingStatus status) {
        assertEquals(status, trackingSessions.findById(trackingSessionId).orElseThrow().getStatus());
    }
}
