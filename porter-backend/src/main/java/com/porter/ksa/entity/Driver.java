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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Column(unique = true, length = 80)
    private String nationalId;

    @Column(unique = true, length = 80)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DriverStatus status = DriverStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_vehicle_id")
    private Vehicle currentVehicle;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Driver() {
    }

    public Driver(
            String fullName,
            String phoneNumber,
            String nationalId,
            String licenseNumber,
            DriverStatus status
    ) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.nationalId = nationalId;
        this.licenseNumber = licenseNumber;
        this.status = status;
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

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String fullName,
            String phoneNumber,
            String nationalId,
            String licenseNumber,
            DriverStatus status
    ) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.nationalId = nationalId;
        this.licenseNumber = licenseNumber;
        this.status = status;
    }

    public void assignToVehicle(Vehicle vehicle) {
        this.currentVehicle = vehicle;
    }

    public void startOrder() {
        this.status = DriverStatus.BUSY;
    }

    public void unassignVehicle() {
        this.currentVehicle = null;
        if (status == DriverStatus.BUSY) {
            this.status = DriverStatus.AVAILABLE;
        }
    }

    public void releaseFromOrder() {
        if (status == DriverStatus.BUSY) {
            this.status = DriverStatus.AVAILABLE;
        }
    }
}
