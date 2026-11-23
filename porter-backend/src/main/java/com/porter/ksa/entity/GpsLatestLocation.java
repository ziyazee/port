package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "gps_latest_locations")
public class GpsLatestLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gps_device_id", nullable = false, unique = true)
    private GpsDevice gpsDevice;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double speedKph;

    @Column(nullable = false)
    private double headingDegrees;

    @Column(nullable = false)
    private Instant providerRecordedAt;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GpsLatestLocation() {
    }

    public GpsLatestLocation(
            GpsDevice gpsDevice,
            double latitude,
            double longitude,
            double speedKph,
            double headingDegrees,
            Instant providerRecordedAt,
            Instant receivedAt
    ) {
        this.gpsDevice = gpsDevice;
        update(latitude, longitude, speedKph, headingDegrees, providerRecordedAt, receivedAt);
    }

    @PrePersist
    void beforeCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public GpsDevice getGpsDevice() {
        return gpsDevice;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getSpeedKph() {
        return speedKph;
    }

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    public Instant getProviderRecordedAt() {
        return providerRecordedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            double latitude,
            double longitude,
            double speedKph,
            double headingDegrees,
            Instant providerRecordedAt,
            Instant receivedAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKph = speedKph;
        this.headingDegrees = headingDegrees;
        this.providerRecordedAt = providerRecordedAt;
        this.receivedAt = receivedAt;
        this.updatedAt = Instant.now();
    }
}
