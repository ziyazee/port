package com.porter.ksa.service;

import com.porter.ksa.dto.TrackingLocationUpdateResponse;
import com.porter.ksa.dto.TrackingModeUpdateRequest;
import com.porter.ksa.dto.TrackingSessionCreateRequest;
import com.porter.ksa.dto.TrackingSessionResponse;
import com.porter.ksa.config.GpsModuleProperties;
import com.porter.ksa.entity.GpsProviderType;
import com.porter.ksa.entity.OrderVehicle;
import com.porter.ksa.entity.OrderVehicleStatus;
import com.porter.ksa.entity.TrackingMode;
import com.porter.ksa.entity.TrackingSession;
import com.porter.ksa.entity.TransportOrder;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.repository.OrderVehicleRepository;
import com.porter.ksa.repository.TrackingSessionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TrackingSessionService {

    private final TrackingSessionRepository trackingSessions;
    private final OrderVehicleRepository orderVehicles;
    private final TrackingLiveUpdateService liveUpdateService;
    private final TrackingLifecycleService lifecycleService;
    private final GpsModuleProperties properties;

    public TrackingSessionService(
            TrackingSessionRepository trackingSessions,
            OrderVehicleRepository orderVehicles,
            TrackingLiveUpdateService liveUpdateService,
            TrackingLifecycleService lifecycleService,
            GpsModuleProperties properties
    ) {
        this.trackingSessions = trackingSessions;
        this.orderVehicles = orderVehicles;
        this.liveUpdateService = liveUpdateService;
        this.lifecycleService = lifecycleService;
        this.properties = properties;
    }

    public TrackingSessionResponse start(TrackingSessionCreateRequest request) {
        if (trackingSessions.existsByOrderVehicle_Id(request.orderVehicleId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tracking session already exists for assignment");
        }

        OrderVehicle orderVehicle = orderVehicles.findById(request.orderVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order vehicle assignment not found"));
        TrackingSession session = createSession(
                orderVehicle,
                request.mode() == null ? TrackingMode.NORMAL_TRACKING : request.mode(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropLatitude(),
                request.dropLongitude(),
                request.geofenceRadiusMeters()
        );

        return toResponse(session);
    }

    public void startAutomatically(OrderVehicle orderVehicle) {
        if (!properties.isAutoStartTracking()
                || trackingSessions.existsByOrderVehicle_Id(orderVehicle.getId())) {
            return;
        }

        TransportOrder order = orderVehicle.getOrder();
        Vehicle vehicle = orderVehicle.getVehicle();
        if (!order.hasTrackingCoordinates()
                || vehicle.getGpsProvider() == null
                || vehicle.getGpsProviderVehicleId() == null
                || vehicle.getGpsProviderVehicleId().isBlank()) {
            return;
        }

        createSession(
                orderVehicle,
                TrackingMode.NORMAL_TRACKING,
                order.getPickupLatitude(),
                order.getPickupLongitude(),
                order.getDropLatitude(),
                order.getDropLongitude(),
                properties.getDefaultGeofenceRadiusMeters()
        );
    }

    @Transactional(readOnly = true)
    public List<TrackingSessionResponse> findActive() {
        return trackingSessions.findByModeNotOrderByIdAsc(TrackingMode.STOPPED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackingSessionResponse findByOrderVehicle(Long orderVehicleId) {
        return trackingSessions.findByOrderVehicle_Id(orderVehicleId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session not found"));
    }

    public TrackingSessionResponse updateMode(Long id, TrackingModeUpdateRequest request) {
        TrackingSession session = findSession(id);
        session.updateMode(request.mode());
        return toResponse(trackingSessions.save(session));
    }

    public TrackingSessionResponse stop(Long id) {
        TrackingSession session = findSession(id);
        session.stop();
        return toResponse(trackingSessions.save(session));
    }

    public void publishLocation(TrackingSession session) {
        Vehicle vehicle = session.getOrderVehicle().getVehicle();
        if (vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
            return;
        }

        liveUpdateService.publish(new TrackingLocationUpdateResponse(
                session.getId(),
                session.getOrderVehicle().getId(),
                vehicle.getId(),
                vehicle.getFleetNumber(),
                vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(),
                vehicle.getCurrentSpeedKph(),
                vehicle.getCurrentHeadingDegrees(),
                vehicle.getLastSeenAt()
        ));
    }

    private TrackingSession findSession(Long id) {
        return trackingSessions.findByIdWithLifecycle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session not found"));
    }

    private TrackingSession createSession(
            OrderVehicle orderVehicle,
            TrackingMode mode,
            double pickupLatitude,
            double pickupLongitude,
            double dropLatitude,
            double dropLongitude,
            int geofenceRadiusMeters
    ) {
        if (orderVehicle.getStatus() == OrderVehicleStatus.DELIVERED
                || orderVehicle.getStatus() == OrderVehicleStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot track a terminal vehicle assignment");
        }
        if (orderVehicle.getVehicle().getGpsProvider() == GpsProviderType.WIALON
                && !orderVehicle.getVehicle().getGpsProviderVehicleId().matches("^\\d+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wialon unit id must be a positive number");
        }

        TrackingSession session = trackingSessions.save(new TrackingSession(
                orderVehicle,
                mode,
                pickupLatitude,
                pickupLongitude,
                dropLatitude,
                dropLongitude,
                geofenceRadiusMeters
        ));
        lifecycleService.trackingStarted(session.getId());
        return session;
    }

    private TrackingSessionResponse toResponse(TrackingSession session) {
        OrderVehicle orderVehicle = session.getOrderVehicle();
        Vehicle vehicle = orderVehicle.getVehicle();
        return new TrackingSessionResponse(
                session.getId(),
                orderVehicle.getId(),
                orderVehicle.getOrder().getId(),
                vehicle.getId(),
                vehicle.getFleetNumber(),
                session.getMode(),
                session.getStatus(),
                session.getPickupLatitude(),
                session.getPickupLongitude(),
                session.getDropLatitude(),
                session.getDropLongitude(),
                session.getGeofenceRadiusMeters(),
                session.getStartedAt(),
                session.getStoppedAt(),
                session.getUpdatedAt()
        );
    }
}
