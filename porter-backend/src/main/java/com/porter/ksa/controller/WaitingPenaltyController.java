package com.porter.ksa.controller;

import com.porter.ksa.dto.WaitingPenaltyManualActionRequest;
import com.porter.ksa.dto.WaitingPenaltyResponse;
import com.porter.ksa.service.WaitingPenaltyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waiting-penalties")
public class WaitingPenaltyController {

    private final WaitingPenaltyService waitingPenaltyService;

    public WaitingPenaltyController(WaitingPenaltyService waitingPenaltyService) {
        this.waitingPenaltyService = waitingPenaltyService;
    }

    @GetMapping("/tracking-sessions/{trackingSessionId}")
    public WaitingPenaltyResponse findByTrackingSession(@PathVariable Long trackingSessionId) {
        return waitingPenaltyService.findByTrackingSession(trackingSessionId);
    }

    @PatchMapping("/tracking-sessions/{trackingSessionId}/pause")
    public WaitingPenaltyResponse pause(
            @PathVariable Long trackingSessionId,
            @Valid @RequestBody WaitingPenaltyManualActionRequest request
    ) {
        return waitingPenaltyService.pause(trackingSessionId, request);
    }

    @PatchMapping("/tracking-sessions/{trackingSessionId}/waive")
    public WaitingPenaltyResponse waive(
            @PathVariable Long trackingSessionId,
            @Valid @RequestBody WaitingPenaltyManualActionRequest request
    ) {
        return waitingPenaltyService.waive(trackingSessionId, request);
    }

    @PatchMapping("/tracking-sessions/{trackingSessionId}/adjust")
    public WaitingPenaltyResponse adjust(
            @PathVariable Long trackingSessionId,
            @Valid @RequestBody WaitingPenaltyManualActionRequest request
    ) {
        return waitingPenaltyService.adjust(trackingSessionId, request);
    }
}
