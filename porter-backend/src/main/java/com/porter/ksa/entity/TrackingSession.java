package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracking_sessions")
public class TrackingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_vehicle_id", nullable = false, unique = true)
    private OrderVehicle orderVehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TrackingMode mode = TrackingMode.NORMAL_TRACKING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TrackingStatus status = TrackingStatus.ASSIGNED;

    @Column(nullable = false)
    private double pickupLatitude;

    @Column(nullable = false)
    private double pickupLongitude;

    @Column(nullable = false)
    private double dropLatitude;

    @Column(nullable = false)
    private double dropLongitude;

    @Column(nullable = false)
    private int geofenceRadiusMeters;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant stoppedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TrackingSession() {
    }

    public TrackingSession(
            OrderVehicle orderVehicle,
            TrackingMode mode,
            double pickupLatitude,
            double pickupLongitude,
            double dropLatitude,
            double dropLongitude,
            int geofenceRadiusMeters
    ) {
        this.orderVehicle = orderVehicle;
        this.mode = mode;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropLatitude = dropLatitude;
        this.dropLongitude = dropLongitude;
        this.geofenceRadiusMeters = geofenceRadiusMeters;
        this.startedAt = Instant.now();
    }

    @PrePersist
    void beforeCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public OrderVehicle getOrderVehicle() {
        return orderVehicle;
    }

    public TrackingMode getMode() {
        return mode;
    }

    public TrackingStatus getStatus() {
        return status;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public double getDropLatitude() {
        return dropLatitude;
    }

    public double getDropLongitude() {
        return dropLongitude;
    }

    public int getGeofenceRadiusMeters() {
        return geofenceRadiusMeters;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getStoppedAt() {
        return stoppedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateMode(TrackingMode mode) {
        this.mode = mode;
    }

    public void updateStatus(TrackingStatus status) {
        this.status = status;
    }

    public void stop() {
        this.mode = TrackingMode.STOPPED;
        if (this.stoppedAt == null) {
            this.stoppedAt = Instant.now();
        }
    }
}
