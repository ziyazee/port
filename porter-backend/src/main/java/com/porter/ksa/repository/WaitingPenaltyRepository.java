package com.porter.ksa.repository;

import com.porter.ksa.entity.WaitingPenalty;
import com.porter.ksa.entity.WaitingPenaltyStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitingPenaltyRepository extends JpaRepository<WaitingPenalty, Long> {

    @EntityGraph(attributePaths = "trackingSession")
    Optional<WaitingPenalty> findByTrackingSession_Id(Long trackingSessionId);

    @EntityGraph(attributePaths = "trackingSession")
    List<WaitingPenalty> findByStatusOrderByIdAsc(WaitingPenaltyStatus status);
}
