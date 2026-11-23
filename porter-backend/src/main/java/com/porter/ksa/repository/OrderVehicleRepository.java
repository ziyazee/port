package com.porter.ksa.repository;

import com.porter.ksa.entity.OrderVehicle;
import com.porter.ksa.entity.OrderVehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderVehicleRepository extends JpaRepository<OrderVehicle, Long> {

    boolean existsByOrder_IdAndVehicle_Id(Long orderId, Long vehicleId);

    boolean existsByOrder_IdAndStatusNot(Long orderId, OrderVehicleStatus status);

    long countByOrder_Id(Long orderId);

    @EntityGraph(attributePaths = {"order", "vehicle", "driver"})
    List<OrderVehicle> findByOrder_IdOrderByIdAsc(Long orderId);

    @EntityGraph(attributePaths = {"order", "vehicle", "driver"})
    Optional<OrderVehicle> findById(Long id);
}
