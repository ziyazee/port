package com.porter.ksa.service;

import com.porter.ksa.config.GpsModuleProperties;
import com.porter.ksa.entity.TrackingMode;
import com.porter.ksa.entity.TrackingSession;
import com.porter.ksa.repository.TrackingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TrackingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrackingScheduler.class);

    private final GpsModuleProperties properties;
    private final GpsTrackingService gpsTrackingService;
    private final TrackingSessionRepository trackingSessions;
    private final GeofenceService geofenceService;
    private final WaitingPenaltyService waitingPenaltyService;
    private final TrackingSessionService trackingSessionService;

    public TrackingScheduler(
            GpsModuleProperties properties,
            GpsTrackingService gpsTrackingService,
            TrackingSessionRepository trackingSessions,
            GeofenceService geofenceService,
            WaitingPenaltyService waitingPenaltyService,
            TrackingSessionService trackingSessionService
    ) {
        this.properties = properties;
        this.gpsTrackingService = gpsTrackingService;
        this.trackingSessions = trackingSessions;
        this.geofenceService = geofenceService;
        this.waitingPenaltyService = waitingPenaltyService;
        this.trackingSessionService = trackingSessionService;
    }

    @Scheduled(fixedDelayString = "${porter.tracking.scheduler-interval-ms:10000}")
    public void processTrackingSessions() {
        if (!properties.isTrackingSchedulerEnabled()) {
            return;
        }

        try {
            gpsTrackingService.pollActiveDevices();
        } catch (ResponseStatusException exception) {
            log.warn("GPS refresh failed: {}", exception.getReason());
        } catch (RuntimeException exception) {
            log.error("GPS refresh failed", exception);
        }

        for (TrackingSession session : trackingSessions.findByModeNotOrderByIdAsc(TrackingMode.STOPPED)) {
            try {
                geofenceService.evaluate(session);
                trackingSessionService.publishLocation(session);
            } catch (RuntimeException exception) {
                log.error("Tracking session {} failed", session.getId(), exception);
            }
        }

        try {
            waitingPenaltyService.evaluateActivePenalties();
        } catch (RuntimeException exception) {
            log.error("Waiting penalty evaluation failed", exception);
        }
    }
}
