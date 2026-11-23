package com.porter.ksa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.porter.ksa.dto.OrderCreateRequest;
import com.porter.ksa.dto.OrderResponse;
import com.porter.ksa.entity.CargoSpecification;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.repository.OrderVehicleRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import com.porter.ksa.repository.VehicleRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class OrderCreationServiceTests {

    @Autowired
    private OrderCreationService orderCreationService;

    @Autowired
    private CustomerAccountRepository customerAccounts;

    @Autowired
    private VehicleRepository vehicles;

    @Autowired
    private TransportOrderRepository orders;

    @Autowired
    private OrderVehicleRepository orderVehicles;

    @BeforeEach
    @AfterEach
    void resetData() {
        orderVehicles.deleteAll();
        orders.deleteAll();
        vehicles.deleteAll();
        customerAccounts.deleteAll();
    }

    @Test
    void createsOrderAndInitialVehicleAssignmentTogether() {
        CustomerAccount customer = customerAccounts.save(customer());
        Vehicle vehicle = vehicles.save(vehicle("FLEET-100", "ABC-100"));

        OrderResponse response = orderCreationService.create(request(customer.getId(), List.of(vehicle.getId())));

        assertEquals(1, response.assignedVehicles());
        assertEquals(OrderStatus.VEHICLE_ASSIGNED, response.orderStatus());
        assertEquals(1, orderVehicles.findByOrder_IdOrderByIdAsc(response.id()).size());
        assertEquals(VehicleStatus.ASSIGNED, vehicles.findById(vehicle.getId()).orElseThrow().getStatus());
    }

    @Test
    void rollsBackOrderAndAssignmentsWhenAnyVehicleIsInvalid() {
        CustomerAccount customer = customerAccounts.save(customer());
        Vehicle vehicle = vehicles.save(vehicle("FLEET-200", "ABC-200"));

        assertThrows(
                ResponseStatusException.class,
                () -> orderCreationService.create(request(customer.getId(), List.of(vehicle.getId(), 999_999L)))
        );

        assertEquals(0, orders.count());
        assertEquals(0, orderVehicles.count());
        assertEquals(VehicleStatus.AVAILABLE, vehicles.findById(vehicle.getId()).orElseThrow().getStatus());
    }

    private OrderCreateRequest request(Long customerId, List<Long> vehicleIds) {
        return new OrderCreateRequest(
                customerId,
                "Dammam Industrial",
                "Riyadh Dry Port",
                26.4207,
                50.0888,
                24.7136,
                46.6753,
                VehicleType.LOWBED,
                "Steel pipes",
                CargoSpecification.REGULAR_CARGO,
                "100",
                "1000",
                "600 x 200 x 100 cm",
                1,
                vehicleIds,
                "operations"
        );
    }

    private CustomerAccount customer() {
        return new CustomerAccount(
                "Order Creation Customer",
                "CR-ORDER-100",
                "VAT-ORDER-100",
                "Riyadh 12211",
                "+966512345678",
                "orders@example.com",
                List.of(),
                null,
                null,
                null,
                "operations"
        );
    }

    private Vehicle vehicle(String fleetNumber, String plateNumber) {
        return new Vehicle(
                fleetNumber,
                plateNumber,
                VehicleType.LOWBED,
                VehicleStatus.AVAILABLE,
                "Assigned Driver"
        );
    }
}
