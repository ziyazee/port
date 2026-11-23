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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "waiting_penalties")
public class WaitingPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tracking_session_id", nullable = false, unique = true)
    private TrackingSession trackingSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WaitingPenaltyStatus status = WaitingPenaltyStatus.NOT_STARTED;

    private Instant unloadingStartedAt;

    private Instant freeUntil;

    private Instant penaltyStartedAt;

    private Instant pausedAt;

    private Instant waivedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal manualAdjustmentAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WaitingPenalty() {
    }

    public WaitingPenalty(TrackingSession trackingSession) {
        this.trackingSession = trackingSession;
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

    public TrackingSession getTrackingSession() {
        return trackingSession;
    }

    public WaitingPenaltyStatus getStatus() {
        return status;
    }

    public Instant getUnloadingStartedAt() {
        return unloadingStartedAt;
    }

    public Instant getFreeUntil() {
        return freeUntil;
    }

    public Instant getPenaltyStartedAt() {
        return penaltyStartedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public Instant getWaivedAt() {
        return waivedAt;
    }

    public BigDecimal getManualAdjustmentAmount() {
        return manualAdjustmentAmount;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void startFreeWindow(Instant unloadingStartedAt, Instant freeUntil) {
        this.status = WaitingPenaltyStatus.FREE_WINDOW;
        this.unloadingStartedAt = unloadingStartedAt;
        this.freeUntil = freeUntil;
    }

    public void startPenalty() {
        this.status = WaitingPenaltyStatus.ACTIVE;
        this.penaltyStartedAt = Instant.now();
    }

    public void pause(String notes) {
        this.status = WaitingPenaltyStatus.PAUSED;
        this.pausedAt = Instant.now();
        this.notes = notes;
    }

    public void waive(String notes) {
        this.status = WaitingPenaltyStatus.WAIVED;
        this.waivedAt = Instant.now();
        this.notes = notes;
    }

    public void adjust(BigDecimal amount, String notes) {
        this.manualAdjustmentAmount = amount;
        this.notes = notes;
    }

    public void complete() {
        this.status = WaitingPenaltyStatus.COMPLETED;
    }
}
