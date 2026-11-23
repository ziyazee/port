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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "gps_events")
public class GpsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracking_session_id", nullable = false)
    private TrackingSession trackingSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private GpsEventType eventType;

    private Double latitude;

    private Double longitude;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Instant eventAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected GpsEvent() {
    }

    public GpsEvent(TrackingSession trackingSession, GpsEventType eventType, Double latitude, Double longitude, String notes) {
        this.trackingSession = trackingSession;
        this.eventType = eventType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
        this.eventAt = Instant.now();
    }

    @PrePersist
    void beforeCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public TrackingSession getTrackingSession() {
        return trackingSession;
    }

    public GpsEventType getEventType() {
        return eventType;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
