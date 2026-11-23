package com.porter.ksa.repository;

import com.porter.ksa.entity.GpsDevice;
import com.porter.ksa.entity.GpsProviderType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsDeviceRepository extends JpaRepository<GpsDevice, Long> {

    boolean existsByProviderAndProviderVehicleId(GpsProviderType provider, String providerVehicleId);

    boolean existsByProviderAndProviderVehicleIdAndVehicleFleetNumberNot(
            GpsProviderType provider,
            String providerVehicleId,
            String vehicleFleetNumber
    );

    List<GpsDevice> findByActiveTrueOrderByIdAsc();

    Optional<GpsDevice> findByProviderAndProviderVehicleId(GpsProviderType provider, String providerVehicleId);

    Optional<GpsDevice> findFirstByVehicleFleetNumberOrderByIdAsc(String vehicleFleetNumber);
}
