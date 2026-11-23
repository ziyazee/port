package com.porter.ksa.repository;

import com.porter.ksa.entity.TrackingMode;
import com.porter.ksa.entity.TrackingSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, Long> {

    boolean existsByOrderVehicle_Id(Long orderVehicleId);

    @EntityGraph(attributePaths = {"orderVehicle", "orderVehicle.vehicle", "orderVehicle.order"})
    Optional<TrackingSession> findByOrderVehicle_Id(Long orderVehicleId);

    @EntityGraph(attributePaths = {
            "orderVehicle",
            "orderVehicle.vehicle",
            "orderVehicle.driver",
            "orderVehicle.order"
    })
    @Query("select session from TrackingSession session where session.id = :id")
    Optional<TrackingSession> findByIdWithLifecycle(Long id);

    @EntityGraph(attributePaths = {"orderVehicle", "orderVehicle.vehicle", "orderVehicle.order"})
    List<TrackingSession> findByModeNotOrderByIdAsc(TrackingMode mode);
}
