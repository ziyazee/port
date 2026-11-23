package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "gps_devices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gps_devices_provider_external_id",
                columnNames = {"provider", "provider_vehicle_id"}
        )
)
public class GpsDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GpsProviderType provider;

    @Column(name = "provider_vehicle_id", nullable = false, length = 120)
    private String providerVehicleId;

    @Column(length = 80)
    private String vehicleFleetNumber;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GpsDevice() {
    }

    public GpsDevice(GpsProviderType provider, String providerVehicleId, String vehicleFleetNumber) {
        this.provider = provider;
        this.providerVehicleId = providerVehicleId;
        this.vehicleFleetNumber = vehicleFleetNumber;
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

    public GpsProviderType getProvider() {
        return provider;
    }

    public String getProviderVehicleId() {
        return providerVehicleId;
    }

    public String getVehicleFleetNumber() {
        return vehicleFleetNumber;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String vehicleFleetNumber, boolean active) {
        this.vehicleFleetNumber = vehicleFleetNumber;
        this.active = active;
    }

    public void updateVehicleFleetNumber(String vehicleFleetNumber) {
        this.vehicleFleetNumber = vehicleFleetNumber;
    }
}
