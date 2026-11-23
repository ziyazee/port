package com.porter.ksa.config;

import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.VehicleRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class VehicleSeeder implements CommandLineRunner {

    private final VehicleRepository vehicles;

    public VehicleSeeder(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    @Override
    public void run(String... args) {
        if (vehicles.count() > 0) {
            return;
        }

        vehicles.saveAll(List.of(
                vehicle("LB-001", "KSA-LB-1001", VehicleType.LOWBED, VehicleStatus.AVAILABLE),
                vehicle("LB-002", "KSA-LB-1002", VehicleType.LOWBED, VehicleStatus.AVAILABLE),
                vehicle("LB-003", "KSA-LB-1003", VehicleType.LOWBED, VehicleStatus.ASSIGNED),
                vehicle("LB-004", "KSA-LB-1004", VehicleType.LOWBED, VehicleStatus.MAINTENANCE),
                vehicle("LB-005", "KSA-LB-1005", VehicleType.LOWBED, VehicleStatus.AVAILABLE),
                vehicle("FB-001", "KSA-FB-2001", VehicleType.FLATBED, VehicleStatus.AVAILABLE),
                vehicle("FB-002", "KSA-FB-2002", VehicleType.FLATBED, VehicleStatus.AVAILABLE),
                vehicle("FB-003", "KSA-FB-2003", VehicleType.FLATBED, VehicleStatus.AVAILABLE),
                vehicle("FB-004", "KSA-FB-2004", VehicleType.FLATBED, VehicleStatus.ASSIGNED),
                vehicle("FB-005", "KSA-FB-2005", VehicleType.FLATBED, VehicleStatus.INACTIVE),
                vehicle("CT-001", "KSA-CT-3001", VehicleType.CURTAIN_TRAILER, VehicleStatus.AVAILABLE),
                vehicle("CT-002", "KSA-CT-3002", VehicleType.CURTAIN_TRAILER, VehicleStatus.AVAILABLE),
                vehicle("CT-003", "KSA-CT-3003", VehicleType.CURTAIN_TRAILER, VehicleStatus.ASSIGNED),
                vehicle("CT-004", "KSA-CT-3004", VehicleType.CURTAIN_TRAILER, VehicleStatus.MAINTENANCE),
                vehicle("CT-005", "KSA-CT-3005", VehicleType.CURTAIN_TRAILER, VehicleStatus.AVAILABLE),
                vehicle("RF-001", "KSA-RF-4001", VehicleType.REFER_TRUCK, VehicleStatus.AVAILABLE),
                vehicle("RF-002", "KSA-RF-4002", VehicleType.REFER_TRUCK, VehicleStatus.ASSIGNED),
                vehicle("RF-003", "KSA-RF-4003", VehicleType.REFER_TRUCK, VehicleStatus.AVAILABLE),
                vehicle("RF-004", "KSA-RF-4004", VehicleType.REFER_TRUCK, VehicleStatus.MAINTENANCE),
                vehicle("RF-005", "KSA-RF-4005", VehicleType.REFER_TRUCK, VehicleStatus.AVAILABLE)
        ));
    }

    private Vehicle vehicle(String fleetNumber, String plateNumber, VehicleType vehicleType, VehicleStatus status) {
        return new Vehicle(
                fleetNumber,
                plateNumber,
                vehicleType,
                status,
                "Driver " + fleetNumber
        );
    }
}
