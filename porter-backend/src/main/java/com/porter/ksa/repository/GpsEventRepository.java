package com.porter.ksa.repository;

import com.porter.ksa.entity.GpsEvent;
import com.porter.ksa.entity.GpsEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsEventRepository extends JpaRepository<GpsEvent, Long> {

    boolean existsByTrackingSession_IdAndEventType(Long trackingSessionId, GpsEventType eventType);

    Optional<GpsEvent> findFirstByTrackingSession_IdAndEventTypeOrderByEventAtAsc(
            Long trackingSessionId,
            GpsEventType eventType
    );

    @EntityGraph(attributePaths = "trackingSession")
    List<GpsEvent> findByTrackingSession_IdOrderByEventAtAsc(Long trackingSessionId);
}
