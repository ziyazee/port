package com.porter.ksa.repository;

import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    long countByVehicleTypeAndStatus(VehicleType vehicleType, VehicleStatus status);

    long countByStatus(VehicleStatus status);

    boolean existsByFleetNumber(String fleetNumber);

    boolean existsByFleetNumberAndIdNot(String fleetNumber, Long id);

    boolean existsByPlateNumber(String plateNumber);

    boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);

    Optional<Vehicle> findByFleetNumber(String fleetNumber);

    List<Vehicle> findAllByOrderByIdAsc();

    List<Vehicle> findByStatusOrderByIdAsc(VehicleStatus status);

    List<Vehicle> findByVehicleTypeAndStatusOrderByIdAsc(VehicleType vehicleType, VehicleStatus status);

    List<Vehicle> findTop20ByVehicleTypeOrderByIdAsc(VehicleType vehicleType);
}
