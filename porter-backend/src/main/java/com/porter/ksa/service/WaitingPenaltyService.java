package com.porter.ksa.service;

import com.porter.ksa.dto.WaitingPenaltyManualActionRequest;
import com.porter.ksa.dto.WaitingPenaltyResponse;
import com.porter.ksa.entity.GpsEventType;
import com.porter.ksa.entity.TrackingSession;
import com.porter.ksa.entity.WaitingPenalty;
import com.porter.ksa.entity.WaitingPenaltyStatus;
import com.porter.ksa.repository.TrackingSessionRepository;
import com.porter.ksa.repository.WaitingPenaltyRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class WaitingPenaltyService {

    private static final Duration FREE_WINDOW = Duration.ofHours(2);

    private final WaitingPenaltyRepository penalties;
    private final TrackingSessionRepository trackingSessions;
    private final GpsEventService gpsEventService;

    public WaitingPenaltyService(
            WaitingPenaltyRepository penalties,
            TrackingSessionRepository trackingSessions,
            GpsEventService gpsEventService
    ) {
        this.penalties = penalties;
        this.trackingSessions = trackingSessions;
        this.gpsEventService = gpsEventService;
    }

    public WaitingPenaltyResponse ensureFreeWindowStarted(TrackingSession trackingSession) {
        WaitingPenalty penalty = penalties.findByTrackingSession_Id(trackingSession.getId())
                .orElseGet(() -> penalties.save(new WaitingPenalty(trackingSession)));
        if (penalty.getStatus() == WaitingPenaltyStatus.NOT_STARTED) {
            Instant now = Instant.now();
            penalty.startFreeWindow(now, now.plus(FREE_WINDOW));
        }
        return toResponse(penalties.save(penalty));
    }

    public void evaluateActivePenalties() {
        Instant now = Instant.now();
        penalties.findByStatusOrderByIdAsc(WaitingPenaltyStatus.FREE_WINDOW)
                .stream()
                .filter(penalty -> penalty.getFreeUntil() != null && !penalty.getFreeUntil().isAfter(now))
                .forEach(penalty -> {
                    penalty.startPenalty();
                    penalties.save(penalty);
                    gpsEventService.record(
                            penalty.getTrackingSession(),
                            GpsEventType.WAITING_PENALTY_STARTED,
                            null,
                            null,
                            "Free unloading window expired"
                    );
                });
    }

    @Transactional(readOnly = true)
    public WaitingPenaltyResponse findByTrackingSession(Long trackingSessionId) {
        return penalties.findByTrackingSession_Id(trackingSessionId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiting penalty not found"));
    }

    public WaitingPenaltyResponse pause(Long trackingSessionId, WaitingPenaltyManualActionRequest request) {
        WaitingPenalty penalty = findPenalty(trackingSessionId);
        penalty.pause(request.notes());
        gpsEventService.record(penalty.getTrackingSession(), GpsEventType.MANUAL_OVERRIDE, null, null, "Waiting penalty paused");
        return toResponse(penalties.save(penalty));
    }

    public WaitingPenaltyResponse waive(Long trackingSessionId, WaitingPenaltyManualActionRequest request) {
        WaitingPenalty penalty = findPenalty(trackingSessionId);
        penalty.waive(request.notes());
        gpsEventService.record(penalty.getTrackingSession(), GpsEventType.MANUAL_OVERRIDE, null, null, "Waiting penalty waived");
        return toResponse(penalties.save(penalty));
    }

    public WaitingPenaltyResponse adjust(Long trackingSessionId, WaitingPenaltyManualActionRequest request) {
        WaitingPenalty penalty = findPenalty(trackingSessionId);
        BigDecimal amount = request.adjustmentAmount() == null ? BigDecimal.ZERO : request.adjustmentAmount();
        penalty.adjust(amount, request.notes());
        gpsEventService.record(penalty.getTrackingSession(), GpsEventType.MANUAL_OVERRIDE, null, null, "Waiting penalty adjusted");
        return toResponse(penalties.save(penalty));
    }

    private WaitingPenalty findPenalty(Long trackingSessionId) {
        trackingSessions.findById(trackingSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session not found"));
        return penalties.findByTrackingSession_Id(trackingSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiting penalty not found"));
    }

    private WaitingPenaltyResponse toResponse(WaitingPenalty penalty) {
        return new WaitingPenaltyResponse(
                penalty.getId(),
                penalty.getTrackingSession().getId(),
                penalty.getStatus(),
                penalty.getUnloadingStartedAt(),
                penalty.getFreeUntil(),
                penalty.getPenaltyStartedAt(),
                penalty.getPausedAt(),
                penalty.getWaivedAt(),
                penalty.getManualAdjustmentAmount(),
                penalty.getNotes(),
                penalty.getUpdatedAt()
        );
    }
}
