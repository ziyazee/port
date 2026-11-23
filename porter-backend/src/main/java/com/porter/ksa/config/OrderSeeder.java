package com.porter.ksa.config;

import com.porter.ksa.entity.CargoSpecification;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.TransportOrder;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@ConditionalOnProperty(name = "porter.seed.orders.enabled", havingValue = "true", matchIfMissing = true)
public class OrderSeeder implements CommandLineRunner {

    private final TransportOrderRepository orders;
    private final CustomerAccountRepository customerAccounts;

    public OrderSeeder(
            TransportOrderRepository orders,
            CustomerAccountRepository customerAccounts
    ) {
        this.orders = orders;
        this.customerAccounts = customerAccounts;
    }

    @Override
    public void run(String... args) {
        if (orders.count() > 0) {
            return;
        }

        List<CustomerAccount> customers = customerAccounts.findAll();
        if (customers.isEmpty()) {
            return;
        }

        orders.saveAll(List.of(
                order(customers.get(0), "Dammam Industrial", "Riyadh Dry Port", VehicleType.LOWBED, "Steel Pipes", 1, OrderStatus.CREATED),
                order(customers.get(1 % customers.size()), "Jubail 2", "King Fahd Port", VehicleType.FLATBED, "Palletized chemicals", 2, OrderStatus.VERIFIED),
                order(customers.get(2 % customers.size()), "Ras Al Khair", "NEOM Hub 1", VehicleType.CURTAIN_TRAILER, "Mining equipment crates", 1, OrderStatus.VEHICLE_ASSIGNED),
                order(customers.get(3 % customers.size()), "Jeddah Yard", "Riyadh South Industrial", VehicleType.REFER_TRUCK, "Temperature controlled materials", 3, OrderStatus.TRACKING_STARTED),
                order(customers.get(4 % customers.size()), "Second Industrial City", "Dammam Port", VehicleType.FLATBED, "Fabricated steel beams", 2, OrderStatus.IN_PROGRESS),
                order(customers.get(5 % customers.size()), "Riyadh Yard", "Qassim Hub", VehicleType.LOWBED, "Generator skids", 1, OrderStatus.COMPLETED),
                order(customers.get(6 % customers.size()), "Dammam Port", "Jubail 1", VehicleType.CURTAIN_TRAILER, "Event staging material", 2, OrderStatus.CANCELLED)
        ));
    }

    private TransportOrder order(
            CustomerAccount customerAccount,
            String fromLocation,
            String toLocation,
            VehicleType vehicleType,
            String loadDescription,
            int quantityUnits,
            OrderStatus status
    ) {
        TransportOrder order = new TransportOrder(
                customerAccount,
                fromLocation,
                toLocation,
                vehicleType,
                loadDescription,
                CargoSpecification.REGULAR_CARGO,
                "0",
                "0",
                "0 cm L x 0 cm W x 0 cm H",
                quantityUnits,
                "system"
        );
        order.updateStatus(status, "system");
        return order;
    }
}
