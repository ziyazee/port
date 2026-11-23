package com.porter.ksa.repository;

import com.porter.ksa.entity.VehicleDriverAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDriverAssignmentRepository extends JpaRepository<VehicleDriverAssignment, Long> {

    List<VehicleDriverAssignment> findByVehicleIdOrderByAssignedAtDesc(Long vehicleId);

    Optional<VehicleDriverAssignment> findFirstByVehicleIdAndUnassignedAtIsNullOrderByAssignedAtDesc(Long vehicleId);

    void deleteByVehicleId(Long vehicleId);
}
