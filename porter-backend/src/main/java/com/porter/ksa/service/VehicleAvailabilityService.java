package com.porter.ksa.service;

import com.porter.ksa.dto.VehicleAvailabilitySearchResponse;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.VehicleRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleAvailabilityService {

    private static final double AVERAGE_SPEED_KPH = 45.0;

    private final VehicleRepository vehicles;

    public VehicleAvailabilityService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    public List<VehicleAvailabilitySearchResponse> search(
            VehicleType vehicleType,
            Double pickupLatitude,
            Double pickupLongitude,
            boolean includeAssigned
    ) {
        List<Vehicle> candidates = vehicleType == null
                ? vehicles.findAllByOrderByIdAsc()
                : vehicles.findTop20ByVehicleTypeOrderByIdAsc(vehicleType);

        return candidates.stream()
                .filter(vehicle -> isCandidate(vehicle, includeAssigned))
                .map(vehicle -> toResponse(vehicle, pickupLatitude, pickupLongitude))
                .sorted(Comparator
                        .comparing(VehicleAvailabilitySearchResponse::availabilityGroup)
                        .thenComparing(response -> response.distanceKm() == null ? Double.MAX_VALUE : response.distanceKm())
                        .thenComparing(VehicleAvailabilitySearchResponse::fleetNumber))
                .toList();
    }

    private boolean isCandidate(Vehicle vehicle, boolean includeAssigned) {
        return vehicle.getStatus() == VehicleStatus.AVAILABLE
                || (includeAssigned && vehicle.getStatus() == VehicleStatus.ASSIGNED);
    }

    private VehicleAvailabilitySearchResponse toResponse(
            Vehicle vehicle,
            Double pickupLatitude,
            Double pickupLongitude
    ) {
        Double distanceKm = distanceKm(vehicle, pickupLatitude, pickupLongitude);
        Integer eta = distanceKm == null ? null : (int) Math.ceil((distanceKm / AVERAGE_SPEED_KPH) * 60.0);
        return new VehicleAvailabilitySearchResponse(
                vehicle.getId(),
                vehicle.getFleetNumber(),
                vehicle.getPlateNumber(),
                vehicle.getVehicleType(),
                vehicle.getStatus(),
                availabilityGroup(vehicle),
                vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(),
                distanceKm,
                eta,
                vehicle.getLastSeenAt()
        );
    }

    private String availabilityGroup(Vehicle vehicle) {
        if (vehicle.getStatus() == VehicleStatus.ASSIGNED) {
            return "IN_TRANSIT_OR_ASSIGNED";
        }
        if (vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
            return "AVAILABLE_LOCATION_UNKNOWN";
        }
        return "AVAILABLE_NEAR_PICKUP";
    }

    private Double distanceKm(Vehicle vehicle, Double pickupLatitude, Double pickupLongitude) {
        if (pickupLatitude == null
                || pickupLongitude == null
                || vehicle.getCurrentLatitude() == null
                || vehicle.getCurrentLongitude() == null) {
            return null;
        }

        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(vehicle.getCurrentLatitude() - pickupLatitude);
        double dLng = Math.toRadians(vehicle.getCurrentLongitude() - pickupLongitude);
        double lat1 = Math.toRadians(pickupLatitude);
        double lat2 = Math.toRadians(vehicle.getCurrentLatitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(earthRadiusKm * c * 10.0) / 10.0;
    }
}
