package com.porter.ksa.service;

import com.porter.ksa.config.GpsModuleProperties;
import com.porter.ksa.entity.GpsEventType;
import com.porter.ksa.entity.TrackingSession;
import com.porter.ksa.entity.TrackingStatus;
import com.porter.ksa.entity.Vehicle;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final GpsModuleProperties properties;
    private final GpsEventService gpsEventService;
    private final TrackingLifecycleService lifecycleService;

    public GeofenceService(
            GpsModuleProperties properties,
            GpsEventService gpsEventService,
            TrackingLifecycleService lifecycleService
    ) {
        this.properties = properties;
        this.gpsEventService = gpsEventService;
        this.lifecycleService = lifecycleService;
    }

    public void evaluate(TrackingSession session) {
        Vehicle vehicle = session.getOrderVehicle().getVehicle();
        if (!hasFreshLocation(vehicle)) {
            return;
        }

        double pickupDistance = distanceMeters(
                vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(),
                session.getPickupLatitude(),
                session.getPickupLongitude()
        );
        double dropDistance = distanceMeters(
                vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(),
                session.getDropLatitude(),
                session.getDropLongitude()
        );
        boolean insidePickup = pickupDistance <= session.getGeofenceRadiusMeters();
        boolean insideDrop = dropDistance <= session.getGeofenceRadiusMeters();
        boolean stoppedOrSlow = vehicle.getCurrentSpeedKph() == null
                || vehicle.getCurrentSpeedKph() <= properties.getArrivalMaxSpeedKph();

        switch (session.getStatus()) {
            case ASSIGNED -> lifecycleService.applyGpsTransition(
                    session.getId(),
                    TrackingStatus.EN_ROUTE_TO_PICKUP,
                    "Fresh GPS location received"
            );
            case EN_ROUTE_TO_PICKUP -> {
                if (insidePickup && stoppedOrSlow) {
                    lifecycleService.applyGpsTransition(
                            session.getId(),
                            TrackingStatus.ARRIVED_AT_PICKUP,
                            "Vehicle arrived inside pickup geofence"
                    );
                }
            }
            case ARRIVED_AT_PICKUP -> {
                if (!insidePickup) {
                    lifecycleService.applyGpsTransition(
                            session.getId(),
                            TrackingStatus.LEFT_PICKUP,
                            "Vehicle left pickup geofence"
                    );
                }
            }
            case LEFT_PICKUP -> lifecycleService.applyGpsTransition(
                    session.getId(),
                    TrackingStatus.IN_TRANSIT,
                    "Vehicle is in transit to destination"
            );
            case IN_TRANSIT -> {
                if (insideDrop && stoppedOrSlow) {
                    lifecycleService.applyGpsTransition(
                            session.getId(),
                            TrackingStatus.ARRIVED_AT_DROP,
                            "Vehicle arrived inside destination geofence"
                    );
                }
            }
            case ARRIVED_AT_DROP -> {
                if (insideDrop && stoppedOrSlow) {
                    lifecycleService.applyGpsTransition(
                            session.getId(),
                            TrackingStatus.UNLOADING,
                            "Vehicle stopped for unloading"
                    );
                }
            }
            case UNLOADING -> {
                if (!insideDrop && hasCompletedDeliveryDwell(session)) {
                    recordLeftDrop(session, vehicle);
                    lifecycleService.applyGpsTransition(
                            session.getId(),
                            TrackingStatus.DELIVERED,
                            "Vehicle left destination after unloading"
                    );
                }
            }
            case DELIVERED, CANCELLED -> {
                // Terminal sessions are ignored.
            }
        }
    }

    private boolean hasFreshLocation(Vehicle vehicle) {
        if (vehicle.getCurrentLatitude() == null
                || vehicle.getCurrentLongitude() == null
                || vehicle.getLastSeenAt() == null) {
            return false;
        }

        Instant oldestAccepted = Instant.now().minusSeconds(Math.max(1, properties.getMaxLocationAgeSeconds()));
        return !vehicle.getLastSeenAt().isBefore(oldestAccepted);
    }

    private boolean hasCompletedDeliveryDwell(TrackingSession session) {
        return gpsEventService.findEventTime(session.getId(), GpsEventType.UNLOADING_STARTED)
                .map(startedAt -> Duration.between(startedAt, Instant.now()).getSeconds()
                        >= Math.max(0, properties.getDeliveryMinimumDwellSeconds()))
                .orElse(false);
    }

    private void recordLeftDrop(TrackingSession session, Vehicle vehicle) {
        if (!gpsEventService.hasEvent(session.getId(), GpsEventType.LEFT_DROP)) {
            gpsEventService.record(
                    session,
                    GpsEventType.LEFT_DROP,
                    vehicle.getCurrentLatitude(),
                    vehicle.getCurrentLongitude(),
                    "Vehicle left destination geofence"
            );
        }
    }

    double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
