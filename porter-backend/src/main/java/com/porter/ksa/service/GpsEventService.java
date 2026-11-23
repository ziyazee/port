package com.porter.ksa.service;

import com.porter.ksa.dto.GpsEventResponse;
import com.porter.ksa.entity.GpsEvent;
import com.porter.ksa.entity.GpsEventType;
import com.porter.ksa.entity.TrackingSession;
import com.porter.ksa.repository.GpsEventRepository;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GpsEventService {

    private final GpsEventRepository events;

    public GpsEventService(GpsEventRepository events) {
        this.events = events;
    }

    public GpsEventResponse record(
            TrackingSession trackingSession,
            GpsEventType eventType,
            Double latitude,
            Double longitude,
            String notes
    ) {
        return toResponse(events.save(new GpsEvent(trackingSession, eventType, latitude, longitude, notes)));
    }

    public boolean hasEvent(Long trackingSessionId, GpsEventType eventType) {
        return events.existsByTrackingSession_IdAndEventType(trackingSessionId, eventType);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> findEventTime(Long trackingSessionId, GpsEventType eventType) {
        return events.findFirstByTrackingSession_IdAndEventTypeOrderByEventAtAsc(trackingSessionId, eventType)
                .map(GpsEvent::getEventAt);
    }

    @Transactional(readOnly = true)
    public List<GpsEventResponse> findByTrackingSession(Long trackingSessionId) {
        return events.findByTrackingSession_IdOrderByEventAtAsc(trackingSessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GpsEventResponse toResponse(GpsEvent event) {
        return new GpsEventResponse(
                event.getId(),
                event.getTrackingSession().getId(),
                event.getEventType(),
                event.getLatitude(),
                event.getLongitude(),
                event.getNotes(),
                event.getEventAt(),
                event.getCreatedAt()
        );
    }
}
