package com.porter.ksa.service;

import com.porter.ksa.entity.GpsEventType;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.OrderVehicle;
import com.porter.ksa.entity.OrderVehicleStatus;
import com.porter.ksa.entity.TrackingMode;
import com.porter.ksa.entity.TrackingSession;
import com.porter.ksa.entity.TrackingStatus;
import com.porter.ksa.entity.TransportOrder;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.repository.OrderVehicleRepository;
import com.porter.ksa.repository.TrackingSessionRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import com.porter.ksa.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TrackingLifecycleService {

    private static final String AUTOMATION_ACTOR = "gps-lifecycle";

    private final TrackingSessionRepository trackingSessions;
    private final OrderVehicleRepository orderVehicles;
    private final TransportOrderRepository orders;
    private final VehicleRepository vehicles;
    private final TrackingLifecycleTransitionPolicy transitionPolicy;
    private final GpsEventService gpsEventService;
    private final WaitingPenaltyService waitingPenaltyService;

    public TrackingLifecycleService(
            TrackingSessionRepository trackingSessions,
            OrderVehicleRepository orderVehicles,
            TransportOrderRepository orders,
            VehicleRepository vehicles,
            TrackingLifecycleTransitionPolicy transitionPolicy,
            GpsEventService gpsEventService,
            WaitingPenaltyService waitingPenaltyService
    ) {
        this.trackingSessions = trackingSessions;
        this.orderVehicles = orderVehicles;
        this.orders = orders;
        this.vehicles = vehicles;
        this.transitionPolicy = transitionPolicy;
        this.gpsEventService = gpsEventService;
        this.waitingPenaltyService = waitingPenaltyService;
    }

    public void trackingStarted(Long trackingSessionId) {
        TrackingSession session = findSession(trackingSessionId);
        recordOnce(session, GpsEventType.TRACKING_STARTED, "Tracking started");

        TransportOrder order = session.getOrderVehicle().getOrder();
        if (order.getOrderStatus() == OrderStatus.CREATED
                || order.getOrderStatus() == OrderStatus.VERIFIED
                || order.getOrderStatus() == OrderStatus.VEHICLE_ASSIGNED) {
            order.updateStatus(OrderStatus.TRACKING_STARTED, AUTOMATION_ACTOR);
            orders.save(order);
        }
    }

    public boolean applyGpsTransition(Long trackingSessionId, TrackingStatus target, String notes) {
        return transition(trackingSessionId, target, LifecycleTransitionSource.GPS, AUTOMATION_ACTOR, notes);
    }

    public void applyManualTransition(Long trackingSessionId, TrackingStatus target, String updatedBy) {
        transition(
                trackingSessionId,
                target,
                LifecycleTransitionSource.MANUAL,
                requireActor(updatedBy),
                "Manual status override to " + target
        );
    }

    public void applyManualTransition(OrderVehicle assignment, OrderVehicleStatus target, String updatedBy) {
        TrackingStatus currentTrackingStatus = toTrackingStatus(assignment.getStatus());
        TrackingStatus targetTrackingStatus = toTrackingStatus(target);
        if (!transitionPolicy.canTransition(
                currentTrackingStatus,
                targetTrackingStatus,
                LifecycleTransitionSource.MANUAL
        )) {
            throw invalidTransition(currentTrackingStatus, targetTrackingStatus);
        }

        assignment.updateStatus(target);
        applyVehicleAvailability(assignment, targetTrackingStatus);
        orderVehicles.save(assignment);
        updateOrderForTransition(assignment.getOrder(), targetTrackingStatus, requireActor(updatedBy));
    }

    private boolean transition(
            Long trackingSessionId,
            TrackingStatus target,
            LifecycleTransitionSource source,
            String actor,
            String notes
    ) {
        TrackingSession session = findSession(trackingSessionId);
        TrackingStatus current = session.getStatus();
        if (current == target) {
            return false;
        }
        if (!transitionPolicy.canTransition(current, target, source)) {
            if (source == LifecycleTransitionSource.MANUAL) {
                throw invalidTransition(current, target);
            }
            return false;
        }

        session.updateStatus(target);
        OrderVehicle assignment = session.getOrderVehicle();
        assignment.updateStatus(toOrderVehicleStatus(target));
        applyVehicleAvailability(assignment, target);

        trackingSessions.save(session);
        orderVehicles.save(assignment);
        recordTransitionEvent(session, target, source, notes);
        updateOrderForTransition(assignment.getOrder(), target, actor);

        if (target == TrackingStatus.UNLOADING) {
            waitingPenaltyService.ensureFreeWindowStarted(session);
        }
        if (transitionPolicy.isTerminal(target) && session.getMode() != TrackingMode.STOPPED) {
            session.stop();
            trackingSessions.save(session);
        }

        return true;
    }

    private void updateOrderForTransition(TransportOrder order, TrackingStatus target, String actor) {
        if (target == TrackingStatus.DELIVERED) {
            if (orderVehicles.countByOrder_Id(order.getId()) > 0
                    && !orderVehicles.existsByOrder_IdAndStatusNot(order.getId(), OrderVehicleStatus.DELIVERED)) {
                order.updateStatus(OrderStatus.COMPLETED, actor);
                orders.save(order);
            }
            return;
        }

        if (order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getOrderStatus() != OrderStatus.COMPLETED
                && target != TrackingStatus.ASSIGNED
                && target != TrackingStatus.CANCELLED) {
            order.updateStatus(OrderStatus.IN_PROGRESS, actor);
            orders.save(order);
        }
    }

    private void applyVehicleAvailability(OrderVehicle assignment, TrackingStatus target) {
        Vehicle vehicle = assignment.getVehicle();
        if (target == TrackingStatus.DELIVERED || target == TrackingStatus.CANCELLED) {
            vehicle.updateOperationalStatus(VehicleStatus.AVAILABLE);
            if (assignment.getDriver() != null) {
                assignment.getDriver().releaseFromOrder();
            }
        } else {
            vehicle.updateOperationalStatus(VehicleStatus.ASSIGNED);
        }
        vehicles.save(vehicle);
    }

    private void recordTransitionEvent(
            TrackingSession session,
            TrackingStatus target,
            LifecycleTransitionSource source,
            String notes
    ) {
        if (source == LifecycleTransitionSource.MANUAL) {
            gpsEventService.record(
                    session,
                    GpsEventType.MANUAL_OVERRIDE,
                    currentLatitude(session),
                    currentLongitude(session),
                    notes
            );
            return;
        }

        GpsEventType eventType = switch (target) {
            case ARRIVED_AT_PICKUP -> GpsEventType.ENTERED_PICKUP;
            case LEFT_PICKUP -> GpsEventType.LEFT_PICKUP;
            case ARRIVED_AT_DROP -> GpsEventType.ENTERED_DROP;
            case UNLOADING -> GpsEventType.UNLOADING_STARTED;
            case DELIVERED -> GpsEventType.DELIVERED;
            default -> null;
        };
        if (eventType != null) {
            recordOnce(session, eventType, notes);
        }
    }

    private void recordOnce(TrackingSession session, GpsEventType eventType, String notes) {
        if (!gpsEventService.hasEvent(session.getId(), eventType)) {
            gpsEventService.record(
                    session,
                    eventType,
                    currentLatitude(session),
                    currentLongitude(session),
                    notes
            );
        }
    }

    private Double currentLatitude(TrackingSession session) {
        return session.getOrderVehicle().getVehicle().getCurrentLatitude();
    }

    private Double currentLongitude(TrackingSession session) {
        return session.getOrderVehicle().getVehicle().getCurrentLongitude();
    }

    private TrackingSession findSession(Long trackingSessionId) {
        return trackingSessions.findByIdWithLifecycle(trackingSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session not found"));
    }

    private OrderVehicleStatus toOrderVehicleStatus(TrackingStatus status) {
        return OrderVehicleStatus.valueOf(status.name());
    }

    private TrackingStatus toTrackingStatus(OrderVehicleStatus status) {
        return TrackingStatus.valueOf(status.name());
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Updated by is required");
        }
        return actor.trim();
    }

    private ResponseStatusException invalidTransition(TrackingStatus current, TrackingStatus target) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invalid tracking transition from " + current + " to " + target
        );
    }
}
