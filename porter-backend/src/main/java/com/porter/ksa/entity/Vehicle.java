package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String fleetNumber;

    @Column(nullable = false, length = 80)
    private String plateNumber;

    @Column(length = 160)
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private GpsProviderType gpsProvider;

    @Column(length = 120)
    private String gpsProviderVehicleId;

    private Double currentLatitude;

    private Double currentLongitude;

    private Double currentSpeedKph;

    private Double currentHeadingDegrees;

    private Instant lastSeenAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt = Instant.now();

    protected Vehicle() {
    }

    public Vehicle(String fleetNumber, String plateNumber, VehicleType vehicleType, VehicleStatus status) {
        this(fleetNumber, plateNumber, vehicleType, status, null);
    }

    public Vehicle(
            String fleetNumber,
            String plateNumber,
            VehicleType vehicleType,
            VehicleStatus status,
            String driverName
    ) {
        this.fleetNumber = fleetNumber;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.status = status;
        this.driverName = driverName;
    }

    public Long getId() {
        return id;
    }

    public String getFleetNumber() {
        return fleetNumber;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public GpsProviderType getGpsProvider() {
        return gpsProvider;
    }

    public String getGpsProviderVehicleId() {
        return gpsProviderVehicleId;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public Double getCurrentSpeedKph() {
        return currentSpeedKph;
    }

    public Double getCurrentHeadingDegrees() {
        return currentHeadingDegrees;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String fleetNumber,
            String plateNumber,
            VehicleType vehicleType,
            VehicleStatus status,
            String driverName
    ) {
        this.fleetNumber = fleetNumber;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.status = status;
        this.driverName = driverName;
        this.updatedAt = Instant.now();
    }

    public void updateGpsDevice(GpsProviderType gpsProvider, String gpsProviderVehicleId) {
        this.gpsProvider = gpsProvider;
        this.gpsProviderVehicleId = gpsProviderVehicleId;
        this.updatedAt = Instant.now();
    }

    public void updateLatestLocation(
            double latitude,
            double longitude,
            double speedKph,
            double headingDegrees,
            Instant lastSeenAt
    ) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.currentSpeedKph = speedKph;
        this.currentHeadingDegrees = headingDegrees;
        this.lastSeenAt = lastSeenAt;
        this.updatedAt = Instant.now();
    }

    public void updateDriverName(String driverName) {
        this.driverName = driverName;
        this.updatedAt = Instant.now();
    }

    public void updateOperationalStatus(VehicleStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
