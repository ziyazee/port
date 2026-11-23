package com.porter.ksa.repository;

import com.porter.ksa.entity.GpsLatestLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsLatestLocationRepository extends JpaRepository<GpsLatestLocation, Long> {

    @EntityGraph(attributePaths = "gpsDevice")
    List<GpsLatestLocation> findAllByOrderByGpsDeviceVehicleFleetNumberAsc();

    @EntityGraph(attributePaths = "gpsDevice")
    Optional<GpsLatestLocation> findByGpsDeviceId(Long gpsDeviceId);
}
